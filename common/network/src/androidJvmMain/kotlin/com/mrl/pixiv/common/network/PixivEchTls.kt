package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.data.Constants.AUTH_HOST
import org.conscrypt.Conscrypt
import org.conscrypt.ConscryptNetworkSecurityPolicy
import org.conscrypt.DomainEncryptionMode
import org.conscrypt.NetworkSecurityPolicy
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal object PixivEchTls {
    private val context by lazy {
        val systemTrustManager = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null as KeyStore?) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .single()
        PixivEchTlsContext(systemTrustManager)
    }

    val trustManager: X509TrustManager
        get() = context.trustManager

    val socketFactory: SSLSocketFactory
        get() = context.socketFactory

    fun configure(socket: SSLSocket, config: ByteArray) {
        Conscrypt.setEchConfigList(socket, config)
    }
}

internal class PixivEchTlsContext(systemTrustManager: X509TrustManager) {
    private val provider = Conscrypt.newProviderBuilder()
        .provideTrustManager(true)
        .build()

    val trustManager: X509TrustManager
    val socketFactory: SSLSocketFactory

    init {
        // 继承系统信任根（包含 Android 调试配置），使用兼容 Conscrypt 的 PKIX 验证器。
        val roots = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            systemTrustManager.acceptedIssuers.forEachIndexed { index, certificate ->
                setCertificateEntry("system-$index", certificate)
            }
        }
        val delegate = TrustManagerFactory.getInstance("PKIX", provider)
            .apply { init(roots) }
            .trustManagers
            .filterIsInstance<X509TrustManager>()
            .single()
        trustManager = PixivEchTrustManager(delegate)
        socketFactory = SSLContext.getInstance("TLS", provider)
            .apply { init(null, arrayOf(trustManager), null) }
            .socketFactory
    }
}

// 不能继承 X509ExtendedTrustManager：Conscrypt 会再次包装它，导致反射无法获取 ECH 策略。
internal class PixivEchTrustManager(
    private val delegate: X509TrustManager,
) : X509TrustManager {
    // Conscrypt 通过反射调用此公开方法，名称和可见性需要在混淆时保留。
    fun getNetworkSecurityPolicy(): NetworkSecurityPolicy = policy

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        delegate.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        delegate.checkServerTrusted(chain, authType)
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers

    private companion object {
        val policy = object : ConscryptNetworkSecurityPolicy() {
            override fun getDomainEncryptionMode(hostname: String?): DomainEncryptionMode =
                when (hostname?.lowercase()) {
                    API_HOST, AUTH_HOST -> DomainEncryptionMode.REQUIRED
                    else -> DomainEncryptionMode.DISABLED
                }
        }
    }
}
