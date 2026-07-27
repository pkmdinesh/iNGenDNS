package com.ingendns.app.dns.model

data class DnsTestResult(

    val server: DnsServer,

    val latency: Long,

    val successRate: Float,

    val timestamp: Long,

    val reachable: Boolean,

    val lowestLatency: Long = latency,

    val jitter: Long = 0,

    val reliability: Float = successRate,

    val packetLoss: Float = 100f - successRate,

    val score: Int = 0,

    val sessionId: String = ""

)
