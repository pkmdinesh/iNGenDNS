package com.ingendns.app.dns.benchmark

data class BenchmarkResult(
    val latencyMs: Long,
    val success: Boolean
)