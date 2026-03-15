package com.mrl.pixiv.search

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.android.annotation.KoinViewModel

@Stable
data class SearchState(
    val autoCompleteSearchWords: ImmutableList<Tag> = persistentListOf(),
    val isIdSearch: Boolean = false,
) {
    data class SearchFilter(
        val sort: SearchSort = SearchSort.POPULAR_DESC,
        val searchTarget: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
        val searchAiType: SearchAiType = SearchAiType.HIDE_AI,
    )
}

sealed class SearchAction : ViewIntent {
    data object ClearAutoCompleteSearchWords : SearchAction()

    data class UpdateSearchWords(
        val searchWords: String,
    ) : SearchAction()

    data class UpdateIsIdSearch(
        val isIdSearch: Boolean,
    ) : SearchAction()

    data class SearchAutoComplete(
        val searchWords: String,
        val mergePlainKeywordResults: Boolean = true,
    ) : SearchAction()


    data class AddSearchHistory(
        val searchWords: String,
    ) : SearchAction()

    data class DeleteSearchHistory(
        val searchWords: String,
    ) : SearchAction()
}

@KoinViewModel
class SearchViewModel : BaseMviViewModel<SearchState, SearchAction>(
    initialState = SearchState()
) {
    var searchWords: String = ""
        private set

    val appViewMode: AppViewMode
        get() = SettingRepository.userPreferenceFlow.value.appViewMode

    override suspend fun handleIntent(intent: SearchAction) {
        when (intent) {
            is SearchAction.SearchAutoComplete -> searchAutoComplete(intent)
            is SearchAction.AddSearchHistory -> addSearchHistory(intent)
            is SearchAction.DeleteSearchHistory -> deleteSearchHistory(intent)
            is SearchAction.UpdateSearchWords -> searchWords = intent.searchWords
            is SearchAction.UpdateIsIdSearch -> updateState { copy(isIdSearch = intent.isIdSearch) }
            else -> Unit
        }
    }

    private fun addSearchHistory(action: SearchAction.AddSearchHistory) {
        searchWords = action.searchWords
        when (appViewMode) {
            AppViewMode.ILLUST -> SearchRepository.addSearchHistory(action.searchWords)
            AppViewMode.NOVEL -> SearchRepository.addNovelSearchHistory(action.searchWords)
        }
    }

    private fun deleteSearchHistory(action: SearchAction.DeleteSearchHistory) {
        when (appViewMode) {
            AppViewMode.ILLUST -> SearchRepository.deleteSearchHistory(action.searchWords)
            AppViewMode.NOVEL -> SearchRepository.deleteNovelSearchHistory(action.searchWords)
        }
    }

    private fun searchAutoComplete(action: SearchAction.SearchAutoComplete) {
        launchIO {
            val resp = PixivRepository.searchAutoComplete(word = action.searchWords)
            updateState {
                copy(autoCompleteSearchWords = resp.tags.toImmutableList())
            }
        }
    }

    fun addSearchIdHistory(searchId: String) {
        searchWords = searchId
        when (appViewMode) {
            AppViewMode.ILLUST -> SearchRepository.addSearchIdHistory(searchId)
            AppViewMode.NOVEL -> SearchRepository.addNovelSearchIdHistory(searchId)
        }
    }

    fun deleteSearchIdHistory(searchId: String) {
        when (appViewMode) {
            AppViewMode.ILLUST -> SearchRepository.removeSearchIdHistory(searchId)
            AppViewMode.NOVEL -> SearchRepository.removeNovelSearchIdHistory(searchId)
        }
    }

    fun switchViewMode(mode: AppViewMode) {
        SettingRepository.setAppViewMode(mode)
    }
}
