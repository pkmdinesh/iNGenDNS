package com.ingendns.app.dns.benchmark

import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.dns.model.DnsTestResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.math.abs

class DnsBenchmarkEngine(
    private val probe: DnsProbe,
    private val config: BenchmarkConfig = BenchmarkConfig()
) {
    private val recordTypes = intArrayOf(1, 28, 65, 16) // A, AAAA, HTTPS, TXT

    suspend fun benchmark(servers: List<DnsServer>): List<DnsTestResult> = coroutineScope {
        val timestamp = System.currentTimeMillis()
        servers.map { server ->
            async { benchmarkServer(server, timestamp) }
        }.awaitAll().filter { it.reachable }.sortedByDescending { it.score }
    }

    /** Benchmarks one newly saved profile and retains an unreachable result for UI feedback. */
    suspend fun benchmarkSingle(server: DnsServer): DnsTestResult =
        benchmarkServer(server, System.currentTimeMillis())

    private suspend fun benchmarkServer(server: DnsServer, timestamp: Long): DnsTestResult {
        val latencies = mutableListOf<Long>()
        repeat(config.attempts) { attempt ->
            val result = probe.probe(
                server,
                config.timeoutMillis,
                recordTypes[attempt % recordTypes.size]
            )
            if (result.success) latencies += result.latencyMs
            if (attempt + 1 < config.attempts) delay(config.delayBetweenAttemptsMillis)
        }

        val successRate = latencies.size * 100f / config.attempts
        val average = latencies.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: Long.MAX_VALUE
        val lowest = latencies.minOrNull() ?: Long.MAX_VALUE
        val jitter = latencies.takeIf { it.isNotEmpty() }
            ?.map { abs(it - latencies.average()) }?.average()?.toLong() ?: 0
        val median = latencies.sorted().let { sorted ->
            if (sorted.isEmpty()) 0.0
            else if (sorted.size % 2 == 1) sorted[sorted.size / 2].toDouble()
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        val stabilityWindow = maxOf(10.0, median * 0.5)
        val stableResponses = latencies.count { abs(it - median) <= stabilityWindow }
        val reliability = stableResponses * 100f / config.attempts
        val packetLoss = 100f - successRate
        val score = if (latencies.isEmpty()) 0 else DnsQualityScore.calculate(
            average, reliability, successRate, jitter, packetLoss
        )
        return DnsTestResult(
            server = server,
            latency = average,
            successRate = successRate,
            timestamp = timestamp,
            reachable = latencies.isNotEmpty(),
            lowestLatency = lowest,
            jitter = jitter,
            reliability = reliability,
            packetLoss = packetLoss,
            score = score
        )
    }
}
