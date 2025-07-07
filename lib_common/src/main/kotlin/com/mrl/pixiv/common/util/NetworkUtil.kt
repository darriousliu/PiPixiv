package com.mrl.pixiv.common.util

import androidx.compose.ui.text.intl.Locale
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.datasource.local.mmkv.AuthManager
import com.mrl.pixiv.common.repository.SettingRepository
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.encodedPath
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate.Formats.ISO
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import okio.ByteString.Companion.toByteString
import org.koin.core.component.KoinComponent
import javax.net.ssl.HostnameVerifier
import kotlin.time.Clock

internal const val TAG = "HttpManager"
internal const val HashSalt =
    "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
internal const val API_HOST = "app-api.pixiv.net"
internal const val IMAGE_HOST = "i.pximg.net"
internal const val STATIC_IMAGE_HOST = "s.pximg.net"
internal const val AUTH_HOST = "oauth.secure.pixiv.net"
internal val hostMap: Map<String, String> = mapOf(
    API_HOST to "210.140.139.155",
    AUTH_HOST to "210.140.139.155",
    IMAGE_HOST to "210.140.92.144",
    STATIC_IMAGE_HOST to "210.140.92.143",
    "doh" to "doh.dns.sb",
)

internal val iso8601DateTimeFormat = LocalDateTime.Format {
    date(ISO)
    alternativeParsing({
        char('t')
    }) {
        char('T')
    }
    hour()
    char(':')
    minute()
    char(':')
    second()
}

internal fun encode(text: String): String {
    try {
        val md5 = text.toByteArray().toByteString().md5()
        return md5.hex()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

suspend fun addAuthHeader(request: HttpRequestBuilder) {
    val local = Locale.current
    val instantNow = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val isoDate = "${instantNow.toLocalDateTime(timeZone).format(iso8601DateTimeFormat)}${
        instantNow.offsetIn(timeZone)
    }"
    request.headers.apply {
        remove("User-Agent")
        set(
            "User-Agent",
            "PixivAndroidApp/5.0.166 (Android 14; 2210132C)"
        )
        if (!request.url.encodedPath.contains("/auth/token")) {
            set("Authorization", "Bearer ${AuthManager.requireUserAccessToken()}")
        }
        set("Accept-Language", "${local.language}_${local.region}")
        set("App-OS", "Android")
        set("App-OS-Version", "14")
        set("App-Version", "5.0.166")
        set("X-Client-Time", isoDate)
        set("X-Client-Hash", encode("$isoDate$HashSalt"))
//            set("Host", request.host)
    }

}

object NetworkUtil : KoinComponent {
    private val allSetting: UserPreference = SettingRepository.userPreferenceFlow.value
    val enableBypassSniffing: Boolean = allSetting.enableBypassSniffing
    val imageHost: String = allSetting.imageHost.ifEmpty { IMAGE_HOST }
    val hostnameVerifier = HostnameVerifier { hostname, session ->
        // 检查主机名是否是你期望连接的IP地址或域名
        hostname in hostMap.keys || hostname in hostMap.values || hostname == imageHost || hostname == "doh.dns.sb"
    }
}
