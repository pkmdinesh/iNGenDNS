package com.ingendns.app.dns.benchmark

import kotlin.math.roundToInt

data class QualityScoreBreakdown(
    val latencyScore: Double,
    val reliabilityScore: Double,
    val successScore: Double,
    val jitterScore: Double,
    val packetLossScore: Double,
    val weightedLatency: Double,
    val weightedReliability: Double,
    val weightedSuccess: Double,
    val weightedJitter: Double,
    val weightedPacketLoss: Double,
    val total: Int
)

object DnsQualityScore {
    fun calculate(
        averageLatency: Long,
        reliability: Float,
        successRate: Float,
        jitter: Long,
        packetLoss: Float
    ): Int = breakdown(averageLatency, reliability, successRate, jitter, packetLoss).total

    fun breakdown(
        averageLatency: Long,
        reliability: Float,
        successRate: Float,
        jitter: Long,
        packetLoss: Float
    ): QualityScoreBreakdown {
        val latencyScore = (100.0 - averageLatency * 0.4).coerceIn(0.0, 100.0)
        val reliabilityScore = reliability.toDouble().coerceIn(0.0, 100.0)
        val successScore = successRate.toDouble().coerceIn(0.0, 100.0)
        val jitterScore = (100.0 - jitter * 5.0).coerceIn(0.0, 100.0)
        val packetLossScore = (100.0 - packetLoss).coerceIn(0.0, 100.0)
        val weightedLatency = 0.40 * latencyScore
        val weightedReliability = 0.25 * reliabilityScore
        val weightedSuccess = 0.20 * successScore
        val weightedJitter = 0.10 * jitterScore
        val weightedPacketLoss = 0.05 * packetLossScore
        val total = (
            weightedLatency + weightedReliability + weightedSuccess +
                weightedJitter + weightedPacketLoss
            ).roundToInt().coerceIn(0, 100)
        return QualityScoreBreakdown(
            latencyScore = latencyScore,
            reliabilityScore = reliabilityScore,
            successScore = successScore,
            jitterScore = jitterScore,
            packetLossScore = packetLossScore,
            weightedLatency = weightedLatency,
            weightedReliability = weightedReliability,
            weightedSuccess = weightedSuccess,
            weightedJitter = weightedJitter,
            weightedPacketLoss = weightedPacketLoss,
            total = total
        )
    }

    fun rating(score: Int): String = when {
        score >= 90 -> "★★★★★ Excellent"
        score >= 80 -> "★★★★☆ Very Good"
        score >= 70 -> "★★★★☆ Good"
        score >= 60 -> "★★★☆☆ Fair"
        score >= 40 -> "★★☆☆☆ Poor"
        else -> "★☆☆☆☆ Very Poor"
    }
}
