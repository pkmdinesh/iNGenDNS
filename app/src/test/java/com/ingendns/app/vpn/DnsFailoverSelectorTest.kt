package com.ingendns.app.vpn

import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsFailoverSelectorTest {
    private val first = DnsServer("First", "1.1.1.1", dotHostname = "first.test", dohUrl = "https://first.test/dns-query")
    private val second = DnsServer("Second", "2.2.2.2", dotHostname = "second.test", dohUrl = "https://second.test/dns-query")
    private val dohOnly = DnsServer("DoH only", "3.3.3.3", dohUrl = "https://third.test/dns-query")

    @Test
    fun selectsNextHighestScoringCompatibleResolver() {
        val next = DnsFailoverSelector.next(
            currentIp = first.ip,
            protocol = DnsProtocol.DOT,
            rankedResults = listOf(result(second, 80), result(dohOnly, 90), result(first, 100)),
            availableServers = listOf(first, second, dohOnly)
        )

        assertEquals(second, next)
    }

    @Test
    fun wrapsFromLowestScoringResolverToHighestAlternative() {
        val next = DnsFailoverSelector.next(
            currentIp = second.ip,
            protocol = DnsProtocol.DOT,
            rankedResults = listOf(result(first, 100), result(second, 80)),
            availableServers = listOf(first, second)
        )

        assertEquals(first, next)
    }

    @Test
    fun excludesResolversThatAlreadyFailed() {
        val next = DnsFailoverSelector.next(
            currentIp = second.ip,
            protocol = DnsProtocol.DOH,
            rankedResults = listOf(result(first, 100), result(dohOnly, 90), result(second, 80)),
            availableServers = listOf(first, second, dohOnly),
            excludedIps = setOf(first.ip)
        )

        assertEquals(dohOnly, next)
    }

    @Test
    fun returnsNullWhenEveryAlternativeHasFailed() {
        val next = DnsFailoverSelector.next(
            currentIp = second.ip,
            protocol = DnsProtocol.DOT,
            rankedResults = listOf(result(first, 100), result(second, 80)),
            availableServers = listOf(first, second),
            excludedIps = setOf(first.ip)
        )

        assertNull(next)
    }

    @Test
    fun ignoresAlternativesBelowMinimumScore() {
        val next = DnsFailoverSelector.next(
            currentIp = first.ip,
            protocol = DnsProtocol.DOT,
            rankedResults = listOf(result(first, 90), result(second, 69)),
            availableServers = listOf(first, second)
        )

        assertNull(next)
    }

    private fun result(server: DnsServer, score: Int) = DnsTestResult(
        server = server,
        latency = 10,
        successRate = 100f,
        timestamp = 1,
        reachable = true,
        score = score
    )
}
