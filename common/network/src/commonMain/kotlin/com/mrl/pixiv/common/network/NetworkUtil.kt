package com.mrl.pixiv.common.network

import androidx.compose.ui.text.intl.Locale
import com.mrl.pixiv.common.data.Constants.HashSalt
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.util.DeviceInfo
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.encodedPath
import io.ktor.utils.io.core.toByteArray
import kotlinx.datetime.LocalDate.Formats.ISO
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetIn
import kotlinx.datetime.toLocalDateTime
import okio.ByteString.Companion.toByteString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

interface NetworkFeature {
    fun provideUserPreference(): UserPreference

    suspend fun provideUserAccessToken(): String
}

internal object NetworkUtil : KoinComponent {
    private val networkFeature by inject<NetworkFeature>()
    val imageHost: String
        get() = networkFeature.provideUserPreference().imageHost.ifEmpty { IMAGE_HOST }

    private val iso8601DateTimeFormat = LocalDateTime.Format {
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

    private fun encode(text: String): String {
        try {
            val md5 = text.toByteArray().toByteString().md5()
            return md5.hex()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    suspend fun addAuthHeader(request: HttpRequestBuilder) {
        val locale = Locale.current
        val instantNow = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val isoDate = "${instantNow.toLocalDateTime(timeZone).format(iso8601DateTimeFormat)}${
            instantNow.offsetIn(timeZone)
        }"

        val appAcceptLanguage = locale.language.lowercase().let { lang ->
            when (lang) {
                "zh" -> {
                    val country = locale.region.lowercase()
                    if (country == "cn") "zh-hans" else "zh-hant"
                }

                "ja", "ko", "es" -> lang // hashCode 3241, 3383, 3428 可能对应这些语言
                else -> "en"
            }
        }
        request.headers.apply {
            remove("User-Agent")
            set(
                "User-Agent",
                "Pixiv${DeviceInfo.PLATFORM.replaceFirstChar { it.uppercase() }}App/${DeviceInfo.APP_VERSION} (${DeviceInfo.PLATFORM} ${DeviceInfo.VERSION}; ${DeviceInfo.MODEL})"
            )
            if (!request.url.encodedPath.contains("/auth/token")) {
                set("Authorization", "Bearer ${requireUserAccessToken()}")
            }
            // zh_CN
            set("Accept-Language", locale.toLanguageTag().replace("-", "_"))
            // zh-hans
            set("App-Accept-Language", appAcceptLanguage)
            set("App-OS", DeviceInfo.PLATFORM.lowercase())
            set("App-OS-Version", DeviceInfo.VERSION)
            set("App-Version", DeviceInfo.APP_VERSION)
            set("X-Client-Time", isoDate)
            set("X-Client-Hash", encode("$isoDate$HashSalt"))
//            set("Host", request.host)
        }

    }

    suspend fun requireUserAccessToken() = networkFeature.provideUserAccessToken()
}
