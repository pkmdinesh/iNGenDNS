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
    val connectionFailed: Boolean = false
)

object DnsVpnState {
    private val mutableState = MutableStateFlow(VpnState())
    val state = mutableState.asStateFlow()

    fun tunnelStarted(resolver: String, name: String?, protocol: DnsProtocol, hostname: String) {
        mutableState.value = VpnState(true, resolver, name, protocol, hostname, false)
    }

    fun encryptedConnected() {
        mutableState.value = mutableState.value.copy(
            encryptedConnected = true,
            connectionFailed = false
        )
    }

    fun connectionFailed() {
        mutableState.value = mutableState.value.copy(
            encryptedConnected = false,
            connectionFailed = true
        )
    }

    fun disconnected() {
        mutableState.value = VpnState()
    }
}
