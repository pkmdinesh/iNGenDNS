package com.ingendns.app.dns.benchmark

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class DnsQualityScoreTest {
    @Test
    fun exampleMetricsProduceScore96() {
        assertEquals(
            96,
            DnsQualityScore.calculate(
                averageLatency = 18,
                reliability = 100f,
                successRate = 99f,
                jitter = 2,
                packetLoss = 0f
            )
        )
    }

    @Test
    fun scoreIsClampedToValidRange() {
        assertEquals(0, DnsQualityScore.calculate(1_000, 0f, 0f, 100, 100f))
        assertEquals(100, DnsQualityScore.calculate(0, 100f, 100f, 0, 0f))
    }

    @Test
    fun breakdownContributionsMatchFinalScore() {
        val result = DnsQualityScore.breakdown(18, 100f, 99f, 2, 1f)

        assertEquals(
            result.total,
            (result.weightedLatency + result.weightedReliability + result.weightedSuccess +
                result.weightedJitter + result.weightedPacketLoss).roundToInt()
        )
    }
}
