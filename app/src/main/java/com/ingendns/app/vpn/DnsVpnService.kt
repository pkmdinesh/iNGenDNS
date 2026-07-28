package com.ingendns.app.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ingendns.app.R
import com.ingendns.app.di.AppContainer
import com.ingendns.app.logger.EventLogStore
import com.ingendns.app.preferences.PreferenceManager
import com.ingendns.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet4Address
import kotlin.concurrent.thread

/** Local, DNS-only IPv4 UDP forwarder. All non-DNS traffic remains on the underlying network. */
class DnsVpnService : VpnService() {
    private val forwarder = DnsForwarder()
    private var vpnInterface: android.os.ParcelFileDescriptor? = null
    private var runner: Thread? = null
    private lateinit var resolver: String
    private var resolverName: String? = null
    private lateinit var protocol: DnsProtocol
    private lateinit var endpoint: String
    private var callbackRegistered = false
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var failoverRunnable: Runnable? = null
    private var connectionWatchdogRunnable: Runnable? = null
    private var failoverInProgress = false
    private var activeMode = VpnOperatingMode.INACTIVE
    private var activeResolver: String? = null
    private val failedResolvers = linkedSetOf<String>()
    private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }
    private val configurationStore by lazy { VpnConfigurationStore(applicationContext) }
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateForActiveNetwork()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
            updateForActiveNetwork()

        override fun onLost(network: Network) {
            updateForActiveNetwork()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            configurationStore.clear()
            stopSelf(); return START_NOT_STICKY
        }
        val configuration = intent?.toVpnConfiguration() ?: configurationStore.load()
            ?: return START_NOT_STICKY
        resolver = configuration.resolver
        resolverName = configuration.name
        protocol = configuration.protocol
        endpoint = configuration.endpoint
        configurationStore.save(configuration)
        if (intent?.getBooleanExtra(EXTRA_IS_FAILOVER, false) != true) failedResolvers.clear()
        startForeground(NOTIFICATION_ID, notification())
        if (!callbackRegistered) {
            connectivity.registerDefaultNetworkCallback(callback)
            callbackRegistered = true
        }
        closeTunnel("Resolver changed")
        updateForActiveNetwork()
        return START_STICKY
    }

    private fun updateForActiveNetwork() {
        val network = connectivity.activeNetwork
        val caps = network?.let(connectivity::getNetworkCapabilities)
        when {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                openTunnel(
                    resolver,
                    resolverName,
                    protocol,
                    endpoint,
                    VpnOperatingMode.CELLULAR_OPTIMIZED
                )
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val networkDns = connectivity.getLinkProperties(network)?.dnsServers
                    ?.filterIsInstance<Inet4Address>()?.firstOrNull()?.hostAddress
                if (networkDns != null) {
                    openTunnel(
                        networkDns,
                        "Network DNS",
                        null,
                        null,
                        VpnOperatingMode.WIFI_NETWORK_DNS
                    )
                } else {
                    waitForNetwork("Wi-Fi did not provide a compatible DNS server")
                }
            }
            else -> waitForNetwork("Waiting for a validated network")
        }
    }

    private fun isCellular(): Boolean =
        connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.let { caps ->
            VpnNetworkPolicy.shouldOpenTunnel(
                isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            )
        } == true

    @Synchronized
    private fun openTunnel(
        targetResolver: String,
        targetName: String?,
        targetProtocol: DnsProtocol?,
        targetEndpoint: String?,
        mode: VpnOperatingMode
    ) {
        if (vpnInterface != null && activeMode == mode && activeResolver == targetResolver) return
        closeTunnel("Network mode changed")
        runCatching {
            vpnInterface = Builder().setSession("iNGenDNS")
                .addAddress("10.10.0.2", 32).addDnsServer(targetResolver).addRoute(targetResolver, 32)
                .addDisallowedApplication(packageName)
                .setMtu(Constants.VPN_MTU).setBlocking(true).establish()
            requireNotNull(vpnInterface) { "VPN interface was not established" }
            activeMode = mode
            activeResolver = targetResolver
            val status = if (mode == VpnOperatingMode.CELLULAR_OPTIMIZED) {
                "Mobile data - Optimized DNS: ${targetName ?: targetResolver}"
            } else {
                "Wi-Fi - Using network DNS: $targetResolver"
            }
            EventLogStore(this).record("DNS VPN started", status)
            DnsVpnState.tunnelStarted(
                targetResolver, targetName, targetProtocol, targetEndpoint,
                mode, status, lockdownEnabled(), alwaysOnEnabled()
            )
            if (mode == VpnOperatingMode.CELLULAR_OPTIMIZED) scheduleConnectionWatchdog()
            val descriptor = requireNotNull(vpnInterface)
            runner = thread(name = "dns-vpn", isDaemon = true) {
                forwardDns(descriptor, targetResolver, targetProtocol, targetEndpoint)
            }
        }.onFailure {
            EventLogStore(this).record(
                "DNS VPN failed",
                it.message ?: "Unknown error"
            ); closeTunnel("Setup failed")
            DnsVpnState.waiting(
                if (lockdownEnabled()) LOCKDOWN_MESSAGE else "VPN tunnel setup failed",
                lockdownEnabled()
            )
        }
    }

    private fun forwardDns(
        descriptor: android.os.ParcelFileDescriptor,
        targetResolver: String,
        targetProtocol: DnsProtocol?,
        targetEndpoint: String?
    ) {
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val buffer = ByteArray(32_767)
        var consecutiveFailures = 0
        try {
            val verified = sendQuery(targetResolver, targetProtocol, targetEndpoint, CONNECTION_TEST_QUERY) != null
            if (verified) markConnected(targetProtocol)
            else markConnectionFailed()

            while (!Thread.currentThread().isInterrupted) {
                val count = input.read(buffer)
                if (count <= 28) continue
                val packet = buffer.copyOf(count)
                val header = (packet[0].toInt() and 0x0f) * 4
                if ((packet[0].toInt() ushr 4) != 4 || packet[9].toInt() != 17 || header < 20) continue
                val source = packet.copyOfRange(12, 16)
                val sourcePort =
                    ((packet[header].toInt() and 0xff) shl 8) or (packet[header + 1].toInt() and 0xff)
                val destinationPort =
                    ((packet[header + 2].toInt() and 0xff) shl 8) or (packet[header + 3].toInt() and 0xff)
                if (destinationPort != Constants.DNS_PORT) continue
                val payload = packet.copyOfRange(header + 8, count)
                val response = sendQuery(targetResolver, targetProtocol, targetEndpoint, payload)
                if (response == null) {
                    consecutiveFailures++
                    if (consecutiveFailures >= FAILURE_THRESHOLD) {
                        markConnectionFailed()
                    }
                    continue
                }
                consecutiveFailures = 0
                markConnected(targetProtocol)
                output.write(
                    Ipv4UdpPacketBuilder.response(
                        sourceAddress = targetResolver,
                        destinationAddress = source,
                        sourcePort = Constants.DNS_PORT,
                        destinationPort = sourcePort,
                        payload = response
                    )
                )
                output.flush()
            }
        } catch (_: Exception) { /* tunnel closure or transient network failure */
        } finally {
            input.close(); output.close()
        }
    }

    private fun markConnected(targetProtocol: DnsProtocol?) {
        if (targetProtocol == null) DnsVpnState.networkDnsConnected()
        else markEncryptedConnected()
    }

    private fun sendQuery(
        targetResolver: String,
        targetProtocol: DnsProtocol?,
        targetEndpoint: String?,
        request: ByteArray
    ): ByteArray? = if (targetProtocol == null) {
        forwarder.sendPlain(targetResolver, request, ::protect)
    } else {
        forwarder.sendQuery(targetProtocol, targetResolver, requireNotNull(targetEndpoint), request, ::protect)
    }

    @Synchronized
    private fun markEncryptedConnected() {
        val wasConnected = DnsVpnState.state.value.encryptedConnected
        cancelConnectionWatchdog()
        cancelFailover()
        DnsVpnState.encryptedConnected()
        if (!wasConnected) {
            val connectedResolver = resolver
            val connectedName = resolverName
            val connectedProtocol = protocol
            serviceScope.launch {
                PreferenceManager(applicationContext).saveLastConnectedDns(
                    connectedResolver,
                    connectedName,
                    connectedProtocol
                )
            }
        }
    }

    @Synchronized
    private fun markConnectionFailed() {
        cancelConnectionWatchdog()
        DnsVpnState.connectionFailed()
        synchronized(this) {
            if (failoverRunnable != null || failoverInProgress) return
            val task = Runnable {
                synchronized(this) {
                    failoverRunnable = null
                    failoverInProgress = true
                }
                if (!DnsVpnState.state.value.connectionFailed) {
                    synchronized(this) { failoverInProgress = false }
                    return@Runnable
                }
                serviceScope.launch {
                    try {
                        rollOverToNextResolver()
                    } finally {
                        synchronized(this@DnsVpnService) { failoverInProgress = false }
                    }
                }
            }
            failoverRunnable = task
            handler.postDelayed(task, FAILOVER_DELAY_MILLIS)
        }
        EventLogStore(this).record(
            "DNS failover scheduled",
            "${resolverName ?: resolver}: switching resolver in ${FAILOVER_DELAY_MILLIS / 1_000} seconds"
        )
    }

    @Synchronized
    private fun scheduleConnectionWatchdog() {
        cancelConnectionWatchdog()
        val expectedResolver = resolver
        val expectedProtocol = protocol
        val task = Runnable {
            handleConnectionWatchdogTimeout(expectedResolver, expectedProtocol)
        }
        connectionWatchdogRunnable = task
        handler.postDelayed(task, CONNECTION_WATCHDOG_MILLIS)
    }

    @Synchronized
    private fun handleConnectionWatchdogTimeout(
        expectedResolver: String,
        expectedProtocol: DnsProtocol
    ) {
        connectionWatchdogRunnable = null
        val state = DnsVpnState.state.value
        if (
            state.active && state.resolver == expectedResolver &&
            state.protocol == expectedProtocol && !state.encryptedConnected &&
            !state.connectionFailed
        ) {
            EventLogStore(this).record(
                "DNS connection timed out",
                "${resolverName ?: expectedResolver}: no encrypted response within 10 seconds"
            )
            markConnectionFailed()
        }
    }

    @Synchronized
    private fun cancelConnectionWatchdog() {
        connectionWatchdogRunnable?.let(handler::removeCallbacks)
        connectionWatchdogRunnable = null
    }

    private suspend fun rollOverToNextResolver() {
        if (!DnsVpnState.state.value.connectionFailed || !isCellular()) return

        val repository = AppContainer(applicationContext).dnsRepository
        failedResolvers += resolver
        val next = DnsFailoverSelector.next(
            currentIp = resolver,
            protocol = protocol,
            rankedResults = repository.getLatestResults(),
            availableServers = repository.getDnsServers(),
            excludedIps = failedResolvers
        )
        if (next == null) {
            EventLogStore(this).record(
                "DNS failover exhausted",
                "No remaining ${protocol.name} resolver is available"
            )
            DnsVpnState.connectionFailed()
            return
        }
        val nextEndpoint = when (protocol) {
            DnsProtocol.DOT -> next.dotHostname
            DnsProtocol.DOH -> next.dohUrl
        }
        EventLogStore(this).record(
            "DNS failover",
            "${resolverName ?: resolver} to ${next.name} (${next.ip})"
        )
        start(applicationContext, next.ip, next.name, protocol, nextEndpoint, isFailover = true)
    }

    private fun cancelFailover() {
        synchronized(this) {
            failoverRunnable?.let(handler::removeCallbacks)
            failoverRunnable = null
            failoverInProgress = false
        }
    }

    @Synchronized
    private fun closeTunnel(reason: String) {
        cancelConnectionWatchdog(); cancelFailover(); vpnInterface?.close(); vpnInterface = null; runner?.interrupt(); forwarder.close(); runner =
            null; activeMode = VpnOperatingMode.INACTIVE; activeResolver = null
        DnsVpnState.disconnected(); EventLogStore(this).record("DNS VPN stopped", reason)
    }

    private fun waitForNetwork(message: String) {
        closeTunnel(message)
        DnsVpnState.waiting(
            if (lockdownEnabled()) LOCKDOWN_MESSAGE else message,
            lockdownEnabled()
        )
    }

    private fun lockdownEnabled(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isLockdownEnabled

    private fun alwaysOnEnabled(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isAlwaysOn

    override fun onDestroy() {
        closeTunnel("Service stopped"); runCatching {
            if (callbackRegistered) connectivity.unregisterNetworkCallback(callback)
        }; serviceScope.cancel(); super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    private fun Intent.toVpnConfiguration(): PersistedVpnConfiguration? {
        val address = getStringExtra(EXTRA_RESOLVER)?.takeIf { it.isNotBlank() } ?: return null
        val selectedProtocol = getStringExtra(EXTRA_PROTOCOL)?.let { stored ->
            runCatching { DnsProtocol.valueOf(stored) }.getOrNull()
        } ?: return null
        val selectedEndpoint = getStringExtra(EXTRA_ENDPOINT)?.takeIf { it.isNotBlank() }
            ?: return null
        return PersistedVpnConfiguration(
            resolver = address,
            name = getStringExtra(EXTRA_NAME),
            protocol = selectedProtocol,
            endpoint = selectedEndpoint
        )
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(CHANNEL_VISIBLE)
        val channel = NotificationChannel(
            CHANNEL_SILENT,
            "VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_SILENT).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("iNGenDNS ${protocol.name} DNS active")
            .setContentText("Using ${resolverName ?: resolver}")
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    companion object {
        const val ACTION_STOP = "stop"
        const val EXTRA_RESOLVER = "resolver"
        const val EXTRA_NAME = "name"
        const val EXTRA_PROTOCOL = "protocol"
        const val EXTRA_ENDPOINT = "endpoint"
        const val EXTRA_IS_FAILOVER = "is_failover"
        private const val CHANNEL_SILENT = "ingendns_vpn_silent"
        private const val CHANNEL_VISIBLE = "ingendns_vpn_status"
        private const val NOTIFICATION_ID = Constants.VPN_NOTIFICATION_ID
        // Two failed live queries tolerate a brief packet loss without leaving users
        // on a dead resolver for the previous 10-second grace period. This policy is
        // shared by DoH and DoT; their individual socket timeouts remain 3 seconds.
        private const val FAILURE_THRESHOLD = 2
        private const val CONNECTION_WATCHDOG_MILLIS = 7_000L
        private const val FAILOVER_DELAY_MILLIS = 2_000L
        const val LOCKDOWN_MESSAGE =
            "Internet is blocked because Block connections without VPN is ON. Turn it OFF in Android VPN settings, then retry."
        private val CONNECTION_TEST_QUERY = byteArrayOf(
            0x56, 0x47, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x07,
            'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(),
            'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(),
            'e'.code.toByte(), 0x03, 'c'.code.toByte(), 'o'.code.toByte(),
            'm'.code.toByte(), 0x00, 0x00, 0x01, 0x00, 0x01
        )
        fun start(
            context: Context,
            address: String,
            name: String? = null,
            protocol: DnsProtocol,
            endpoint: String,
            isFailover: Boolean = false
        ) {
            VpnConfigurationStore(context).save(
                PersistedVpnConfiguration(address, name, protocol, endpoint)
            )
            context.startForegroundService(
                Intent(context, DnsVpnService::class.java)
                    .putExtra(EXTRA_RESOLVER, address)
                    .putExtra(EXTRA_NAME, name)
                    .putExtra(EXTRA_PROTOCOL, protocol.name)
                    .putExtra(EXTRA_ENDPOINT, endpoint)
                    .putExtra(EXTRA_IS_FAILOVER, isFailover)
            )
        }

        fun stop(context: Context) {
            VpnConfigurationStore(context).clear()
            context.stopService(Intent(context, DnsVpnService::class.java))
            DnsVpnState.disconnected()
        }

        fun removeObsoleteNotificationChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java)
                .deleteNotificationChannel(CHANNEL_VISIBLE)
        }

    }
}
