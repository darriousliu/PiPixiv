package com.mrl.pixiv.common.network

import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SniReplacingSslSocketFactoryTest {
    @Test
    fun apiAndOauthReplaceSniWhilePreservingLayeredSocketContract() {
        for (host in listOf(API_HOST, AUTH_HOST)) {
            val delegate = RecordingSslSocketFactory()
            val factory = SniReplacingSslSocketFactory(delegate)
            val rawSocket = connectedSocket()

            factory.createSocket(
                rawSocket,
                host,
                443,
                false,
            ).use { result ->
                assertSame(rawSocket, delegate.layeredSocket)
                assertEquals(PIXIV_TLS_SERVER_NAME, delegate.layeredHost)
                assertEquals(443, delegate.layeredPort)
                assertEquals(false, delegate.layeredAutoClose)
                assertEquals(listOf(PIXIV_TLS_SERVER_NAME), result.serverNames())
            }
        }
    }

    @Test
    fun imageHostsDisableSni() {
        for (host in listOf(IMAGE_HOST, STATIC_IMAGE_HOST)) {
            val delegate = RecordingSslSocketFactory()
            val factory = SniReplacingSslSocketFactory(delegate)
            val rawSocket = connectedSocket()

            factory.createSocket(rawSocket, host, 443, true).use { result ->
                assertEquals(rawSocket.inetAddress.hostAddress, delegate.layeredHost)
                assertTrue(result.serverNames().isEmpty())
            }
        }
    }

    @Test
    fun unknownHostKeepsDelegateSni() {
        val delegate = RecordingSslSocketFactory()
        val factory = SniReplacingSslSocketFactory(delegate)

        factory.createSocket(connectedSocket(), "example.com", 443, true).use { result ->
            assertEquals(listOf("example.com"), result.serverNames())
        }
    }

    @Test
    fun addingAlpnDoesNotDiscardReplacementSni() {
        val factory = SniReplacingSslSocketFactory(RecordingSslSocketFactory())

        factory.createSocket(connectedSocket(), API_HOST, 443, true).use { result ->
            val sslSocket = result as SSLSocket
            sslSocket.sslParameters = sslSocket.sslParameters.apply {
                applicationProtocols = arrayOf("h2", "http/1.1")
            }

            assertEquals(listOf(PIXIV_TLS_SERVER_NAME), sslSocket.serverNames())
            assertContentEquals(
                arrayOf("h2", "http/1.1"),
                sslSocket.sslParameters.applicationProtocols,
            )
        }
    }

    @Test
    fun allFactoryMethodsDelegateInsteadOfReturningNull() {
        val delegate = RecordingSslSocketFactory()
        val factory = SniReplacingSslSocketFactory(delegate)
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
        val factory = SniReplacingSslSocketFactory(delegate)

        assertContentEquals(delegate.defaultCipherSuites, factory.defaultCipherSuites)
        assertContentEquals(delegate.supportedCipherSuites, factory.supportedCipherSuites)
    }

    @Test
    fun layeredSocketRequiresConnectedRawSocket() {
        val factory = SniReplacingSslSocketFactory(RecordingSslSocketFactory())

        assertFailsWith<java.net.SocketException> {
            factory.createSocket(Socket(), API_HOST, 443, true)
        }
    }

    private fun connectedSocket(): Socket {
        val remoteAddress = InetAddress.getByAddress(
            byteArrayOf(210.toByte(), 140.toByte(), 139.toByte(), 155.toByte())
        )
        return ConnectedSocket(remoteAddress)
    }

    private fun Socket.serverNames(): List<String> =
        (this as SSLSocket).sslParameters.serverNames.orEmpty().map {
            (it as SNIHostName).asciiName
        }

    private companion object {
        const val API_HOST = "app-api.pixiv.net"
        const val AUTH_HOST = "oauth.secure.pixiv.net"
        const val IMAGE_HOST = "i.pximg.net"
        const val STATIC_IMAGE_HOST = "s.pximg.net"
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
        return newSslSocket(host)
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

    private fun newSslSocket(host: String? = null): SSLSocket =
        (SSLContext.getDefault().socketFactory.createSocket() as SSLSocket).apply {
            if (host != null && host.any(Char::isLetter)) {
                sslParameters = sslParameters.apply {
                    serverNames = listOf(SNIHostName(host))
                }
            }
        }
}
