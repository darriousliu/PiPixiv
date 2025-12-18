package com.mrl.pixiv.comment

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.data.comment.Emoji
import com.mrl.pixiv.common.data.comment.Stamp
import com.mrl.pixiv.common.repository.CommentRepository
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.paging.CommentPagingSource
import com.mrl.pixiv.common.repository.paging.CommentRepliesPagingSource
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.android.annotation.KoinViewModel

internal const val MAX_COMMENT_LENGTH = 140

@Stable
data class CommentState(
    val isSending: Boolean = false,
    val replyTarget: Comment? = null,
    val expandedComment: Comment? = null
)

sealed class CommentSideEffect : SideEffect {
    data class CommentAdded(val commentId: Long, val parentCommentId: Long?) : CommentSideEffect()
    data object CommentDeleted : CommentSideEffect()
}

@KoinViewModel
class CommentViewModel(
    private val id: Long,
    private val type: CommentType
) : BaseMviViewModel<CommentState, ViewIntent>(
    initialState = CommentState()
) {
    val currentInput: TextFieldState = TextFieldState()

    val commentList = Pager(PagingConfig(pageSize = 30)) {
        CommentPagingSource(id)
    }.flow.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val replies = uiState.map { it.expandedComment }
        .distinctUntilChanged()
        .flatMapLatest { comment ->
            if (comment == null) flowOf(PagingData.empty())
            else Pager(PagingConfig(pageSize = 30)) {
                CommentRepliesPagingSource(comment.id)
            }.flow.cachedIn(viewModelScope)
        }

    init {
        launchIO {
            CommentRepository.loadStamps()
            CommentRepository.loadEmojis()
        }
    }

    override suspend fun handleIntent(intent: ViewIntent) {
    }

    fun insertEmoji(emoji: Emoji) {
        val slug = "(${emoji.slug})"
        currentInput.edit {
            if (length - (selection.end - selection.start) + slug.length > MAX_COMMENT_LENGTH) return@edit
            replace(selection.start, selection.end, slug)
        }
    }

    fun setReplyTarget(comment: Comment?) {
        updateState { copy(replyTarget = comment) }
    }

    fun setExpandedComment(comment: Comment?) {
        updateState { copy(expandedComment = comment) }
    }

    fun sendText() {
        launchIO(
            onError = {
                ToastUtil.safeShortToast(RString.comment_failed)
            }
        ) {
            val text = currentInput.text.toString()
            if (text.isBlank() || text.length > MAX_COMMENT_LENGTH) return@launchIO
            updateState { copy(isSending = true) }
            val parentId = uiState.value.replyTarget?.id
            val resp = when (type) {
                CommentType.ILLUST -> PixivRepository.addIllustComment(
                    id,
                    text,
                    parentCommentId = parentId
                )

                CommentType.NOVEL -> PixivRepository.addNovelComment(
                    id,
                    text,
                    parentCommentId = parentId
                )
            }
            currentInput.clearText()
            updateState { copy(isSending = false, replyTarget = null) }
            sendEffect(CommentSideEffect.CommentAdded(resp.comment.id, parentId))
        }
    }

    fun sendStamp(stamp: Stamp) {
        launchIO(
            onError = {
                ToastUtil.safeShortToast(RString.comment_failed)
            }
        ) {
            updateState { copy(isSending = true) }
            val parentId = uiState.value.replyTarget?.id
            val resp = when (type) {
                CommentType.ILLUST -> PixivRepository.addIllustComment(
                    id,
                    "",
                    stamp.stampId,
                    parentCommentId = parentId
                )

                CommentType.NOVEL -> PixivRepository.addNovelComment(
                    id,
                    "",
                    stamp.stampId,
                    parentCommentId = parentId
                )
            }
            currentInput.clearText()
            updateState { copy(isSending = false, replyTarget = null) }
            sendEffect(CommentSideEffect.CommentAdded(resp.comment.id, parentId))
        }
    }

    fun deleteComment(commentId: Long) {
        launchIO(
            onError = {
                ToastUtil.safeShortToast(RString.delete_comment_failed)
            }
        ) {
            when (type) {
                CommentType.ILLUST -> PixivRepository.deleteIllustComment(commentId)
                CommentType.NOVEL -> PixivRepository.deleteNovelComment(commentId)
            }
            sendEffect(CommentSideEffect.CommentDeleted)
        }
    }
}
