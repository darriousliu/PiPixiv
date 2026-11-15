package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.search.SearchNovelQuery
import com.mrl.pixiv.common.repository.feed.FeedSource
import com.mrl.pixiv.common.repository.feed.FeedKey
import com.mrl.pixiv.common.repository.feed.FeedPageRequest
import com.mrl.pixiv.common.repository.feed.SearchNovelFeedSource
import kotlinx.coroutines.CancellationException

class SearchNovelPagingSource(
    private val query: SearchNovelQuery,
    isPremium: Boolean,
    isIdSearch: Boolean,
    // 手动翻页和无限滚动共用筛选逻辑，翻页时保留当前搜索的全部条件。
    private val source: FeedSource<Novel> = SearchNovelFeedSource(query, isPremium, isIdSearch),
) : PagingSource<SearchNovelQuery, Novel>() {
    override fun getRefreshKey(state: PagingState<SearchNovelQuery, Novel>): SearchNovelQuery? = null

    override suspend fun load(params: LoadParams<SearchNovelQuery>): LoadResult<SearchNovelQuery, Novel> = try {
        var key = params.key?.let { FeedKey.Offset(it.offset) }
        var page = source.load(FeedPageRequest(key = key))
        // 无限滚动不能停在被全部过滤的页面；继续请求到有结果或服务端没有下一页。
        while (page.items.isEmpty()) {
            val next = page.nextKey as? FeedKey.Offset ?: break
            if (next.value <= (key?.value ?: 0)) break
            key = next
            page = source.load(FeedPageRequest(key = key))
        }
        LoadResult.Page(
            data = page.items,
            prevKey = null,
            nextKey = (page.nextKey as? FeedKey.Offset)
                ?.takeIf { it.value > (key?.value ?: 0) }
                ?.let { query.copy(offset = it.value) },
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LoadResult.Error(error)
    }
}
