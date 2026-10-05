package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.novel.PublicNovelPreview
import com.mrl.pixiv.common.data.novel.PublicNovelRecommendations
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.ReadingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.hasDisallowedLongNovelTag
import kotlinx.coroutines.CancellationException

class PublicNovelRecommendationsPagingSource(
    private val novelId: Long,
    private val initial: suspend (Long) -> PublicNovelRecommendations = ReadingRepository::publicNovelRecommendations,
    private val more: suspend (List<String>) -> PublicNovelRecommendations = ReadingRepository::publicNovelRecommendationPage,
    private val filter: (List<PublicNovelPreview>) -> List<PublicNovelPreview> = { novels ->
        novels.filter { novel ->
            novel.navigableId != null &&
                (novel.xRestrict == 0 || requireUserPreferenceValue.isR18Enabled) &&
                !novel.tags.asSequence().hasDisallowedLongNovelTag(requireUserPreferenceValue.browsingSettings) &&
                novel.tags.none { BlockingRepositoryV2.isTagBlocked(it, allowKeywordMatch = true) }
        }
    },
) : PagingSource<List<String>, PublicNovelPreview>() {
    init { invalidateOnNovelFilterSettingsChanges() }

    override suspend fun load(params: LoadParams<List<String>>): LoadResult<List<String>, PublicNovelPreview> = try {
        val remaining = params.key
        val response: PublicNovelRecommendations
        val next: List<String>
        if (remaining == null) {
            response = initial(novelId)
            val seenIds = response.novels.mapTo(mutableSetOf()) { it.id }
            next = response.nextIds.distinct().filter { (it.toLongOrNull() ?: 0) > 0 && it !in seenIds }
        } else {
            // Public website pagination provides ordered IDs, not a next_url.
            val batch = remaining.take(30)
            response = more(batch)
            next = remaining.drop(batch.size)
        }
        LoadResult.Page(filter(response.novels.distinctBy { it.id }), null, next.takeIf { it.isNotEmpty() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<List<String>, PublicNovelPreview>): List<String>? = null
}
