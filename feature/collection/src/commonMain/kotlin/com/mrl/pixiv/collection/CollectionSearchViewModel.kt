package com.mrl.pixiv.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.collection.CollectionPeriodOption
import com.mrl.pixiv.common.data.collection.CollectionSearchQuery
import com.mrl.pixiv.common.data.collection.CollectionTagOptions
import com.mrl.pixiv.common.data.collection.isCollectionMonth
import com.mrl.pixiv.common.repository.CollectionSearchRepository
import com.mrl.pixiv.common.repository.isSelf
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.util.filterNormalIllust
import com.mrl.pixiv.common.repository.util.filterNormalNovel
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

enum class CollectionSyncState { CHECKING, SYNCING, READY, FAILED, NOT_OWNER }

data class CollectionSearchState(
    val sync: CollectionSyncState = CollectionSyncState.CHECKING,
    val query: CollectionSearchQuery = CollectionSearchQuery(),
    val draft: CollectionSearchQuery = CollectionSearchQuery(),
    val bookmarkTags: CollectionTagOptions = CollectionTagOptions(),
    val workTags: CollectionTagOptions = CollectionTagOptions(),
    val periods: List<CollectionPeriodOption> = emptyList(),
    val optionsLoading: Boolean = false,
    val optionsFailed: Boolean = false,
)

@KoinViewModel
class CollectionSearchViewModel(private val uid: Long) : ViewModel() {
    private val _state = MutableStateFlow(CollectionSearchState())
    val state = _state.asStateFlow()
    private var syncJob: Job? = null
    private var optionsJob: Job? = null
    private var moreJob: Job? = null

    /** Start a fresh draft from the same query that drives the visible collection list. */
    fun open(query: CollectionSearchQuery) {
        optionsJob?.cancel()
        moreJob?.cancel()
        _state.value = beginCollectionFilterEdit(query, uid.isSelf)
        checkSync()
    }

    fun checkSync() {
        syncJob?.cancel()
        if (!uid.isSelf) {
            _state.update { it.copy(sync = CollectionSyncState.NOT_OWNER) }
            return
        }
        syncJob = viewModelScope.launch {
            _state.update { it.copy(sync = CollectionSyncState.CHECKING) }
            try {
                val ready = CollectionSearchRepository.syncStatus().isSynchronised
                _state.update { it.copy(sync = if (ready) CollectionSyncState.READY else CollectionSyncState.SYNCING) }
                if (ready) loadOptions()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(sync = CollectionSyncState.FAILED) }
            }
        }
    }

    fun updateDraft(value: CollectionSearchQuery) {
        val draft = if (uid.isSelf) value else value.normalizedForOwner(false)
        _state.update { it.copy(draft = draft) }
        loadOptions(debounce = true)
    }

    fun applyDraft(): CollectionSearchQuery? {
        if (!canApplyCollectionDraft(state.value)) return null
        _state.update { applyCollectionSearchDraft(it) }
        return state.value.query.normalizedForOwner(uid.isSelf)
    }

    fun close() {
        syncJob?.cancel()
        optionsJob?.cancel()
        moreJob?.cancel()
    }

    fun resetDraft() {
        _state.update(::resetCollectionFilterDraft)
        loadOptions(debounce = true)
    }

    fun loadOptions(debounce: Boolean = false) {
        optionsJob?.cancel()
        moreJob?.cancel()
        if (!uid.isSelf || state.value.sync != CollectionSyncState.READY) return
        val query = state.value.draft
        optionsJob = viewModelScope.launch {
            _state.update { it.copy(optionsLoading = true, optionsFailed = false) }
            if (debounce) delay(300)
            try {
                val (bookmarkResult, workResult, periodResult) = coroutineScope {
                    val bookmarks = async { CollectionSearchRepository.tags(query, true) }
                    val works = async { CollectionSearchRepository.tags(query, false) }
                    val periods = async { CollectionSearchRepository.periods(query) }
                    Triple(bookmarks.await(), works.await(), periods.await().periods.filter { isCollectionMonth(it.name) })
                }
                if (state.value.draft == query) _state.update {
                    it.copy(bookmarkTags = bookmarkResult, workTags = workResult, periods = periodResult,
                        optionsLoading = false, optionsFailed = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (state.value.draft == query) _state.update { it.copy(optionsLoading = false, optionsFailed = true) }
            }
        }
    }

    fun moreTags(bookmarkTags: Boolean) {
        if (!uid.isSelf || state.value.optionsLoading) return
        val query = state.value.draft
        val previous = if (bookmarkTags) state.value.bookmarkTags else state.value.workTags
        val next = previous.nextUrl ?: return
        moreJob?.cancel()
        moreJob = viewModelScope.launch {
            _state.update { it.copy(optionsLoading = true, optionsFailed = false) }
            try {
                val page = CollectionSearchRepository.tags(query, bookmarkTags, next)
                val merged = page.copy(tags = (previous.tags + page.tags).distinctBy { it.name },
                    nextUrl = page.nextUrl?.takeIf { it != next })
                if (state.value.draft == query) _state.update {
                    if (bookmarkTags) it.copy(bookmarkTags = merged, optionsLoading = false)
                    else it.copy(workTags = merged, optionsLoading = false)
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _state.update { it.copy(optionsLoading = false, optionsFailed = true) } }
        }
    }
}

internal fun applyCollectionSearchDraft(state: CollectionSearchState): CollectionSearchState =
    if (!canApplyCollectionDraft(state)) state else state.copy(
        query = state.draft.copy(bookmarkTag = state.draft.bookmarkTag.trim(), workTag = state.draft.workTag.trim()),
    )

internal class CollectionSearchIllustPagingSource(private val query: CollectionSearchQuery) : PagingSource<String, Illust>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Illust> = try {
        val response = CollectionSearchRepository.illusts(query, params.key)
        val visible = (if (requireUserPreferenceValue.isR18Enabled) response.illusts
            else response.illusts.filterNormalIllust()).filterBlockedTags().distinctBy { it.id }
        visible.forEach { BookmarkState.updateIllustBookmarkDetail(it.id, true, query.restrict) }
        LoadResult.Page(visible, null, response.nextUrl?.takeIf { it.isNotBlank() && it != params.key })
    } catch (error: CancellationException) { throw error }
    catch (error: Exception) { LoadResult.Error(error) }
    override fun getRefreshKey(state: PagingState<String, Illust>): String? = null
}

internal class CollectionSearchNovelPagingSource(private val query: CollectionSearchQuery) : PagingSource<String, Novel>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Novel> = try {
        val response = CollectionSearchRepository.novels(query, params.key)
        val visible = (if (requireUserPreferenceValue.isR18Enabled) response.novels
            else response.novels.filterNormalNovel()).filterBlockedTags().distinctBy { it.id }
        visible.forEach { BookmarkState.updateNovelBookmarkDetail(it.id, true, query.restrict) }
        LoadResult.Page(visible, null, response.nextUrl?.takeIf { it.isNotBlank() && it != params.key })
    } catch (error: CancellationException) { throw error }
    catch (error: Exception) { LoadResult.Error(error) }
    override fun getRefreshKey(state: PagingState<String, Novel>): String? = null
}
