package com.mrl.pixiv.common.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PublicNovelClientTest {
    @Test
    fun publicReadsSendNoCredentialsAndPreserveRepeatedPagingParameters() = runTest {
        val requests = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            assertEquals("www.pixiv.net", request.url.host)
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[HttpHeaders.Cookie])
            requests += request.url.encodedPath
            val body = when (request.url.encodedPath) {
                "/ajax/novel/12/recommend/init" -> {
                    assertEquals("30", request.url.parameters["limit"])
                    """{"novels":[{"id":"21","title":"Related"}],"nextIds":["22","23"],"details":{}}"""
                }
                "/ajax/novel/recommend/novels" -> {
                    assertEquals(listOf("22", "23"), request.url.parameters.getAll("novelIds[]"))
                    """{"novels":[{"id":"22"},{"id":"23"}]}"""
                }
                else -> """{"pollData":{"question":"Question","choices":[{"id":1,"text":"Choice","count":2}],"total":2,"selectedValue":null},"content":"not used"}"""
            }
            respond("""{"error":false,"message":"","body":$body}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val client = PublicNovelClient(http)
            val first = client.recommendations(12)
            assertEquals(listOf("22", "23"), first.nextIds)
            assertEquals(2, client.recommendationPage(first.nextIds).novels.size)
            val poll = client.poll(12).pollData!!
            assertEquals(2, poll.total)
            assertEquals("Choice", poll.choices.single().text)
            assertNull(poll.selectedValue)
            assertEquals(3, requests.size)
        } finally { http.close() }
    }

    @Test
    fun websiteErrorIsNotTreatedAsNoPollOrEmptyRecommendations() = runTest {
        val http = HttpClient(MockEngine {
            respond("""{"error":true,"message":"Login required","body":[]} """)
        })
        try {
            val error = assertFailsWith<IllegalStateException> { PublicNovelClient(http).poll(12) }
            assertEquals("Login required", error.message)
        } finally { http.close() }
    }

    @Test
    fun httpFailureIsNotParsedAsAnEmptyFeed() = runTest {
        val http = HttpClient(MockEngine { respond("Forbidden", HttpStatusCode.Forbidden) })
        try {
            assertFailsWith<IllegalStateException> { PublicNovelClient(http).recommendations(12) }
        } finally { http.close() }
    }
}
