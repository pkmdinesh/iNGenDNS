package com.ingendns.app.dns.benchmark

import org.junit.Assert.assertEquals
import org.junit.Test

class UdpDnsProbeTest {
    @Test
    fun `question count stays one and requested type is encoded as QTYPE`() {
        val probe = UdpDnsProbe()

        listOf(1, 28, 65, 16).forEach { recordType ->
            val query = probe.buildDnsQuery(recordType)

            assertEquals(0, query[4].toInt() and 0xff)
            assertEquals(1, query[5].toInt() and 0xff)
            assertEquals(recordType ushr 8, query[query.size - 4].toInt() and 0xff)
            assertEquals(recordType and 0xff, query[query.size - 3].toInt() and 0xff)
            assertEquals(0, query[query.size - 2].toInt() and 0xff)
            assertEquals(1, query[query.size - 1].toInt() and 0xff)
        }
    }
}
