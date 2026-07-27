package com.ingendns.app.domain.model

data class DnsAnalytics(
    val bestDns: String = "Unknown",
    val ipAddress: String = "—",
    val score: Int = 0,
    val averageLatency: Long = 0,
    val lowestLatency: Long = 0,
    val jitter: Long = 0,
    val successRate: Float = 0f,
    val reliability: Float = 0f,
    val packetLoss: Float = 0f
)
