package com.mrl.pixiv.common.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
@CookieAuthClient
internal fun cookieAuthHttpClient(
    json: Json,
    cookieStorage: AcceptAllCookiesStorage,
    engineConfig: HttpClientConfig<*>.() -> Unit = {}
) = baseHttpClient.apply {
    config {
        install(ContentNegotiation) { json(json) }
        install(HttpCookies) { storage = cookieStorage }
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 60_000
        }
        // 不要让 Ktor 自动跟随 3xx，我们需要手动读取 Location
        followRedirects = false
        expectSuccess = false
        defaultRequest {
            header(HttpHeaders.CacheControl, "no-cache")
        }
        engineConfig()
    }
}