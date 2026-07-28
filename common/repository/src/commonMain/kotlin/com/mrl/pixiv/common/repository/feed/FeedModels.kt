package com.mrl.pixiv.common.repository.feed

enum class FeedCapability {
    OFFSET,
    SINGLE_PAGE,
}

sealed interface FeedKey {
    data class Offset(val value: Int) : FeedKey
}

data class FeedPageRequest(
    val key: FeedKey? = null,
    val page: Int = 1,
)

data class FeedPage<T : Any>(
    val items: List<T>,
    val nextKey: FeedKey? = null,
) {
    val hasNextPage: Boolean
        get() = nextKey != null
}

data class PagedFeedState<T : Any>(
    val items: List<T> = emptyList(),
    val currentPage: Int = 1,
    val requestedPage: Int? = null,
    val error: Throwable? = null,
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false,
    val canJumpToPage: Boolean = false,
    val capability: FeedCapability = FeedCapability.SINGLE_PAGE,
    val scrollToTopEventId: Long = 0,
) {
    val isLoading: Boolean
        get() = requestedPage != null
}

interface FeedSource<T : Any> {
    val capability: FeedCapability

    suspend fun load(request: FeedPageRequest): FeedPage<T>
}
