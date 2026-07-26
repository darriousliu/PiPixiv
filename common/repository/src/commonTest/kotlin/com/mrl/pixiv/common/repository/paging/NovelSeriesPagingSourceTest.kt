package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Series
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.novel.NovelSeriesDetail
import com.mrl.pixiv.common.data.novel.NovelSeriesResp
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NovelSeriesPagingSourceTest {

    @Test
    fun publishesDetailDeduplicatesChaptersAndKeepsCursor() = runTest {
        val detail = seriesDetail()
        val firstNovel = novel(id = 10)
        var publishedDetail: NovelSeriesDetail? = null
        var appendQuery: Map<String, String>? = null
        val source = NovelSeriesPagingSource(
            seriesId = detail.id,
            onSeriesDetail = { publishedDetail = it },
            loadInitial = {
                response(
                    detail = detail,
                    novels = listOf(firstNovel, firstNovel, novel(id = 11)),
                    nextUrl = "https://app-api.pixiv.net/v2/novel/series?series_id=7&offset=30",
                )
            },
            loadMore = { query ->
                appendQuery = query
                response(detail = detail, novels = listOf(novel(id = 12)))
            },
            isR18Enabled = { true },
        )

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        val page = assertIs<PagingSource.LoadResult.Page<String, Novel>>(result)

        assertEquals(detail, publishedDetail)
        assertEquals(listOf(10L, 11L), page.data.map(Novel::id))
        assertNull(page.prevKey)
        assertEquals(
            "https://app-api.pixiv.net/v2/novel/series?series_id=7&offset=30",
            page.nextKey,
        )

        val appended = source.load(
            PagingSource.LoadParams.Append(
                key = page.nextKey!!,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        val appendedPage =
            assertIs<PagingSource.LoadResult.Page<String, Novel>>(appended)
        assertEquals(listOf(12L), appendedPage.data.map(Novel::id))
        assertEquals(
            mapOf("series_id" to "7", "offset" to "30"),
            appendQuery,
        )
    }

    private fun response(
        detail: NovelSeriesDetail,
        novels: List<Novel>,
        nextUrl: String? = null,
    ): NovelSeriesResp {
        val fallback = novels.firstOrNull() ?: novel(id = 1)
        return NovelSeriesResp(
            novelSeriesDetail = detail,
            novelSeriesFirstNovel = fallback,
            novelSeriesLatestNovel = fallback,
            novels = novels,
            nextUrl = nextUrl,
        )
    }

    private fun seriesDetail() = NovelSeriesDetail(
        id = 7,
        title = "Series",
        caption = "Caption",
        isOriginal = true,
        isConcluded = false,
        contentCount = 3,
        totalCharacterCount = 300,
        user = User(id = 99, name = "Author"),
        displayText = "",
        novelAiType = AiType.NotAiGeneratedWork,
        watchlistAdded = false,
    )

    private fun novel(id: Long) = Novel(
        id = id,
        title = "Novel $id",
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
        series = Series(id = 7, title = "Series"),
        isBookmarked = false,
        totalBookmarks = 0,
        totalView = 0,
        visible = true,
        isMuted = false,
        isMypixivOnly = false,
        isXRestricted = false,
        novelAiType = AiType.NotAiGeneratedWork,
    )
}
