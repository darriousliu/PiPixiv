package com.mrl.pixiv.common.network

import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NoSniSslSocketFactoryTest {
    @Test
    fun layeredSocketReusesRawConnectionAndPassesIpAsPeerHost() {
        val delegate = RecordingSslSocketFactory()
        val factory = NoSniSslSocketFactory(delegate)
        val remoteAddress = InetAddress.getByAddress(
            byteArrayOf(210.toByte(), 140.toByte(), 139.toByte(), 155.toByte())
        )
        val rawSocket = ConnectedSocket(remoteAddress)

        factory.createSocket(
            rawSocket,
            "app-api.pixiv.net",
            443,
            true,
        ).use { result ->
            assertSame(rawSocket, delegate.layeredSocket)
            assertEquals("210.140.139.155", delegate.layeredHost)
            assertEquals(443, delegate.layeredPort)
            assertEquals(true, delegate.layeredAutoClose)
            assertTrue(result is SSLSocket)
            assertTrue(result.sslParameters.serverNames.orEmpty().isEmpty())
        }
    }

    @Test
    fun allFactoryMethodsDelegateInsteadOfReturningNull() {
        val delegate = RecordingSslSocketFactory()
        val factory = NoSniSslSocketFactory(delegate)
        val loopback = InetAddress.getLoopbackAddress()

        val sockets = listOf(
            factory.createSocket(),
            factory.createSocket("127.0.0.1", 443),
            factory.createSocket("127.0.0.1", 443, loopback, 0),
            factory.createSocket(loopback, 443),
            factory.createSocket(loopback, 443, loopback, 0),
        )
        sockets.forEach {
            assertTrue(it is SSLSocket)
            it.close()
        }
        assertEquals(5, delegate.directCalls)
    }

    @Test
    fun cipherSuitesComeFromDelegate() {
        val delegate = RecordingSslSocketFactory()
        val factory = NoSniSslSocketFactory(delegate)

        assertContentEquals(delegate.defaultCipherSuites, factory.defaultCipherSuites)
        assertContentEquals(delegate.supportedCipherSuites, factory.supportedCipherSuites)
    }

    @Test
    fun layeredSocketRequiresConnectedRawSocket() {
        val factory = NoSniSslSocketFactory(RecordingSslSocketFactory())

        assertFailsWith<java.net.SocketException> {
            factory.createSocket(Socket(), "app-api.pixiv.net", 443, true)
        }
    }
}

private class ConnectedSocket(
    private val remoteAddress: InetAddress,
) : Socket() {
    override fun getInetAddress(): InetAddress = remoteAddress
}

private class RecordingSslSocketFactory : SSLSocketFactory() {
    var layeredSocket: Socket? = null
    var layeredHost: String? = null
    var layeredPort: Int? = null
    var layeredAutoClose: Boolean? = null
    var directCalls: Int = 0

    override fun getDefaultCipherSuites(): Array<String> =
        arrayOf("default-cipher")

    override fun getSupportedCipherSuites(): Array<String> =
        arrayOf("supported-cipher")

    override fun createSocket(): Socket {
        directCalls++
        return newSslSocket()
    }

    override fun createSocket(
        socket: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean,
    ): Socket {
        layeredSocket = socket
        layeredHost = host
        layeredPort = port
        layeredAutoClose = autoClose
        return newSslSocket()
    }

    override fun createSocket(host: String?, port: Int): Socket {
        directCalls++
        return newSslSocket()
    }

    override fun createSocket(
        host: String?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket {
        directCalls++
        return newSslSocket()
    }

    override fun createSocket(address: InetAddress?, port: Int): Socket {
        directCalls++
        return newSslSocket()
    }

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket {
        directCalls++
        return newSslSocket()
    }

    private fun newSslSocket(): SSLSocket =
        SSLContext.getDefault().socketFactory.createSocket() as SSLSocket
}
