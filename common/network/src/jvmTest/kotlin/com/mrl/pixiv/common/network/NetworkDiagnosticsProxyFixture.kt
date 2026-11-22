package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** A loopback-only proxy that forwards tunnels exclusively to the fixture's HTTPS server. */
internal class NetworkDiagnosticsProxyFixture(
    private val type: BypassSetting.Proxy.ProxyType,
    private val originPort: Int,
    private val reject: Boolean = false,
) : AutoCloseable {
    private val loopback = InetAddress.getByName("127.0.0.1")
    private val listener = ServerSocket(0, 10, loopback)
    private val executor = Executors.newCachedThreadPool { work ->
        Thread(work, "network-diagnostics-proxy").apply { isDaemon = true }
    }
    private val sockets = ConcurrentHashMap.newKeySet<Socket>()
    private val connects = CopyOnWriteArrayList<Pair<String, Int>>()
    val setting = BypassSetting.Proxy("127.0.0.1", listener.localPort, type)
    val destinations: List<Pair<String, Int>>
        get() = connects.toList()

    init {
        executor.execute {
            while (!listener.isClosed) {
                val client = runCatching { listener.accept() }.getOrNull() ?: break
                sockets += client
                executor.execute { runCatching { serve(client) } }
            }
        }
    }

    private fun serve(client: Socket) {
        try {
            client.use {
                client.soTimeout = 5_000
                val input = DataInputStream(BufferedInputStream(client.getInputStream()))
                val destination = when (type) {
                    BypassSetting.Proxy.ProxyType.HTTP -> readConnect(input)
                    BypassSetting.Proxy.ProxyType.SOCKS -> readSocks(input, client)
                }
                connects += destination
                val (host, port) = destination
                check(port == originPort && host in setOf("localhost", "127.0.0.1"))
                if (reject) {
                    acknowledge(client, success = false)
                    return
                }
                Socket().use { upstream ->
                    sockets += upstream
                    try {
                        upstream.connect(InetSocketAddress(loopback, port), 2_000)
                        upstream.soTimeout = 5_000
                        acknowledge(client, success = true)
                        executor.execute { copy(input, upstream) }
                        copy(upstream.getInputStream(), client)
                    } finally {
                        sockets -= upstream
                    }
                }
            }
        } finally {
            sockets -= client
        }
    }

    private fun readConnect(input: DataInputStream): Pair<String, Int> {
        val request = readAsciiLine(input).split(' ')
        check(request.size == 3 && request[0] == "CONNECT")
        while (readAsciiLine(input).isNotEmpty()) {
            // Consume the HTTP CONNECT headers before opening the tunnel.
        }
        val authority = request[1]
        return authority.substringBeforeLast(':') to authority.substringAfterLast(':').toInt()
    }

    private fun readSocks(input: DataInputStream, client: Socket): Pair<String, Int> {
        check(input.readUnsignedByte() == 5)
        val methods = input.readNBytes(input.readUnsignedByte())
        check(0.toByte() in methods)
        client.getOutputStream().apply {
            write(byteArrayOf(5, 0))
            flush()
        }
        check(input.readUnsignedByte() == 5)
        check(input.readUnsignedByte() == 1)
        check(input.readUnsignedByte() == 0)
        val host = when (val addressType = input.readUnsignedByte()) {
            1 -> InetAddress.getByAddress(input.readNBytes(4)).hostAddress
            3 -> input.readNBytes(input.readUnsignedByte()).toString(Charsets.US_ASCII)
            4 -> InetAddress.getByAddress(input.readNBytes(16)).hostAddress
            else -> error("Invalid SOCKS5 address type: $addressType")
        }
        return host to input.readUnsignedShort()
    }

    private fun acknowledge(client: Socket, success: Boolean) {
        val reply = when (type) {
            BypassSetting.Proxy.ProxyType.HTTP -> {
                val response = if (success) {
                    "HTTP/1.1 200 Connection Established\r\n\r\n"
                } else {
                    "HTTP/1.1 407 Proxy Authentication Required\r\n" +
                        "Proxy-Authenticate: Basic realm=\"network-diagnostics\"\r\n" +
                        "Connection: close\r\nContent-Length: 0\r\n\r\n"
                }
                response.toByteArray(Charsets.US_ASCII)
            }
            BypassSetting.Proxy.ProxyType.SOCKS -> {
                val status: Byte = if (success) 0 else 5
                byteArrayOf(5, status, 0, 1, 127, 0, 0, 1, 0, 0)
            }
        }
        client.getOutputStream().apply {
            write(reply)
            flush()
        }
    }

    private fun readAsciiLine(input: DataInputStream): String = buildString {
        while (length < 8_192) {
            val value = input.readUnsignedByte()
            if (value == '\r'.code) {
                check(input.readUnsignedByte() == '\n'.code)
                return@buildString
            }
            append(value.toChar())
        }
        error("HTTP CONNECT line exceeds fixture limit")
    }

    private fun copy(input: java.io.InputStream, destination: Socket) {
        try {
            input.copyTo(destination.getOutputStream())
            destination.shutdownOutput()
        } catch (_: IOException) {
            // Socket closure also ends a tunnel during an interrupted request.
        }
    }

    override fun close() {
        listener.close()
        sockets.forEach(Socket::close)
        executor.shutdownNow()
    }
}
