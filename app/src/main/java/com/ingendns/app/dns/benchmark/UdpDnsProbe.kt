package com.ingendns.app.dns.benchmark

import com.ingendns.app.dns.model.DnsServer
import com.ingendns.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.system.measureTimeMillis

class UdpDnsProbe : DnsProbe {

    override suspend fun probe(
        server: DnsServer,
        timeoutMillis: Int,
        recordType: Int
    ): BenchmarkResult = withContext(Dispatchers.IO) {

        try {

            val address = InetAddress.getByName(server.ip)

            DatagramSocket().use { socket ->

                socket.soTimeout = timeoutMillis

                val query = buildDnsQuery(recordType)

                val request = DatagramPacket(
                    query,
                    query.size,
                    address,
                    Constants.DNS_PORT
                )

                val responseBuffer = ByteArray(512)

                val response = DatagramPacket(
                    responseBuffer,
                    responseBuffer.size
                )

                val elapsed = measureTimeMillis {

                    socket.send(request)

                    socket.receive(response)

                }

                BenchmarkResult(
                    latencyMs = elapsed,
                    success = true
                )

            }

        } catch (_: Exception) {

            BenchmarkResult(
                latencyMs = Long.MAX_VALUE,
                success = false
            )

        }

    }

    /**
     * Simple DNS query for A record of example.com
     */
    internal fun buildDnsQuery(recordType: Int): ByteArray {

        return byteArrayOf(

            0x12, 0x34,
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,

            0x07,
            'e'.code.toByte(),
            'x'.code.toByte(),
            'a'.code.toByte(),
            'm'.code.toByte(),
            'p'.code.toByte(),
            'l'.code.toByte(),
            'e'.code.toByte(),

            0x03,
            'c'.code.toByte(),
            'o'.code.toByte(),
            'm'.code.toByte(),

            0x00,

            (recordType ushr 8).toByte(), recordType.toByte(),

            0x00, 0x01

        )

    }

}
