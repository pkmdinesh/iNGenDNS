package com.ingendns.app.dns.benchmark

data class BenchmarkSummary(
    val averageLatency: Long,
    val successRate: Float,
    val successfulAttempts: Int,
    val totalAttempts: Int
)