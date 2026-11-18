package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import org.koin.core.annotation.Single

@Single
@ImageClient
fun imageHttpClient() = routedPixivClient(::createImageClient)

private fun createImageClient(setting: BypassSetting) = baseImageHttpClient(setting)
    .applyPixivImageRequests {
        (currentNetworkPreference() ?: NetworkUtil.networkSettings()).imageHost
    }

internal fun HttpClient.applyPixivImageRequests(imageHost: suspend () -> String) = apply {
    plugin(HttpSend).intercept { request ->
        request.url.applyImageSource(imageHost())
        request.headers["Referer"] = Constants.PIXIV_API_BASE_URL
        execute(request)
    }
}

fun resolveImageUrl(url: String, imageHost: String): String =
    URLBuilder().takeFrom(url).applyImageSource(imageHost).buildString()

private fun URLBuilder.applyImageSource(imageHost: String): URLBuilder = apply {
    if (host == IMAGE_HOST && imageHost.isNotEmpty()) host = imageHost
    protocol = URLProtocol.HTTPS
}
