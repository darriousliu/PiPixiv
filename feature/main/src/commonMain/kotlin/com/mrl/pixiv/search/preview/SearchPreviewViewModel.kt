package com.mrl.pixiv.search.preview

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.search.TrendingTag
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

sealed class SearchPreviewAction : ViewIntent {
    data object LoadTrendingTags : SearchPreviewAction()
    data class AddSearchHistory(
        val keyword: String,
        val mode: AppViewMode,
    ) : SearchPreviewAction()
}

data class SearchPreviewState(
    val refreshing: Boolean = false,
    val failed: Boolean = false,
    val trendingTags: ImmutableList<TrendingTag> = persistentListOf(),
)

@KoinViewModel
class SearchPreviewViewModel : BaseMviViewModel<SearchPreviewState, SearchPreviewAction>(
    initialState = SearchPreviewState(refreshing = true)
) {
    val lazyGridState = LazyGridState()
    private var trendingTagsJob: Job? = null

    override suspend fun handleIntent(intent: SearchPreviewAction) {
        when (intent) {
            is SearchPreviewAction.LoadTrendingTags -> loadTrendingTags()
            is SearchPreviewAction.AddSearchHistory -> addSearchHistory(intent.keyword, intent.mode)
        }
    }

    init {
        dispatch(SearchPreviewAction.LoadTrendingTags)
    }

    private fun addSearchHistory(keyword: String, mode: AppViewMode) {
        when (mode) {
            AppViewMode.ILLUST -> SearchRepository.addSearchHistory(keyword)
            AppViewMode.NOVEL -> SearchRepository.addNovelSearchHistory(keyword)
        }
    }

    fun switchViewMode(mode: AppViewMode) {
        SettingRepository.setAppViewMode(mode)
        loadTrendingTags()
    }

    private fun loadTrendingTags() {
        trendingTagsJob?.cancel()
        val mode = requireUserPreferenceValue.appViewMode
        trendingTagsJob = viewModelScope.launch {
            updateState { copy(refreshing = true, failed = false) }
            val nextState = withContext(Dispatchers.IO) {
                resolveTrendingTags(uiState.value) {
                    when (mode) {
                        AppViewMode.ILLUST -> PixivRepository.trendingTags(Filter.ANDROID)
                        AppViewMode.NOVEL -> PixivRepository.getTrendingNovelTags(Filter.ANDROID)
                    }.trendTags
                }
            }
            updateState { nextState }
        }
    }
}

internal suspend fun resolveTrendingTags(
    previousState: SearchPreviewState,
    load: suspend () -> List<TrendingTag>,
): SearchPreviewState = try {
    previousState.copy(
        trendingTags = load().toImmutableList(),
        refreshing = false,
        failed = false,
    )
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    previousState.copy(refreshing = false, failed = true)
}
