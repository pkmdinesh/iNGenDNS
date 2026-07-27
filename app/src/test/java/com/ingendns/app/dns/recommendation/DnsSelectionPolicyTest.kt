package com.ingendns.app.dns.recommendation

import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.vpn.DnsProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsSelectionPolicyTest {
    private val managed = DnsServer("Managed", "1.1.1.1", dotHostname = "managed.test")
    private val fasterManaged = DnsServer("Faster", "2.2.2.2", dotHostname = "faster.test")
    private val custom = DnsServer(
        "Custom", "3.3.3.3", isCustom = true, dotHostname = "custom.test"
    )

    @Test
    fun selectsHighestScoringEligibleResult() {
        val selected = DnsSelectionPolicy.bestEligible(
            listOf(result(managed, 80, 20), result(fasterManaged, 80, 10)),
            DnsProtocol.DOT
        )

        assertEquals(fasterManaged, selected?.server)
    }

    @Test
    fun includesCustomProfilesInAutomaticSelection() {
        val selected = DnsSelectionPolicy.bestEligible(
            listOf(result(managed, 69, 5), result(custom, 100, 1)),
            DnsProtocol.DOT
        )

        assertEquals(custom, selected?.server)
    }

    @Test
    fun rejectsResultsBelowMinimumScore() {
        val selected = DnsSelectionPolicy.bestEligible(
            listOf(result(managed, 69, 5)),
            DnsProtocol.DOT
        )

        assertNull(selected)
    }

    @Test
    fun automaticSelectionRetainsCurrentDnsWhenGainIsBelowFive() {
        val selected = DnsSelectionPolicy.bestAutomatic(
            listOf(result(managed, 88, 20), result(fasterManaged, 92, 10)),
            DnsProtocol.DOT,
            managed.ip
        )

        assertEquals(managed, selected?.server)
    }

    @Test
    fun automaticSelectionSwitchesWhenGainIsFive() {
        val selected = DnsSelectionPolicy.bestAutomatic(
            listOf(result(managed, 88, 20), result(fasterManaged, 93, 10)),
            DnsProtocol.DOT,
            managed.ip
        )

        assertEquals(fasterManaged, selected?.server)
    }

    @Test
    fun automaticSelectionRequestsLastConnectedFallbackWhenAllScoresAreBelowSeventy() {
        val selected = DnsSelectionPolicy.bestAutomatic(
            listOf(result(managed, 69, 20), result(fasterManaged, 68, 10)),
            DnsProtocol.DOT,
            managed.ip
        )

        assertNull(selected)
    }

    private fun result(server: DnsServer, score: Int, latency: Long) = DnsTestResult(
        server = server,
        latency = latency,
        successRate = 100f,
        timestamp = 1,
        reachable = true,
        score = score
    )
}
