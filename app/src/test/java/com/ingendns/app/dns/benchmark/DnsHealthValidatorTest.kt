package com.ingendns.app.dns.benchmark

import com.ingendns.app.dns.model.DnsServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsHealthValidatorTest {
    private val server = DnsServer("Test", "1.1.1.1")

    @Test
    fun `healthy requires both DNS and HTTPS`() = runBlocking {
        val probe = object : DnsProbe {
            override suspend fun probe(server: DnsServer, timeoutMillis: Int, recordType: Int) = BenchmarkResult(12, true)
        }
        assertTrue(DnsHealthValidator(probe) { true }.validate(server).healthy)
        assertFalse(DnsHealthValidator(probe) { false }.validate(server).healthy)
    }
}
