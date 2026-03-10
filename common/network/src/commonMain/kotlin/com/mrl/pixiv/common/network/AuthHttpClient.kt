package com.mrl.pixiv.common.network

import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import org.koin.core.annotation.Single

@Single
@AuthClient
fun authHttpClient() = baseHttpClient.apply {
    plugin(HttpSend).intercept { request ->
        NetworkUtil.addAuthHeader(request)
        request.apply {
            headers.remove("Authorization")
        }
        execute(request)
    }
    config {
        defaultRequest {
            accept(ContentType.Application.Json)
        }
    }
}