package com.ingendns.app.vpn

import android.util.Log
import com.ingendns.app.util.Constants
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class DnsForwarder {
    @Volatile private var dotSocket: SSLSocket? = null
    @Volatile private var dotKey: String? = null

    @Synchronized
    fun sendQuery(
        protocol: DnsProtocol,
        dnsIp: String,
        endpoint: String,
        request: ByteArray,
        requestOffset: Int = 0,
        requestLength: Int = request.size,
        protectSocket: (Socket) -> Boolean
    ): ByteArray? = runCatching {
        requireValidRange(request, requestOffset, requestLength)
        when (protocol) {
            DnsProtocol.DOT -> sendDot(
                dnsIp, endpoint, request, requestOffset, requestLength, protectSocket
            )
            DnsProtocol.DOH -> {
                closeDot()
                sendDoh(endpoint, request, requestOffset, requestLength)
            }
        }
    }.onFailure { Log.e("iNGenDNS", "Encrypted DNS forwarding failed", it) }.getOrNull()

    private fun sendDot(
        dnsIp: String,
        hostname: String,
        request: ByteArray,
        requestOffset: Int,
        requestLength: Int,
        protectSocket: (Socket) -> Boolean
    ): ByteArray {
        val key = "$dnsIp|$hostname"
        val socket = dotSocket?.takeIf {
            dotKey == key && it.isConnected && !it.isClosed
        } ?: openDot(dnsIp, hostname, protectSocket)
        return runCatching {
            exchangeDot(socket, request, requestOffset, requestLength)
        }.getOrElse {
            closeDot()
            if (Thread.currentThread().isInterrupted) throw it
            exchangeDot(
                openDot(dnsIp, hostname, protectSocket),
                request,
                requestOffset,
                requestLength
            )
        }
    }

    private fun openDot(
        dnsIp: String,
        hostname: String,
        protectSocket: (Socket) -> Boolean
    ): SSLSocket {
        closeDot()
        val raw = Socket()
        // The VPN builder excludes iNGenDNS itself, so this socket already uses the
        // underlying network. Some Android builds return false from protect() for an
        // already-excluded UID; protection is therefore a best-effort safeguard.
        protectSocket(raw)
        raw.tcpNoDelay = true
        raw.connect(InetSocketAddress(dnsIp, 853), Constants.DNS_TIMEOUT_MS)
        raw.soTimeout = Constants.DNS_TIMEOUT_MS
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val tls = factory.createSocket(raw, hostname, 853, true) as SSLSocket
        tls.soTimeout = Constants.DNS_TIMEOUT_MS
        tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
        tls.startHandshake()
        dotSocket = tls
        dotKey = "$dnsIp|$hostname"
        return tls
    }

    private fun exchangeDot(
        socket: SSLSocket,
        request: ByteArray,
        requestOffset: Int,
        requestLength: Int
    ): ByteArray {
        val output = DataOutputStream(socket.outputStream)
        output.writeShort(requestLength)
        output.write(request, requestOffset, requestLength)
        output.flush()
        val input = DataInputStream(socket.inputStream)
        val length = input.readUnsignedShort()
        return ByteArray(length).also(input::readFully)
    }

    fun close() = closeDot()

    fun sendPlain(
        dnsIp: String,
        request: ByteArray,
        requestOffset: Int = 0,
        requestLength: Int = request.size,
        protectSocket: (DatagramSocket) -> Boolean
    ): ByteArray? = runCatching {
        requireValidRange(request, requestOffset, requestLength)
        DatagramSocket().use { socket ->
            protectSocket(socket)
            socket.soTimeout = Constants.DNS_TIMEOUT_MS
            socket.connect(InetSocketAddress(dnsIp, Constants.DNS_PORT))
            socket.send(DatagramPacket(request, requestOffset, requestLength))
            val response = ByteArray(4_096)
            val packet = DatagramPacket(response, response.size)
            socket.receive(packet)
            response.copyOf(packet.length)
        }
    }.onFailure { Log.e("iNGenDNS", "Network DNS forwarding failed", it) }.getOrNull()

    private fun closeDot() {
        runCatching { dotSocket?.close() }
        dotSocket = null
        dotKey = null
    }

    private fun sendDoh(
        endpoint: String,
        request: ByteArray,
        requestOffset: Int,
        requestLength: Int
    ): ByteArray {
        val connection = URL(endpoint).openConnection() as HttpsURLConnection
        connection.connectTimeout = Constants.DNS_TIMEOUT_MS
        connection.readTimeout = Constants.DNS_TIMEOUT_MS
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setFixedLengthStreamingMode(requestLength)
        connection.setRequestProperty("Content-Type", "application/dns-message")
        connection.setRequestProperty("Accept", "application/dns-message")
        return runCatching {
            connection.outputStream.use { it.write(request, requestOffset, requestLength) }
            require(connection.responseCode == 200) {
                "DoH returned HTTP ${connection.responseCode}"
            }
            // Closing both streams returns a healthy HTTPS socket to Android's
            // connection pool. Calling disconnect() here would discourage reuse.
            connection.inputStream.use { it.readBytes() }
        }.onFailure {
            connection.disconnect()
        }.getOrThrow()
    }

    private fun requireValidRange(buffer: ByteArray, offset: Int, length: Int) {
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length) {
            "Invalid DNS request range"
        }
    }
}
