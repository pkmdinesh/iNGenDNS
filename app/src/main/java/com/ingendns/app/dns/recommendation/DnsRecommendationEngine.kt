package com.ingendns.app.dns.recommendation

import com.ingendns.app.dns.model.DnsTestResult

class DnsRecommendationEngine {

    fun getBest(results: List<DnsTestResult>): DnsTestResult? {

        return results
            .filter { it.reachable }
            .minByOrNull { score(it) }
    }

    private fun score(result: DnsTestResult): Double {

        val latencyScore = result.latency.toDouble()

        val reliabilityPenalty =
            (100f - result.successRate) * 5

        return latencyScore + reliabilityPenalty
    }
}