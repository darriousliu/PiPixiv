package com.mrl.pixiv.common.network

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import javax.net.SocketFactory

internal object DirectSocketFactory : SocketFactory() {
    override fun createSocket(): Socket = Socket(Proxy.NO_PROXY)

    override fun createSocket(host: String, port: Int): Socket =
        connect(InetSocketAddress(host, port))

    override fun createSocket(address: InetAddress, port: Int): Socket =
        connect(InetSocketAddress(address, port))

    override fun createSocket(host: String, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        connect(InetSocketAddress(host, port), InetSocketAddress(localAddress, localPort))

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        connect(InetSocketAddress(address, port), InetSocketAddress(localAddress, localPort))

    private fun connect(remote: InetSocketAddress, local: InetSocketAddress? = null): Socket =
        createSocket().also { socket ->
            try {
                if (local != null) socket.bind(local)
                socket.connect(remote)
            } catch (exception: Exception) {
                socket.close()
                throw exception
            }
        }
}
