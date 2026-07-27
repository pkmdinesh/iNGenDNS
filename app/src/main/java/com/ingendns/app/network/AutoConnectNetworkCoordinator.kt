package com.ingendns.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.ingendns.app.settings.AppSettings
import com.ingendns.app.vpn.DnsVpnState
import com.ingendns.app.workers.DnsWorkScheduler

/** Enqueues an immediate reconnect when the default connection changes to mobile data. */
class AutoConnectNetworkCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
    private val settings = AppSettings(appContext)
    private var cellularActive = false
    private var lastEnqueuedAt = 0L
    private var started = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = evaluate(network)

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            evaluate(network, capabilities)

        override fun onLost(network: Network) {
            val active = connectivity.activeNetwork
            evaluate(active, active?.let(connectivity::getNetworkCapabilities))
        }
    }

    @Synchronized
    fun start() {
        if (started) return
        connectivity.registerDefaultNetworkCallback(callback)
        started = true
        val active = connectivity.activeNetwork
        evaluate(active, active?.let(connectivity::getNetworkCapabilities))
    }

    @Synchronized
    fun stop() {
        if (!started) return
        runCatching { connectivity.unregisterNetworkCallback(callback) }
        cellularActive = false
        started = false
    }

    private fun evaluate(
        network: Network?,
        capabilities: NetworkCapabilities? = network?.let(connectivity::getNetworkCapabilities)
    ) {
        // A DNS VPN becoming the default network is not a physical handover. Ignoring
        // it prevents the VPN from triggering its own Auto Connect restart loop.
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) return

        val isCellular = capabilities?.let {
            it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                !it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } == true

        val shouldEnqueue = synchronized(this) {
            val transitionedToCellular = isCellular && !cellularActive
            cellularActive = isCellular
            val now = System.currentTimeMillis()
            val enqueue =
                transitionedToCellular && now - lastEnqueuedAt >= RECONNECT_DEBOUNCE_MILLIS
            if (enqueue) lastEnqueuedAt = now
            enqueue
        }
        if (shouldEnqueue && settings.autoConnectEnabled && !DnsVpnState.state.value.active) {
            DnsWorkScheduler.runNow(appContext)
        }
    }

    private companion object {
        const val RECONNECT_DEBOUNCE_MILLIS = 30_000L
    }
}
