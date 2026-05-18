package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.search.SearchNovelQuery
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalNovel
import com.mrl.pixiv.common.repository.util.queryParams

class SearchNovelPagingSource(
    private val query: SearchNovelQuery,
    private val isPremium: Boolean,
    private val isIdSearch: Boolean
) : PagingSource<SearchNovelQuery, Novel>() {
    override fun getRefreshKey(state: PagingState<SearchNovelQuery, Novel>): SearchNovelQuery? {
        return null
    }

    override suspend fun load(params: LoadParams<SearchNovelQuery>): LoadResult<SearchNovelQuery, Novel> {
        return try {
            if (isIdSearch) {
                val novelId = query.word.toLongOrNull() ?: return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
                val resp = PixivRepository.getNovelDetail(novelId)
                return LoadResult.Page(
                    data = listOf(resp.novel).filterBlockedTags(),
                    prevKey = null,
                    nextKey = null
                )
            }
            val resp = if (params.key == null) {
                if (query.sort == SearchSort.POPULAR_DESC && !isPremium) {
                    PixivRepository.searchPopularPreviewNovel(query)
                } else {
                    PixivRepository.searchNovel(query)
                }
            } else {
                PixivRepository.searchNovelNext(params.key!!.toMap())
            }
            val query = resp.nextUrl?.queryParams
            val novels = if (requireUserPreferenceValue.isR18Enabled) {
                resp.novels.distinctBy { it.id }
            } else {
                resp.novels.distinctBy { it.id }.filterNormalNovel()
            }.filterBlockedTags()
            if (query != null) {
                val nextKey = SearchNovelQuery(
                    word = query["word"] ?: "",
                    searchTarget = query["search_target"]
                        ?.let { SearchTarget.valueOf(it.uppercase()) }
                        ?: SearchTarget.PARTIAL_MATCH_FOR_TAGS,
                    sort = query["sort"]?.let { SearchSort.valueOf(it.uppercase()) }
                        ?: SearchSort.POPULAR_DESC,
                    offset = query["offset"]?.toInt() ?: 0,
                )
                LoadResult.Page(
                    data = novels,
                    prevKey = params.key,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Page(
                    data = novels,
                    prevKey = params.key,
                    nextKey = null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
