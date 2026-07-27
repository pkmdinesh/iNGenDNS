package com.ingendns.app.dns.recommendation

import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.vpn.DnsProtocol

/** Rules shared by dashboard Start DNS, Auto Connect, and manual-DNS failover. */
object DnsSelectionPolicy {
    const val MINIMUM_AUTOMATIC_SCORE = 70
    const val MINIMUM_SWITCH_GAIN = 5

    fun bestEligible(
        results: List<DnsTestResult>,
        protocol: DnsProtocol,
        excludedIps: Set<String> = emptySet()
    ): DnsTestResult? = results
        .asSequence()
        .filter { result ->
            result.reachable &&
                result.score >= MINIMUM_AUTOMATIC_SCORE &&
                result.server.ip !in excludedIps &&
                result.server.supports(protocol)
        }
        .sortedWith(compareByDescending<DnsTestResult> { it.score }.thenBy { it.latency })
        .firstOrNull()

    /**
     * Selects an automatic candidate with a five-point hysteresis. A null result
     * means no resolver reached the automatic threshold and the caller should
     * retain the last successfully connected DNS.
     */
    fun bestAutomatic(
        results: List<DnsTestResult>,
        protocol: DnsProtocol,
        currentIp: String?
    ): DnsTestResult? {
        val ranked = results
            .asSequence()
            .filter { it.reachable && it.server.supports(protocol) }
            .sortedWith(compareByDescending<DnsTestResult> { it.score }.thenBy { it.latency })
            .toList()
        val highest = ranked.firstOrNull()?.takeIf { it.score >= MINIMUM_AUTOMATIC_SCORE }
            ?: return null
        val current = currentIp?.let { ip -> ranked.firstOrNull { it.server.ip == ip } }
        return if (
            current != null && current.server.ip != highest.server.ip &&
            highest.score - current.score < MINIMUM_SWITCH_GAIN
        ) current else highest
    }

    fun DnsServer.supports(protocol: DnsProtocol): Boolean = when (protocol) {
        DnsProtocol.DOT -> dotHostname.isNotBlank()
        DnsProtocol.DOH -> dohUrl.isNotBlank()
    }
}
