package com.mrl.pixiv.common.repository.feed

import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.search.SearchIllustQuery
import com.mrl.pixiv.common.data.search.SearchNovelQuery
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.user.UserPreview
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalIllust
import com.mrl.pixiv.common.repository.util.filterNormalNovel
import com.mrl.pixiv.common.repository.util.queryParams
import kotlin.math.max

private const val DEFAULT_SEARCH_PAGE_SIZE = 20

class SearchIllustFeedSource(
    private val query: SearchIllustQuery,
    private val isPremium: Boolean,
    private val isIdSearch: Boolean,
    override val pageSize: Int = DEFAULT_SEARCH_PAGE_SIZE,
) : FeedSource<Illust> {
    override val capability: FeedCapability
        get() = when {
            isIdSearch -> FeedCapability.SINGLE_PAGE
            query.sort == SearchSort.POPULAR_DESC && !isPremium -> FeedCapability.SINGLE_PAGE
            else -> FeedCapability.OFFSET
        }

    override suspend fun load(request: FeedPageRequest): FeedPage<Illust> {
        if (isIdSearch) {
            val illustId = query.word.toLongOrNull()
            val illusts = if (illustId == null) {
                emptyList()
            } else {
                listOf(PixivRepository.getIllustDetail(illustId, Filter.ANDROID.value).illust)
                    .filterBlockedTags()
            }
            return FeedPage(
                items = illusts,
                page = 1,
                capability = FeedCapability.SINGLE_PAGE,
            )
        }

        val offset = request.offsetValue()
        if (capability == FeedCapability.SINGLE_PAGE && offset > 0) {
            return FeedPage(emptyList(), page = request.page, capability = capability)
        }

        val resp = if (capability == FeedCapability.SINGLE_PAGE) {
            PixivRepository.searchPopularPreviewIllust(query.copy(offset = 0))
        } else {
            PixivRepository.searchIllust(query.copy(offset = offset))
        }
        val illusts = if (requireUserPreferenceValue.isR18Enabled) {
            resp.illusts.distinctBy { it.id }
        } else {
            resp.illusts.distinctBy { it.id }.filterNormalIllust()
        }.filterBlockedTags()

        val nextOffset = resp.nextUrl?.queryParams?.get("offset")?.toIntOrNull()
        return FeedPage(
            items = illusts,
            page = request.page,
            prevKey = if (capability == FeedCapability.OFFSET) request.prevOffsetKey(offset) else null,
            nextKey = if (capability == FeedCapability.OFFSET) nextOffset?.let(FeedKey::Offset) else null,
            capability = capability,
        )
    }
}

class SearchNovelFeedSource(
    private val query: SearchNovelQuery,
    private val isPremium: Boolean,
    private val isIdSearch: Boolean,
    override val pageSize: Int = DEFAULT_SEARCH_PAGE_SIZE,
) : FeedSource<Novel> {
    override val capability: FeedCapability
        get() = when {
            isIdSearch -> FeedCapability.SINGLE_PAGE
            query.sort == SearchSort.POPULAR_DESC && !isPremium -> FeedCapability.SINGLE_PAGE
            else -> FeedCapability.OFFSET
        }

    override suspend fun load(request: FeedPageRequest): FeedPage<Novel> {
        if (isIdSearch) {
            val novelId = query.word.toLongOrNull()
            val novels = if (novelId == null) {
                emptyList()
            } else {
                listOf(PixivRepository.getNovelDetail(novelId).novel).filterBlockedTags()
            }
            return FeedPage(
                items = novels,
                page = 1,
                capability = FeedCapability.SINGLE_PAGE,
            )
        }

        val offset = request.offsetValue()
        if (capability == FeedCapability.SINGLE_PAGE && offset > 0) {
            return FeedPage(emptyList(), page = request.page, capability = capability)
        }

        val resp = if (capability == FeedCapability.SINGLE_PAGE) {
            PixivRepository.searchPopularPreviewNovel(query.copy(offset = 0))
        } else {
            PixivRepository.searchNovel(query.copy(offset = offset))
        }
        val novels = if (requireUserPreferenceValue.isR18Enabled) {
            resp.novels.distinctBy { it.id }
        } else {
            resp.novels.distinctBy { it.id }.filterNormalNovel()
        }.filterBlockedTags()

        val nextOffset = resp.nextUrl?.queryParams?.get("offset")?.toIntOrNull()
        return FeedPage(
            items = novels,
            page = request.page,
            prevKey = if (capability == FeedCapability.OFFSET) request.prevOffsetKey(offset) else null,
            nextKey = if (capability == FeedCapability.OFFSET) nextOffset?.let(FeedKey::Offset) else null,
            capability = capability,
        )
    }
}

class SearchUserFeedSource(
    private val word: String,
    private val isIdSearch: Boolean,
    override val pageSize: Int = DEFAULT_SEARCH_PAGE_SIZE,
) : FeedSource<UserPreview> {
    override val capability: FeedCapability
        get() = if (isIdSearch) FeedCapability.SINGLE_PAGE else FeedCapability.OFFSET

    override suspend fun load(request: FeedPageRequest): FeedPage<UserPreview> {
        if (isIdSearch) {
            val userId = word.toLongOrNull()
            val users = if (userId == null) {
                emptyList()
            } else {
                val resp = PixivRepository.getUserDetail(userId = userId)
                listOf(UserPreview(resp.user, emptyList(), emptyList(), false))
            }
            return FeedPage(
                items = users,
                page = 1,
                capability = FeedCapability.SINGLE_PAGE,
            )
        }

        val offset = request.offsetValue()
        val resp = PixivRepository.searchUser(word = word, offset = offset)
        val nextOffset = resp.nextUrl?.queryParams?.get("offset")?.toIntOrNull()
        return FeedPage(
            items = resp.userPreviews,
            page = request.page,
            prevKey = request.prevOffsetKey(offset),
            nextKey = nextOffset?.let(FeedKey::Offset),
            capability = capability,
        )
    }
}

private fun FeedPageRequest.offsetValue(): Int {
    return (key as? FeedKey.Offset)?.value ?: ((page - 1) * pageSize)
}

private fun FeedPageRequest.prevOffsetKey(offset: Int): FeedKey.Offset? {
    if (page <= 1) return null
    return FeedKey.Offset(max(0, offset - pageSize))
}
