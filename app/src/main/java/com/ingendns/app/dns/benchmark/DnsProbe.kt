package com.ingendns.app.dns.benchmark

import com.ingendns.app.dns.model.DnsServer

interface DnsProbe {

    suspend fun probe(
        server: DnsServer,
        timeoutMillis: Int,
        recordType: Int
    ): BenchmarkResult

}
