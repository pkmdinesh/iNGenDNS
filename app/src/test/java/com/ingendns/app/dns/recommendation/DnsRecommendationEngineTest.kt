package com.ingendns.app.dns.recommendation

import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsRecommendationEngineTest {
    @Test
    fun `ignores unreachable and penalizes unreliable resolvers`() {
        val fastUnreliable = result("Fast", 10, 40f, true)
        val reliable = result("Reliable", 25, 100f, true)
        val failed = result("Failed", 1, 0f, false)
        assertEquals("Reliable", DnsRecommendationEngine().getBest(listOf(fastUnreliable, failed, reliable))?.server?.name)
        assertNull(DnsRecommendationEngine().getBest(listOf(failed)))
    }

    private fun result(name: String, latency: Long, success: Float, reachable: Boolean) =
        DnsTestResult(DnsServer(name, "1.1.1.1"), latency, success, 1L, reachable)
}
