package com.ingendns.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties
import android.net.NetworkRequest
import android.os.Build
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class NetworkState(
    val transport: Transport = Transport.NONE,
    val hasInternet: Boolean = false,
    val networkName: String? = null,
    val dnsServers: List<String> = emptyList(),
    val privateDnsActive: Boolean = false,
    val privateDnsHostname: String? = null,
    val lteSignal: CellularSignal? = null,
    val nrSignal: CellularSignal? = null,
    val connectedNetwork: String? = null
) {
    val label: String
        get() = when {
            !hasInternet -> "No Internet"
            transport == Transport.WIFI -> "Wi-Fi Connected"
            transport == Transport.CELLULAR -> "Mobile Data"
            else -> "Connected"
        }
}

data class CellularSignal(
    val technology: String,
    val dbm: Int,
    val asu: Int,
    val quality: String
)

enum class Transport { WIFI, CELLULAR, NONE }

class NetworkMonitor(private val context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private val observedNetworks = ConcurrentHashMap<Network, NetworkCapabilities>()
    @Volatile
    private var latestSignalStrength: SignalStrength? = null
    @Volatile
    private var latestNetworkType: Int = TelephonyManager.NETWORK_TYPE_UNKNOWN
    @Volatile
    // OVERRIDE_NETWORK_TYPE_NONE is an inlined zero, but referencing the API 30
    // field directly produces an avoidable warning on our Android 8 minimum.
    private var latestOverrideNetworkType: Int = 0
    private var started = false
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<NetworkState> = _state.asStateFlow()

    private val callback = createCallback()

    private val telephonyCallback: TelephonyCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener,
                TelephonyCallback.DisplayInfoListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    updateSignalStrength(signalStrength)
                }

                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                    updateDisplayInfo(telephonyDisplayInfo)
                }
            }
        } else {
            null
        }

    @Suppress("DEPRECATION")
    private val phoneStateListener: PhoneStateListener? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    updateSignalStrength(signalStrength)
                }

                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                    updateDisplayInfo(telephonyDisplayInfo)
                }
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    updateSignalStrength(signalStrength)
                }

                override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                    latestNetworkType = networkType
                    refresh()
                }
            }
        } else {
            null
        }

    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    @Synchronized
    fun start() {
        if (started) return
        connectivity.registerNetworkCallback(request, callback)
        registerSignalStrengthListener()
        started = true
        refresh()
    }

    @Synchronized
    fun stop() {
        if (!started) return
        runCatching { connectivity.unregisterNetworkCallback(callback) }
        unregisterSignalStrengthListener()
        observedNetworks.clear()
        started = false
    }
    fun refreshNow() = refresh()

    @Suppress("DEPRECATION")
    private fun registerSignalStrengthListener() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let {
                    telephonyManager.registerTelephonyCallback(context.mainExecutor, it)
                }
            } else {
                phoneStateListener?.let {
                    val events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED
                        } else {
                            PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        }
                    telephonyManager.listen(it, events)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun unregisterSignalStrengthListener() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let(telephonyManager::unregisterTelephonyCallback)
            } else {
                phoneStateListener?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        }
    }

    private fun updateSignalStrength(signalStrength: SignalStrength) {
        latestSignalStrength = signalStrength
        refresh()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun updateDisplayInfo(displayInfo: TelephonyDisplayInfo) {
        latestNetworkType = displayInfo.networkType
        latestOverrideNetworkType = displayInfo.overrideNetworkType
        refresh()
    }

    private fun createCallback(): ConnectivityManager.NetworkCallback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(
                ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO
            ) {
                override fun onAvailable(network: Network) = handleAvailable(network)
                override fun onLost(network: Network) = handleLost(network)
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) = handleCapabilitiesChanged(network, capabilities)
                override fun onLinkPropertiesChanged(
                    network: Network,
                    linkProperties: LinkProperties
                ) = refresh()
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = handleAvailable(network)
                override fun onLost(network: Network) = handleLost(network)
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) = handleCapabilitiesChanged(network, capabilities)
                override fun onLinkPropertiesChanged(
                    network: Network,
                    linkProperties: LinkProperties
                ) = refresh()
            }
        }

    private fun handleAvailable(network: Network) {
        connectivity.getNetworkCapabilities(network)?.let {
            observedNetworks[network] = it
        }
        refresh()
    }

    private fun handleLost(network: Network) {
        observedNetworks.remove(network)
        refresh()
    }

    private fun handleCapabilitiesChanged(
        network: Network,
        capabilities: NetworkCapabilities
    ) {
        if (capabilities.isPhysicalInternetNetwork()) {
            observedNetworks[network] = capabilities
        } else {
            observedNetworks.remove(network)
        }
        refresh()
    }

    private fun refresh() {
        _state.value = readState()
    }

    private fun readState(): NetworkState {
        val active = connectivity.activeNetwork
        if (active != null) {
            val activeCapabilities = observedNetworks[active]
                ?: connectivity.getNetworkCapabilities(active)
            if (activeCapabilities?.isPhysicalInternetNetwork() == true) {
                return active.toState(activeCapabilities)
            }
        }
        val candidates = LinkedHashMap<Network, NetworkCapabilities>()
        observedNetworks.forEach { (network, capabilities) ->
            candidates[network] = capabilities
        }
        buildList {
            if (active != null) add(active)
            addAll(connectivity.allNetworks)
        }.forEach { network ->
            connectivity.getNetworkCapabilities(network)?.let { capabilities ->
                candidates[network] = capabilities
            }
        }
        val physicalNetwork = candidates.entries
            .map { it.key to it.value }
            .filter { (_, capabilities) ->
                capabilities.isPhysicalInternetNetwork()
            }
            .sortedWith(
                compareByDescending<Pair<Network, NetworkCapabilities>> { (_, capabilities) ->
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }.thenByDescending { (_, capabilities) ->
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            )
            .firstOrNull()
            ?: return NetworkState()
        val (network, capabilities) = physicalNetwork
        return network.toState(capabilities)
    }

    private fun NetworkCapabilities.isPhysicalInternetNetwork(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            !hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))

    private fun Network.toState(capabilities: NetworkCapabilities): NetworkState {
        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            else -> Transport.NONE
        }
        val linkProperties = connectivity.getLinkProperties(this)
        val signalStrength = latestSignalStrength
        val privateDnsActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProperties?.isPrivateDnsActive == true
        } else false
        val privateDnsHostname = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProperties?.privateDnsServerName
        } else null
        val lteSignal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            signalStrength?.cellSignalStrengths
                ?.filterIsInstance<CellSignalStrengthLte>()
                ?.maxByOrNull { it.level }
                ?.toCellularSignal("LTE")
        } else null
        val nrSignal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            signalStrength?.cellSignalStrengths
                ?.filterIsInstance<CellSignalStrengthNr>()
                ?.maxByOrNull { it.level }
                ?.toCellularSignal("NR")
        } else null
        return NetworkState(
            transport = transport,
            hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            networkName = when (transport) {
                // Reading the SSID requires location/Nearby Devices permission on
                // supported Android versions. Keep network detection permission-free.
                Transport.WIFI -> "Wi-Fi"
                Transport.CELLULAR -> activeDataProviderName()
                Transport.NONE -> null
            },
            dnsServers = linkProperties?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty(),
            privateDnsActive = privateDnsActive,
            privateDnsHostname = privateDnsHostname,
            lteSignal = lteSignal,
            nrSignal = nrSignal,
            connectedNetwork = if (transport == Transport.CELLULAR) {
                describeConnectedNetwork()
            } else {
                null
            }
        )
    }

    private fun activeDataProviderName(): String? {
        val subscriptionId = SubscriptionManager.getDefaultDataSubscriptionId()
        val activeTelephonyManager = if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            runCatching { telephonyManager.createForSubscriptionId(subscriptionId) }
                .getOrDefault(telephonyManager)
        } else {
            telephonyManager
        }
        val operator = runCatching { activeTelephonyManager.networkOperatorName }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: runCatching { activeTelephonyManager.simOperatorName }.getOrNull()
        return formatMobileNetworkName(operator)
    }

    private fun describeConnectedNetwork(): String {
        val strengths = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            latestSignalStrength?.cellSignalStrengths.orEmpty()
        } else emptyList()
        return describeCellularNetwork(
            networkType = latestNetworkType,
            overrideNetworkType = latestOverrideNetworkType,
            hasNrSignal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                strengths.any { it is CellSignalStrengthNr },
            hasLteSignal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                strengths.any { it is CellSignalStrengthLte }
        )
    }

    private fun android.telephony.CellSignalStrength.toCellularSignal(
        technology: String
    ): CellularSignal? {
        val measuredDbm = dbm
        val measuredAsu = asuLevel
        if (measuredDbm !in -160..-20 || measuredAsu !in 0..97) return null
        return CellularSignal(
            technology = technology,
            dbm = measuredDbm,
            asu = measuredAsu,
            quality = when (level) {
                4 -> "Excellent"
                3 -> "Good"
                2 -> "Fair"
                else -> "Poor"
            }
        )
    }

}

internal fun formatMobileNetworkName(operatorName: String?): String? = operatorName
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.let { operator ->
        if (operator.endsWith("Internet", ignoreCase = true)) operator
        else "$operator Internet"
    }

@Suppress("DEPRECATION")
internal fun describeCellularNetwork(
    networkType: Int,
    overrideNetworkType: Int,
    hasNrSignal: Boolean,
    hasLteSignal: Boolean
): String = when (overrideNetworkType) {
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> when {
        !hasNrSignal -> describeBaseNetwork(networkType, hasLteSignal)
        networkType == TelephonyManager.NETWORK_TYPE_NR -> "5G NR Advanced (SA)"
        else -> "5G NR Advanced (NSA)"
    }
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> if (hasNrSignal) {
        "5G NSA (LTE + NR)"
    } else {
        describeBaseNetwork(networkType, hasLteSignal)
    }
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> if (hasNrSignal) {
        "5G NSA (NR mmWave)"
    } else {
        describeBaseNetwork(networkType, hasLteSignal)
    }
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "4G++ (LTE Advanced Pro)"
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "4G+ (LTE CA)"
    else -> when {
        networkType == TelephonyManager.NETWORK_TYPE_NR -> "5G NR (SA)"
        hasNrSignal && networkType == TelephonyManager.NETWORK_TYPE_LTE -> "5G NSA (LTE + NR)"
        hasNrSignal -> "5G NR"
        else -> describeBaseNetwork(networkType, hasLteSignal)
    }
}

private fun describeBaseNetwork(networkType: Int, hasLteSignal: Boolean): String =
    when (networkType) {
        TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G (HSPA+)"
        TelephonyManager.NETWORK_TYPE_HSPA -> "3G (HSPA)"
        TelephonyManager.NETWORK_TYPE_HSDPA -> "3G (HSDPA)"
        TelephonyManager.NETWORK_TYPE_HSUPA -> "3G (HSUPA)"
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G (UMTS)"
        TelephonyManager.NETWORK_TYPE_EDGE -> "2G (EDGE)"
        TelephonyManager.NETWORK_TYPE_GPRS -> "2G (GPRS)"
        TelephonyManager.NETWORK_TYPE_GSM -> "2G (GSM)"
        else -> if (hasLteSignal) "4G (LTE)" else "Mobile Data"
    }
