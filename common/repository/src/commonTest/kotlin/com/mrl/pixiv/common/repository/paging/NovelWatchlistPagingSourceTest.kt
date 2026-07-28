package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import com.mrl.pixiv.common.data.novel.NovelWatchlistResp
import com.mrl.pixiv.common.data.novel.NovelWatchlistSeries
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelWatchlistPagingSourceTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun loadsEmptyResponse() = runTest {
        val source = NovelWatchlistPagingSource(
            loadInitial = {
                NovelWatchlistResp(series = emptyList(), nextUrl = null)
            },
        )

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        val page = assertIs<PagingSource.LoadResult.Page<String, NovelWatchlistSeries>>(result)

        assertTrue(page.data.isEmpty())
        assertNull(page.prevKey)
        assertNull(page.nextKey)
    }

    @Test
    fun decodesNormalMaskedAndNullItems() {
        val response = json.decodeFromString<NovelWatchlistResp>(
            """
            {
              "series": [
                {
                  "id": 123,
                  "title": "Series",
                  "url": "https://example.com/cover.jpg",
                  "published_content_count": 8,
                  "latest_content_id": 456,
                  "last_published_content_datetime": "2026-07-27T12:00:00+00:00",
                  "user": {"id": 9, "name": "Author"},
                  "unknown": true
                },
                {
                  "id": 999,
                  "title": "Unavailable series",
                  "url": "https://example.com/placeholder.jpg",
                  "mask_text": "Unavailable",
                  "user": {"id": 9}
                },
                null
              ],
              "next_url": null
            }
            """.trimIndent(),
        )

        val normal = response.series.orEmpty()[0]
        val masked = response.series.orEmpty()[1]
        assertEquals(123L, normal?.id)
        assertEquals(456L, normal?.latestContentId)
        assertFalse(normal?.isMasked ?: true)
        assertTrue(masked?.isMasked == true)
        assertNull(response.series.orEmpty()[2])
    }

    @Test
    fun keepsMaskedPlaceholderAndUsesServerCursor() = runTest {
        val normal = NovelWatchlistSeries(id = 1, title = "Normal")
        val masked = NovelWatchlistSeries(
            id = 0,
            title = "",
            maskText = "Unavailable",
        )
        var appendQuery: Map<String, String>? = null
        val source = NovelWatchlistPagingSource(
            loadInitial = {
                NovelWatchlistResp(
                    series = listOf(normal, null, masked),
                    nextUrl = "https://app-api.pixiv.net/v1/watchlist/novel?offset=30",
                )
            },
            loadMore = { query ->
                appendQuery = query
                NovelWatchlistResp(series = emptyList(), nextUrl = null)
            },
        )

        val first = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        val firstPage = assertIs<PagingSource.LoadResult.Page<String, NovelWatchlistSeries>>(first)
        assertEquals(listOf(normal, masked), firstPage.data)
        assertNull(firstPage.prevKey)
        assertEquals(
            "https://app-api.pixiv.net/v1/watchlist/novel?offset=30",
            firstPage.nextKey,
        )

        val second = source.load(
            PagingSource.LoadParams.Append(
                key = firstPage.nextKey!!,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        assertIs<PagingSource.LoadResult.Page<String, NovelWatchlistSeries>>(second)
        assertEquals(mapOf("offset" to "30"), appendQuery)
    }
}
