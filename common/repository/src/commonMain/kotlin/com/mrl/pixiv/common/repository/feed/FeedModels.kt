package com.mrl.pixiv.common.repository.feed

enum class FeedCapability {
    OFFSET,
    CURSOR,
    SINGLE_PAGE,
}

sealed interface FeedKey {
    data class Offset(val value: Int) : FeedKey
    data class Cursor(val value: String) : FeedKey
}

data class FeedPageRequest(
    val key: FeedKey? = null,
    val page: Int = 1,
    val pageSize: Int,
)

data class FeedPage<T : Any>(
    val items: List<T>,
    val page: Int,
    val prevKey: FeedKey? = null,
    val nextKey: FeedKey? = null,
    val capability: FeedCapability,
) {
    val hasNextPage: Boolean
        get() = nextKey != null

    val canJumpToPage: Boolean
        get() = capability == FeedCapability.OFFSET
}

data class PagedFeedState<T : Any>(
    val items: List<T> = emptyList(),
    val currentPage: Int = 1,
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val hasNextPage: Boolean = false,
    val canJumpToPage: Boolean = false,
    val capability: FeedCapability = FeedCapability.SINGLE_PAGE,
)

interface FeedSource<T : Any> {
    val pageSize: Int
    val capability: FeedCapability

    suspend fun load(request: FeedPageRequest): FeedPage<T>
}
