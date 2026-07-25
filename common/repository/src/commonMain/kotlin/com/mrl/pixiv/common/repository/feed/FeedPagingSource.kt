package com.mrl.pixiv.common.repository.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState

class FeedPagingSource<T : Any>(
    private val source: FeedSource<T>,
) : PagingSource<FeedKey, T>() {
    override fun getRefreshKey(state: PagingState<FeedKey, T>): FeedKey? = null

    override suspend fun load(params: LoadParams<FeedKey>): LoadResult<FeedKey, T> {
        return try {
            val key = params.key
            val pageNumber = when (key) {
                is FeedKey.Offset -> key.value / source.pageSize + 1
                else -> 1
            }
            val page = source.load(
                FeedPageRequest(
                    key = key,
                    page = pageNumber,
                    pageSize = source.pageSize,
                )
            )
            LoadResult.Page(
                data = page.items,
                prevKey = page.prevKey,
                nextKey = page.nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
