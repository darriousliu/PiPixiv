package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.novel.PublicNovelInteractionDetail
import com.mrl.pixiv.common.data.novel.PublicNovelRecommendations
import com.mrl.pixiv.common.network.configureNetworkProxy
import com.mrl.pixiv.common.network.httpEngineFactory
import com.mrl.pixiv.common.serialize.JSON
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/** Public website reads never use the shared App API client, bearer token or cookie store. */
internal class PublicNovelClient(private val httpClient: HttpClient) {
    suspend fun recommendations(novelId: Long): PublicNovelRecommendations {
        require(novelId > 0)
        return JSON.decodeFromJsonElement(read("/ajax/novel/$novelId/recommend/init", listOf("limit" to "30")))
    }

    suspend fun recommendationPage(ids: List<String>): PublicNovelRecommendations {
        require(ids.isNotEmpty() && ids.all { (it.toLongOrNull() ?: 0) > 0 })
        return JSON.decodeFromJsonElement(read("/ajax/novel/recommend/novels", ids.map { "novelIds[]" to it }))
    }

    suspend fun poll(novelId: Long): PublicNovelInteractionDetail {
        require(novelId > 0)
        return JSON.decodeFromJsonElement(read("/ajax/novel/$novelId", emptyList()))
    }

    private suspend fun read(path: String, parameters: List<Pair<String, String>>): JsonElement {
        val response = httpClient.get("https://www.pixiv.net$path") {
            header(HttpHeaders.Accept, "application/json")
            header("Referer", "https://www.pixiv.net/")
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (compatible; PiPixiv/1.0)")
            parameters.forEach { (name, value) -> parameter(name, value) }
        }
        check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
        val envelope = JSON.parseToJsonElement(response.bodyAsText()) as? JsonObject
            ?: error("Invalid public novel response")
        check(envelope["error"]?.jsonPrimitive?.booleanOrNull == false) {
            envelope["message"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "Public novel data unavailable" }
        }
        return envelope["body"] as? JsonObject ?: error("Public novel data unavailable")
    }
}

internal fun createPublicNovelHttpClient(): HttpClient = HttpClient(httpEngineFactory) {
    // No HttpSend authentication interceptor or HttpCookies plugin is installed here.
    configureNetworkProxy()
    followRedirects = false
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
