package com.mrl.pixiv.novel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.novel.NovelPollData
import com.mrl.pixiv.common.data.novel.PublicNovelPoll
import com.mrl.pixiv.common.repository.ReadingRepository
import com.mrl.pixiv.common.repository.paging.PublicNovelRecommendationsPagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

internal data class NovelPollState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val poll: PublicNovelPoll? = null,
    val choiceId: Int? = null,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
) {
    fun loaded(response: PublicNovelPoll?): NovelPollState {
        val selected = response?.selectedValue?.takeIf { id -> response.choices.any { it.id == id } }
        return copy(
            loading = false,
            loaded = true,
            poll = response,
            choiceId = selected,
            submitted = selected != null,
            error = null,
        )
    }

    fun select(id: Int) = if (!submitting && !submitted && poll?.choices?.any { it.id == id } == true) {
        copy(choiceId = id, error = null)
    } else this

    fun answered(result: NovelPollData): NovelPollState = copy(
        poll = PublicNovelPoll(result.question, result.total, result.choices, result.selectedId),
        choiceId = result.selectedId,
        submitting = false,
        submitted = true,
        error = null,
    )
}

@KoinViewModel
class NovelInteractionsViewModel(val novelId: Long) : ViewModel() {
    private val _poll = MutableStateFlow(NovelPollState())
    internal val poll = _poll.asStateFlow()
    val recommendations = Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
        PublicNovelRecommendationsPagingSource(novelId)
    }.flow.cachedIn(viewModelScope)

    init { loadPoll() }

    fun loadPoll() {
        if (_poll.value.loading || _poll.value.submitting || _poll.value.submitted) return
        _poll.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val response = ReadingRepository.publicNovelPoll(novelId)
                _poll.update { it.loaded(response) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _poll.update { it.copy(loading = false, error = error.message.orEmpty()) }
            }
        }
    }

    fun selectChoice(id: Int) { _poll.update { it.select(id) } }

    fun answerPoll() {
        val current = _poll.value
        val choiceId = current.choiceId ?: return
        if (current.submitting || current.submitted || current.poll?.choices?.none { it.id == choiceId } != false) return
        _poll.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                // This is the only write: the signed-in App API submits after an explicit tap.
                val response = ReadingRepository.answerNovelPoll(novelId, choiceId)
                _poll.update { it.answered(response) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _poll.update { it.copy(error = error.message.orEmpty()) }
            } finally {
                _poll.update { it.copy(submitting = false) }
            }
        }
    }
}
