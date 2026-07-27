package com.ingendns.app.vpn

internal object VpnNetworkPolicy {
    fun shouldOpenTunnel(isCellular: Boolean, isValidated: Boolean): Boolean =
        isCellular && isValidated
}
