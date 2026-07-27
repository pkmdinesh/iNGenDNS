package com.ingendns.app.domain.optimizer

import com.ingendns.app.dns.model.DnsTestResult

class DnsOptimizer {

    fun shouldSwitch(
        currentDns: String,
        results: List<DnsTestResult>
    ): Boolean {

        val best = results.firstOrNull()
            ?: return false

        return best.server.name != currentDns
    }
}