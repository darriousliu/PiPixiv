package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import com.mrl.pixiv.common.data.discovery.CommunityUserPreview
import com.mrl.pixiv.common.data.discovery.CommunityUsersKind
import com.mrl.pixiv.common.data.discovery.CommunityUsersResponse
import com.mrl.pixiv.common.data.notification.NotificationsResponse
import com.mrl.pixiv.common.data.notification.PixivNotification
import com.mrl.pixiv.common.datasource.remote.CommunityApi
import com.mrl.pixiv.common.datasource.remote.createCommunityApi
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommunityPagingSourceTest {
    @Test
    fun notificationGroupKeepsIdAndPaginationQueryWithLargeIds() = runTest {
        val groupId = 9007199254740993L
        val cursor = "https://app-api.pixiv.net/v1/notification/view-more?notification_id=$groupId&offset=30&limit=30"
        var requests = 0
        withApi(MockEngine { request ->
            requests++
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/v1/notification/view-more", request.url.encodedPath)
            assertEquals(groupId.toString(), request.url.parameters["notification_id"])
            assertEquals(if (requests == 1) null else "30", request.url.parameters["offset"])
            respond(
                if (requests == 1) """{"notifications":[{"id":1},{"id":1},{"id":2}],"next_url":"$cursor"}"""
                else """{"notifications":[],"next_url":""}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { api ->
            val source = NotificationsPagingSource(
                notificationId = groupId,
                loadInitial = { api.getNotificationGroup(groupId) },
                loadMore = api::getNotificationGroupNext,
            )
            val initial = assertIs<PagingSource.LoadResult.Page<String, PixivNotification>>(source.load(refresh()))
            assertEquals(listOf(1L, 2L), initial.data.map { it.id })
            assertNull(initial.prevKey)
            val next = assertIs<PagingSource.LoadResult.Page<String, PixivNotification>>(
                source.load(PagingSource.LoadParams.Append(requireNotNull(initial.nextKey), 30, false)),
            )
            assertTrue(next.data.isEmpty())
            assertNull(next.nextKey)
            assertEquals(2, requests)
        }
    }

    @Test
    fun followerRequestsDoNotInventAnArbitraryUserId() = runTest {
        val cursor = "https://app-api.pixiv.net/v1/user/follower?offset=30"
        var requests = 0
        withApi(MockEngine { request ->
            requests++
            assertEquals("/v1/user/follower", request.url.encodedPath)
            assertNull(request.url.parameters["user_id"])
            assertEquals(if (requests == 1) null else "30", request.url.parameters["offset"])
            respond(
                if (requests == 1) """{"user_previews":[{"user":{"id":4}},{"user":{"id":4}}],"next_url":"$cursor"}"""
                else """{"user_previews":[]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { api ->
            val source = CommunityUsersPagingSource(
                kind = CommunityUsersKind.FOLLOWERS,
                loadInitial = api::getMyFollowers,
                loadMore = api::getMyFollowersNext,
            )
            val first = assertIs<PagingSource.LoadResult.Page<String, CommunityUserPreview>>(source.load(refresh()))
            assertEquals(listOf(4L), first.data.map { it.user.id })
            val next = assertIs<PagingSource.LoadResult.Page<String, CommunityUserPreview>>(
                source.load(PagingSource.LoadParams.Append(requireNotNull(first.nextKey), 30, false)),
            )
            assertNull(next.nextKey)
        }
    }

    @Test
    fun relatedAuthorsSendSeedAndDoNotInventPagination() = runTest {
        withApi(MockEngine { request ->
            assertEquals("/v1/user/related", request.url.encodedPath)
            assertEquals("42", request.url.parameters["seed_user_id"])
            assertEquals("for_android", request.url.parameters["filter"])
            respond("""{"user_previews":[],"next_url":"https://app-api.pixiv.net/unexpected"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { api ->
            val source = CommunityUsersPagingSource(
                CommunityUsersKind.RELATED, 42,
                loadInitial = { api.getRelatedUsers(42) },
            )
            val page = assertIs<PagingSource.LoadResult.Page<String, CommunityUserPreview>>(source.load(refresh()))
            assertNull(page.nextKey)
        }
    }

    @Test
    fun foreignPaginationUrlFailsBeforeAnyRequest() = runTest {
        var requested = false
        val source = NotificationsPagingSource(loadMore = {
            requested = true
            NotificationsResponse()
        })
        assertIs<PagingSource.LoadResult.Error<String, PixivNotification>>(
            source.load(PagingSource.LoadParams.Append("https://evil.example/v1/notification/list", 30, false)),
        )
        assertTrue(!requested)
    }

    @Test
    fun errorsCanRetryAndBothSourcesPropagateCancellation() = runTest {
        var fail = true
        val source = NotificationsPagingSource(loadInitial = {
            if (fail) error("Offline")
            NotificationsResponse()
        })
        assertIs<PagingSource.LoadResult.Error<String, PixivNotification>>(source.load(refresh()))
        fail = false
        assertIs<PagingSource.LoadResult.Page<String, PixivNotification>>(source.load(refresh()))
        assertFailsWith<CancellationException> {
            NotificationsPagingSource(loadInitial = { throw CancellationException() }).load(refresh())
        }
        assertFailsWith<CancellationException> {
            CommunityUsersPagingSource(CommunityUsersKind.FOLLOWERS,
                loadInitial = { throw CancellationException() }).load(refresh())
        }
    }

    @Test
    fun repeatedNextCursorStopsAppendLoop() = runTest {
        val cursor = "https://app-api.pixiv.net/v1/user/recommended?offset=30"
        val source = CommunityUsersPagingSource(
            CommunityUsersKind.RECOMMENDED,
            loadMore = { CommunityUsersResponse(nextUrl = cursor) },
        )
        val result = assertIs<PagingSource.LoadResult.Page<String, CommunityUserPreview>>(
            source.load(PagingSource.LoadParams.Append(cursor, 30, false)),
        )
        assertNull(result.nextKey)
    }

    private fun refresh() = PagingSource.LoadParams.Refresh<String>(null, 30, false)

    private suspend fun withApi(engine: MockEngine, block: suspend (CommunityApi) -> Unit) {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            block(Ktorfit.Builder().baseUrl("https://app-api.pixiv.net/").httpClient(client).build().createCommunityApi())
        } finally {
            client.close()
            engine.close()
        }
    }
}
