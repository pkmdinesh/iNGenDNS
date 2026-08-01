package com.ingendns.app.vpn

import java.nio.ByteBuffer

internal object Ipv4UdpPacketBuilder {
    fun response(
        sourceAddress: ByteArray,
        destinationPacket: ByteArray,
        destinationAddressOffset: Int,
        sourcePort: Int,
        destinationPort: Int,
        payload: ByteArray
    ): ByteArray {
        require(sourceAddress.size == 4) { "IPv4 source required" }
        require(destinationAddressOffset >= 0 &&
            destinationAddressOffset <= destinationPacket.size - IPV4_ADDRESS_LENGTH
        ) { "IPv4 destination required" }
        val length = IPV4_HEADER_LENGTH + UDP_HEADER_LENGTH + payload.size
        val output = ByteBuffer.allocate(length)
        output.put(0x45).put(0).putShort(length.toShort()).putInt(0)
            .put(DEFAULT_TTL).put(UDP_PROTOCOL).putShort(0)
            .put(sourceAddress)
            .put(destinationPacket, destinationAddressOffset, IPV4_ADDRESS_LENGTH)
            .putShort(sourcePort.toShort()).putShort(destinationPort.toShort())
            .putShort((UDP_HEADER_LENGTH + payload.size).toShort()).putShort(0)
            .put(payload)
        val packet = output.array()
        val checksum = ipv4HeaderChecksum(packet)
        packet[10] = (checksum ushr 8).toByte()
        packet[11] = checksum.toByte()
        return packet
    }

    internal fun ipv4HeaderChecksum(packet: ByteArray): Int {
        var sum = 0
        for (index in 0 until IPV4_HEADER_LENGTH step 2) {
            sum += ((packet[index].toInt() and 0xff) shl 8) or
                (packet[index + 1].toInt() and 0xff)
        }
        while (sum ushr 16 != 0) sum = (sum and 0xffff) + (sum ushr 16)
        return sum.inv() and 0xffff
    }

    private const val IPV4_HEADER_LENGTH = 20
    private const val IPV4_ADDRESS_LENGTH = 4
    private const val UDP_HEADER_LENGTH = 8
    private const val DEFAULT_TTL: Byte = 64
    private const val UDP_PROTOCOL: Byte = 17
}
