package com.ingendns.app.dns.benchmark

import com.ingendns.app.util.Constants


data class BenchmarkConfig(
    val attempts: Int = Constants.DEFAULT_ATTEMPTS,
    val timeoutMillis: Int = Constants.DNS_TIMEOUT_MS,
    val delayBetweenAttemptsMillis: Long = Constants.DEFAULT_DELAY_MS
)