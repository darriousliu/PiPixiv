package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.util.queryParams

class SearchIllustPagingSource(
    private val query: SearchIllustQuery,
) : PagingSource<SearchIllustQuery, Illust>() {
    override fun getRefreshKey(state: PagingState<SearchIllustQuery, Illust>): SearchIllustQuery? {
        return null
    }

    override suspend fun load(params: LoadParams<SearchIllustQuery>): LoadResult<SearchIllustQuery, Illust> {
        return try {
            val resp = if (params.key == null) {
                PixivRepository.searchIllust(query)
            } else {
                PixivRepository.searchIllustNext(params.key!!.toMap())
            }
            val query = resp.nextUrl?.queryParams
            if (query != null) {
                val nextKey = SearchIllustQuery(
                    word = query["word"] ?: "",
                    searchTarget = query["search_target"]
                        ?.let { SearchTarget.valueOf(it.uppercase()) }
                        ?: SearchTarget.PARTIAL_MATCH_FOR_TAGS,
                    sort = query["sort"]?.let { SearchSort.valueOf(it.uppercase()) }
                        ?: SearchSort.POPULAR_DESC,
                    searchAiType = query["search_ai_type"]
                        ?.let { type -> SearchAiType.entries.find { it.value == type.toInt() } }
                        ?: SearchAiType.HIDE_AI,
                    offset = query["offset"]?.toInt() ?: 0,
                )
                LoadResult.Page(
                    data = resp.illusts.distinctBy { it.id },
                    prevKey = params.key,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Page(
                    data = resp.illusts.distinctBy { it.id },
                    prevKey = params.key,
                    nextKey = null
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}