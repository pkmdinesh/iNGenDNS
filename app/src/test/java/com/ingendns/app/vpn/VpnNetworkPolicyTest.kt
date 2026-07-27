package com.ingendns.app.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnNetworkPolicyTest {
    @Test fun `opens only for validated cellular`() {
        assertTrue(VpnNetworkPolicy.shouldOpenTunnel(isCellular = true, isValidated = true))
        assertFalse(VpnNetworkPolicy.shouldOpenTunnel(isCellular = true, isValidated = false))
        assertFalse(VpnNetworkPolicy.shouldOpenTunnel(isCellular = false, isValidated = true))
    }
}
