package com.ingendns.app.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VpnState(
    val active: Boolean = false,
    val resolver: String? = null,
    val name: String? = null,
    val protocol: DnsProtocol? = null,
    val hostname: String? = null,
    val encryptedConnected: Boolean = false,
    val connectionFailed: Boolean = false,
    val mode: VpnOperatingMode = VpnOperatingMode.INACTIVE,
    val statusMessage: String = "VPN inactive",
    val lockdownEnabled: Boolean = false,
    val alwaysOnEnabled: Boolean = false
)

enum class VpnOperatingMode { INACTIVE, CELLULAR_OPTIMIZED, WIFI_NETWORK_DNS, WAITING_FOR_NETWORK }

object DnsVpnState {
    private val mutableState = MutableStateFlow(VpnState())
    val state = mutableState.asStateFlow()

    fun tunnelStarted(
        resolver: String,
        name: String?,
        protocol: DnsProtocol?,
        hostname: String?,
        mode: VpnOperatingMode,
        statusMessage: String,
        lockdownEnabled: Boolean,
        alwaysOnEnabled: Boolean
    ) {
        mutableState.value = VpnState(
            active = true,
            resolver = resolver,
            name = name,
            protocol = protocol,
            hostname = hostname,
            mode = mode,
            statusMessage = statusMessage,
            lockdownEnabled = lockdownEnabled,
            alwaysOnEnabled = alwaysOnEnabled
        )
    }

    fun encryptedConnected() {
        mutableState.value = mutableState.value.copy(
            encryptedConnected = true,
            connectionFailed = false
        )
    }

    fun networkDnsConnected() {
        mutableState.value = mutableState.value.copy(
            encryptedConnected = false,
            connectionFailed = false
        )
    }

    fun connectionFailed() {
        mutableState.value = mutableState.value.copy(
            encryptedConnected = false,
            connectionFailed = true
        )
    }

    fun waiting(statusMessage: String, lockdownEnabled: Boolean) {
        mutableState.value = VpnState(
            mode = VpnOperatingMode.WAITING_FOR_NETWORK,
            statusMessage = statusMessage,
            connectionFailed = true,
            lockdownEnabled = lockdownEnabled
        )
    }

    fun disconnected() {
        mutableState.value = VpnState()
    }
}
