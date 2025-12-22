package com.mrl.pixiv.comment

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.comment.components.CommentInput
import com.mrl.pixiv.comment.components.CommentInputPlaceholder
import com.mrl.pixiv.comment.components.CommentItem
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.hPadding
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.CommentRepository
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.ReportType
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.common.viewmodel.state
import com.mrl.pixiv.strings.comment_success
import com.mrl.pixiv.strings.delete_comment_success
import com.mrl.pixiv.strings.view_comments
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
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
    val replies = viewModel.replies.collectAsLazyPagingItems()
    val state = viewModel.asState()
    val listState = rememberLazyListState()
    val repliesListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var scrollToTopAfterRefresh by remember { mutableStateOf(false) }
    var hasStartedLoading by remember { mutableStateOf(false) }
    var showInputSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is CommentSideEffect.CommentAdded -> {
                    if (effect.parentCommentId == null) {
                        comments.refresh()
                        scrollToTopAfterRefresh = true
                        hasStartedLoading = false
                    } else if (viewModel.state.expandedComment != null) {
                        replies.refresh()
                    }
                    showInputSheet = false
                    ToastUtil.safeShortToast(RStrings.comment_success)
                }

                is CommentSideEffect.CommentDeleted -> {
                    comments.refresh()
                    if (viewModel.state.expandedComment != null) {
                        replies.refresh()
                    }
                    ToastUtil.safeShortToast(RStrings.delete_comment_success)
                }
            }
        }
    }

    LaunchedEffect(comments.loadState) {
        if (scrollToTopAfterRefresh) {
            if (comments.loadState.refresh is LoadState.Loading) {
                hasStartedLoading = true
            } else if (hasStartedLoading && comments.loadState.refresh is LoadState.NotLoading) {
                listState.animateScrollToItem(0)
                scrollToTopAfterRefresh = false
                hasStartedLoading = false
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
                    Text(text = stringResource(RStrings.view_comments))
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
            val isSending = state.isSending
            CommentInput(
                state = viewModel.currentInput,
                isSending = isSending,
                emojis = emojis?.emojiDefinitions.orEmpty().toPersistentList(),
                stamps = stamps?.stamps.orEmpty().toPersistentList(),
                onInsertEmoji = { viewModel.insertEmoji(it, isSubComment = false) },
                onSendStamp = {
                    viewModel.sendStamp(it, isSubComment = false)
                    focusManager.clearFocus()
                },
                onSendText = {
                    viewModel.sendText(isSubComment = false)
                    focusManager.clearFocus()
                },
                replyTarget = state.replyTarget,
                onClearReplyTarget = { viewModel.setReplyTarget(null) },
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
                state = listState,
                contentPadding = 8.hPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.setReplyTarget(null)
                            }
                        )
                    }
            ) {
                items(
                    count = comments.itemCount,
                    key = comments.itemKey { it.id }
                ) { index ->
                    val comment = comments[index] ?: return@items
                    val isBlocked = BlockingRepositoryV2.collectCommentBlockAsState(comment.id)
                    if (isBlocked) return@items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        CommentItem(
                            comment = comment,
                            onReplyComment = {
                                viewModel.setReplyTarget(comment)
                                scope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            },
                            onBlockComment = {
                                BlockingRepositoryV2.blockComment(comment)
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
                            onViewReplies = {
                                viewModel.setExpandedComment(comment)
                            },
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth(),
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

    if (state.expandedComment != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setExpandedComment(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxHeight(2 / 3f)) {
                RepliesContent(
                    parentComment = state.expandedComment,
                    replies = replies,
                    listState = repliesListState,
                    navigationManager = navigationManager,
                    onReply = { comment, index ->
                        viewModel.setSubCommentReplyTarget(comment)
                        showInputSheet = true
                        scope.launch {
                            repliesListState.animateScrollToItem(index)
                        }
                    },
                    onCancelReply = {
                        viewModel.setSubCommentReplyTarget(null)
                    },
                    onDeleteComment = { id ->
                        viewModel.deleteComment(id)
                    },
                    modifier = Modifier.weight(1f)
                )
                CommentInputPlaceholder(
                    onClick = { showInputSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showInputSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInputSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RectangleShape,
            dragHandle = null
        ) {
            val focusManager = LocalFocusManager.current
            val isSending = state.isSending
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            CommentInput(
                state = viewModel.subCommentInput,
                isSending = isSending,
                emojis = emojis?.emojiDefinitions.orEmpty().toPersistentList(),
                stamps = stamps?.stamps.orEmpty().toPersistentList(),
                onInsertEmoji = { viewModel.insertEmoji(it, isSubComment = true) },
                onSendStamp = {
                    viewModel.sendStamp(it, isSubComment = true)
                    focusManager.clearFocus()
                },
                onSendText = {
                    viewModel.sendText(isSubComment = true)
                    focusManager.clearFocus()
                },
                replyTarget = state.subCommentReplyTarget,
                onClearReplyTarget = { viewModel.setSubCommentReplyTarget(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                focusRequester = focusRequester
            )
        }
    }
}

@Composable
private fun RepliesContent(
    parentComment: Comment,
    replies: LazyPagingItems<Comment>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    navigationManager: NavigationManager,
    onReply: (Comment, Int) -> Unit,
    onCancelReply: () -> Unit,
    onDeleteComment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        contentPadding = 8.hPadding,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onCancelReply()
                    }
                )
            }
    ) {
        item(key = parentComment.id) {
            CommentItem(
                comment = parentComment,
                onReplyComment = {
                    onReply(parentComment, 0)
                },
                onBlockComment = {
                    BlockingRepositoryV2.blockComment(parentComment)
                },
                onReportComment = {
                    navigationManager.navigateToReportCommentScreen(
                        parentComment.id,
                        ReportType.ILLUST_COMMENT
                    )
                },
                onNavToUserProfile = {
                    navigationManager.navigateToProfileDetailScreen(parentComment.user.id)
                },
                onDeleteComment = {
                    onDeleteComment(parentComment.id)
                },
                onViewReplies = null,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
        }

        items(
            count = replies.itemCount,
            key = replies.itemKey { it.id }
        ) { index ->
            val comment = replies[index] ?: return@items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(start = 32.dp)
            ) {
                CommentItem(
                    comment = comment,
                    onReplyComment = {
                        onReply(comment, index + 1)
                    },
                    onBlockComment = {
                        BlockingRepositoryV2.blockComment(comment)
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
                        onDeleteComment(comment.id)
                    },
                    onViewReplies = null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (index != replies.itemCount - 1) {
                    8.VSpacer
                    HorizontalDivider()
                }
            }
        }
    }
}
