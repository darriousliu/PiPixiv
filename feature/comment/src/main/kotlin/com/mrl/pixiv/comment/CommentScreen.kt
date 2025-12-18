package com.mrl.pixiv.comment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.comment.components.CommentInput
import com.mrl.pixiv.comment.components.CommentItem
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.hPadding
import com.mrl.pixiv.common.repository.BlockingRepository
import com.mrl.pixiv.common.repository.CommentRepository
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.ReportType
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.asState
import kotlinx.collections.immutable.toPersistentList
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun CommentScreen(
    id: Long,
    type: CommentType,
    modifier: Modifier = Modifier,
    viewModel: CommentViewModel = koinViewModel { parametersOf(id, type) },
) {
    val navigationManager = koinInject<NavigationManager>()
    val emojis by CommentRepository.emojiCacheFlow.collectAsStateWithLifecycle()
    val stamps by CommentRepository.stampsCacheFlow.collectAsStateWithLifecycle()
    val comments = viewModel.commentList.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is CommentSideEffect.CommentAdded -> {
                    // 刷新评论列表
                    comments.refresh()
                    ToastUtil.safeShortToast(RString.comment_success)
                }
                is CommentSideEffect.CommentDeleted -> {
                    comments.refresh()
                    ToastUtil.safeShortToast(RString.delete_comment_success)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RString.view_comments))
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navigationManager.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            val focusManager = LocalFocusManager.current
            val isSending = viewModel.asState().isSending
            CommentInput(
                state = viewModel.currentInput,
                isSending = isSending,
                emojis = emojis?.emojiDefinitions.orEmpty().toPersistentList(),
                stamps = stamps?.stamps.orEmpty().toPersistentList(),
                onInsertEmoji = viewModel::insertEmoji,
                onSendStamp = {
                    viewModel.sendStamp(it)
                    focusManager.clearFocus()
                },
                onSendText = {
                    viewModel.sendText()
                    focusManager.clearFocus()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = comments.loadState.refresh is LoadState.Loading,
            onRefresh = { comments.refresh() },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = 8.hPadding
            ) {
                items(
                    count = comments.itemCount,
                    key = comments.itemKey { it.id }
                ) { index ->
                    val comment = comments[index] ?: return@items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        CommentItem(
                            comment = comment,
                            onReplyComment = {

                            },
                            onBlockComment = {
                                BlockingRepository.blockComment(comment.id)
                            },
                            onReportComment = {
                                navigationManager.navigateToReportCommentScreen(
                                    comment.id,
                                    ReportType.ILLUST_COMMENT
                                )
                            },
                            onNavToUserProfile = {
                                navigationManager.navigateToProfileDetailScreen(comment.user.id)
                            },
                            onDeleteComment = {
                                viewModel.deleteComment(comment.id)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (index != comments.itemCount - 1) {
                            8.VSpacer
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}


