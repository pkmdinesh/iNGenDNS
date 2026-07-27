package com.ingendns.app.dns.benchmark

import com.ingendns.app.dns.model.DnsServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsBenchmarkEngineTest {
    @Test
    fun `filters unreachable resolvers and sorts by latency`() = runBlocking {
        val fast = DnsServer("Fast", "1.1.1.1")
        val slow = DnsServer("Slow", "8.8.8.8")
        val failed = DnsServer("Failed", "192.0.2.1")
        val probe = object : DnsProbe {
            override suspend fun probe(server: DnsServer, timeoutMillis: Int, recordType: Int) = when (server) {
                fast -> BenchmarkResult(10, true)
                slow -> BenchmarkResult(40, true)
                else -> BenchmarkResult(0, false)
            }
        }
        val results = DnsBenchmarkEngine(
            probe,
            BenchmarkConfig(attempts = 3, timeoutMillis = 100, delayBetweenAttemptsMillis = 0)
        ).benchmark(listOf(slow, failed, fast))

        assertEquals(listOf(fast, slow), results.map { it.server })
        assertTrue(results.all { it.successRate == 100f })
    }
}
