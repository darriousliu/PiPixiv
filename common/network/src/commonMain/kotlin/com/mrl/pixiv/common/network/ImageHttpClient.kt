package com.mrl.pixiv.common.network

import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import com.mrl.pixiv.common.network.NetworkUtil.imageHost
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.host
import io.ktor.http.URLProtocol
import org.koin.core.annotation.Single

@Single(createdAtStart = true)
@ImageClient
fun imageHttpClient() = baseImageHttpClient.apply {
    plugin(HttpSend).intercept { request ->
        request.apply {
            url {
                host = if (request.host == IMAGE_HOST) imageHost else request.host
                protocol = URLProtocol.HTTPS
            }
            headers["Referer"] = Constants.PIXIV_API_BASE_URL
        }
        execute(request)
    }
}