package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Series
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.novel.MarkedNovel
import com.mrl.pixiv.common.data.novel.NovelMarker
import com.mrl.pixiv.common.data.novel.NovelMarkersResp
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NovelMarkerPagingSourceTest {
    @Test
    fun loadsEmptyMarkerList() = runTest {
        val source = NovelMarkerPagingSource(
            loadInitial = { NovelMarkersResp() },
            filterVisible = { it },
        )

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            )
        )
        val page = assertIs<PagingSource.LoadResult.Page<String, MarkedNovel>>(result)

        assertEquals(emptyList(), page.data)
        assertNull(page.prevKey)
        assertNull(page.nextKey)
    }

    @Test
    fun deduplicatesNovelsAndPassesNextCursorQuery() = runTest {
        val first = markedNovel(novelId = 10, page = 2)
        var appendQuery: Map<String, String>? = null
        val source = NovelMarkerPagingSource(
            loadInitial = {
                NovelMarkersResp(
                    markedNovels = listOf(
                        first,
                        first,
                        markedNovel(novelId = 11, page = 4),
                        markedNovel(novelId = 13, page = null),
                    ),
                    nextUrl = "https://app-api.pixiv.net/v2/novel/markers?offset=30",
                )
            },
            loadMore = { query ->
                appendQuery = query
                NovelMarkersResp(
                    markedNovels = listOf(markedNovel(novelId = 12, page = 1)),
                )
            },
            filterVisible = { it },
        )

        val refreshed = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            )
        )
        val firstPage =
            assertIs<PagingSource.LoadResult.Page<String, MarkedNovel>>(refreshed)
        assertEquals(listOf(10L, 11L), firstPage.data.map { it.novel.id })
        assertEquals(listOf(2, 4), firstPage.data.map { it.novelMarker?.page })

        val appended = source.load(
            PagingSource.LoadParams.Append(
                key = requireNotNull(firstPage.nextKey),
                loadSize = 30,
                placeholdersEnabled = false,
            )
        )
        val nextPage =
            assertIs<PagingSource.LoadResult.Page<String, MarkedNovel>>(appended)
        assertEquals(listOf(12L), nextPage.data.map { it.novel.id })
        assertEquals(mapOf("offset" to "30"), appendQuery)
    }

    @Test
    fun returnsLoadErrorWithoutPublishingPartialData() = runTest {
        val failure = IllegalStateException("marker request failed")
        val source = NovelMarkerPagingSource(
            loadInitial = { throw failure },
            filterVisible = { it },
        )

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            )
        )

        assertEquals(
            failure,
            assertIs<PagingSource.LoadResult.Error<String, MarkedNovel>>(result).throwable,
        )
    }

    private fun markedNovel(novelId: Long, page: Int?) = MarkedNovel(
        novel = Novel(
            id = novelId,
            title = "Novel $novelId",
            caption = "",
            restrict = 0,
            xRestrict = XRestrict.Normal,
            isOriginal = true,
            imageUrls = ImageUrls(),
            createDate = "",
            tags = emptyList(),
            pageCount = 1,
            textLength = 100,
            user = User(id = 99, name = "Author"),
            series = Series(),
            isBookmarked = false,
            totalBookmarks = 0,
            totalView = 0,
            visible = true,
            isMuted = false,
            isMypixivOnly = false,
            isXRestricted = false,
            novelAiType = AiType.NotAiGeneratedWork,
        ),
        novelMarker = page?.let(::NovelMarker),
    )
}
