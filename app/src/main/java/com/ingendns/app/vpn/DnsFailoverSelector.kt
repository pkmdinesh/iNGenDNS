package com.ingendns.app.vpn

import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import com.ingendns.app.dns.recommendation.DnsSelectionPolicy

object DnsFailoverSelector {
    fun next(
        currentIp: String,
        protocol: DnsProtocol,
        rankedResults: List<DnsTestResult>,
        availableServers: List<DnsServer>,
        excludedIps: Set<String> = emptySet()
    ): DnsServer? {
        val serversByIp = availableServers.associateBy { it.ip }
        val candidates = rankedResults.map { result ->
            serversByIp[result.server.ip]?.let { result.copy(server = it) } ?: result
        }
        return DnsSelectionPolicy.bestEligible(
            results = candidates,
            protocol = protocol,
            excludedIps = excludedIps + currentIp
        )?.server
    }
}
