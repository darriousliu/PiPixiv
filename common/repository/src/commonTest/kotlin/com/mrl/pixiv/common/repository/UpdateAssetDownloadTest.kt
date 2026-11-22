package com.mrl.pixiv.common.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateAssetDownloadTest {
    @Test
    fun followsHttpsReleaseRedirectAndWritesTheFullResponse() = runTest {
        val requests = mutableListOf<String>()
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    requests += request.url.host
                    when (request.url.host) {
                        "github.com" -> respond(
                            content = "",
                            status = HttpStatusCode.Found,
                            headers = headersOf(HttpHeaders.Location, "https://release-assets.example/update.apk"),
                        )
                        "release-assets.example" -> respond(
                            content = "update",
                            headers = headersOf(HttpHeaders.ContentLength, "6"),
                        )
                        else -> error("Unexpected release host")
                    }
                }
            }
        }.use { client ->
            val downloaded = Buffer()
            streamUpdateAsset(client, "https://github.com/example/releases/download/update.apk") { downloaded }
            assertEquals("update", downloaded.readString())
        }
        assertEquals(listOf("github.com", "release-assets.example"), requests)
    }

    @Test
    fun rejectsAnHttpRedirectBeforeOpeningTheFile() = runTest {
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    assertEquals("github.com", request.url.host)
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, "http://release-assets.example/update.apk"),
                    )
                }
            }
        }.use { client ->
            assertFailsWith<IllegalStateException> {
                streamUpdateAsset(client, "https://github.com/example/releases/download/update.apk") {
                    throw AssertionError("Redirect response must not be written to disk")
                }
            }
        }
    }
}
