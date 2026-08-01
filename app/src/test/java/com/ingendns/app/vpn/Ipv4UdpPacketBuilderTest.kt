package com.ingendns.app.vpn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Ipv4UdpPacketBuilderTest {
    @Test
    fun `builds valid IPv4 UDP DNS response`() {
        val payload = byteArrayOf(0x12, 0x34, 0x01, 0x00)
        val inputPacket = ByteArray(24).apply {
            this[14] = 10
            this[15] = 10
            this[16] = 0
            this[17] = 2
        }
        val packet = Ipv4UdpPacketBuilder.response(
            sourceAddress = byteArrayOf(1, 1, 1, 1),
            destinationPacket = inputPacket,
            destinationAddressOffset = 14,
            sourcePort = 53,
            destinationPort = 49152,
            payload = payload
        )
        assertEquals(28 + payload.size, packet.size)
        assertEquals(17, packet[9].toInt())
        assertEquals(0, Ipv4UdpPacketBuilder.ipv4HeaderChecksum(packet))
        assertArrayEquals(payload, packet.copyOfRange(28, packet.size))
    }
}
