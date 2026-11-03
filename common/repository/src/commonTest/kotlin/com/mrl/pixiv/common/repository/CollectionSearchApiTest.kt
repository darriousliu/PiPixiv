package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionWorkType
import com.mrl.pixiv.common.data.search.IllustAdvancedFilter
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.data.search.SearchNumberRange
import com.mrl.pixiv.common.datasource.remote.createCollectionSearchApi
import com.mrl.pixiv.common.datasource.remote.createPixivApi
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionSearchApiTest {
    @Test
    fun collectionQueryUsesFixedOperationAndPreservesEncodedTagsAcrossPages() = runTest {
        val query = CollectionSearchQuery(restrict = Restrict.PRIVATE, bookmarkTag = "猫 & 犬", workTag = "C++")
        var requests = 0
        withApi(MockEngine { request ->
            requests++
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/v1/search/bookmark/illust", request.url.encodedPath)
            assertEquals("private", request.url.parameters["bookmark_restrict"])
            assertEquals("猫 & 犬", request.url.parameters["bookmark_tag"])
            assertEquals("C++", request.url.parameters["illust_tag"])
            assertEquals(if (requests == 1) null else "30", request.url.parameters["offset"])
            assertNull(request.url.parameters["word"])
            assertNull(request.url.parameters["user_id"])
            assertNull(request.url.parameters["bookmark_period"])
            respond("""{"illusts":[],"total":42,"next_url":null}""", headers = jsonHeaders)
        }) { ktorfit ->
            val api = ktorfit.createCollectionSearchApi()
            assertEquals(42L, api.illusts(collectionPageParameters(query, null)).total)
            api.illusts(collectionPageParameters(query,
                "https://app-api.pixiv.net/v1/search/bookmark/illust?offset=30&bookmark_restrict=public"))
            assertEquals(2, requests)
        }
    }

    @Test
    fun nextUrlRejectsAnotherAccountOperationOrForeignHost() {
        listOf(
            "https://evil.example/v1/search/bookmark/illust?offset=30",
            "https://app-api.pixiv.net/v1/search/bookmark/novel?offset=30",
            "http://app-api.pixiv.net/v1/search/bookmark/illust?offset=30",
            "https://somebody@app-api.pixiv.net/v1/search/bookmark/illust?offset=30",
        ).forEach { next ->
            assertFailsWith<IllegalArgumentException> { collectionPageParameters(CollectionSearchQuery(), next) }
        }
        assertEquals("v1/search/bookmark/novel/novel-tag", collectionTagPath(CollectionWorkType.NOVEL, false))
        assertEquals(mapOf("cursor" to "a+b"), collectionNextParameters(
            "https://app-api.pixiv.net/v1/search/bookmark/illust?cursor=a%2Bb", "v1/search/bookmark/illust"))
    }

    @Test
    fun tagCandidateUsesWordAndDynamicOptionsUsePluralAiParameter() = runTest {
        withApi(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/search/bookmark/novel/novel-tag" -> {
                    assertEquals("科 幻", request.url.parameters["word"])
                    assertNull(request.url.parameters["novel_tag"])
                    respond("""{"tags":[{"name":"科幻","count":12}],"next_url":null}""", headers = jsonHeaders)
                }
                "/v1/search/options" -> {
                    assertEquals("1", request.url.parameters["search_ai_types"])
                    assertNull(request.url.parameters["search_ai_type"])
                    respond("""{"illust":{"lang":{"options":[{"code":"ja","name":"日本語"}]},"tool":{"options":["Photoshop"]}},"novel":{"lang":{"options":[{"code":"en","name":"English"}]},"genre":{"options":[{"id":3,"label":"Fantasy"}]},"word_count_supported_languages":"English"}}""", headers = jsonHeaders)
                }
                else -> error("Unexpected operation")
            }
        }) { ktorfit ->
            val tags = ktorfit.createCollectionSearchApi().novelWorkTags(
                CollectionSearchQuery(type = CollectionWorkType.NOVEL).tagParameters("科 幻"))
            assertEquals(12L, tags.tags.single().count)
            val options = ktorfit.createPixivApi().getSearchOptions("cat", "partial_match_for_tags", 1)
            assertEquals("Photoshop", options.illust.tool.options.single())
            assertEquals("en", options.novel.lang.options.single().code)
            assertEquals(3, options.novel.genre.options.single().id)
        }
    }

    @Test
    fun generalSearchQueryMapOmitsNullBoundsAndEncodesSelectedTool() = runTest {
        withApi(MockEngine { request ->
            assertEquals("/v1/search/illust", request.url.encodedPath)
            assertEquals("900", request.url.parameters["width_min"])
            assertEquals("CLIP STUDIO PAINT", request.url.parameters["tool"])
            assertEquals("猫 & 犬", request.url.parameters["word"])
            assertNull(request.url.parameters["width_max"])
            assertNull(request.url.parameters["height_min"])
            assertTrue(request.url.parameters.names().none { request.url.parameters[it] == "null" })
            respond("""{"illusts":[],"search_span_limit":0,"next_url":null}""", headers = jsonHeaders)
        }) { ktorfit ->
            ktorfit.createPixivApi().searchIllust(SearchIllustQuery(word = "猫 & 犬",
                advanced = IllustAdvancedFilter(width = SearchNumberRange(900), tool = "CLIP STUDIO PAINT")).toMap())
        }
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private suspend fun withApi(engine: MockEngine, block: suspend (Ktorfit) -> Unit) {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            block(Ktorfit.Builder().baseUrl("https://app-api.pixiv.net/").httpClient(client).build())
        } finally {
            client.close()
            engine.close()
        }
    }
}
