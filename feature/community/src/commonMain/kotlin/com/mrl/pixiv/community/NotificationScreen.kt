package com.mrl.pixiv.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.ui.ContentPullToRefreshBox
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.image.LoadingImage
import com.mrl.pixiv.common.data.notification.PixivNotification
import com.mrl.pixiv.common.repository.CommunityRepository
import com.mrl.pixiv.common.repository.NotificationTarget
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.repository.resolveNotificationTarget
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.community_empty_notifications
import com.mrl.pixiv.strings.community_notification
import com.mrl.pixiv.strings.community_notifications
import com.mrl.pixiv.strings.community_refresh
import com.mrl.pixiv.strings.community_unknown_link
import com.mrl.pixiv.strings.community_unread_failed
import com.mrl.pixiv.strings.community_view_more
import com.mrl.pixiv.strings.retry
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NotificationScreen(
    onViewMore: (Long, String?) -> Unit,
    modifier: Modifier = Modifier,
    notificationId: Long? = null,
    title: String? = null,
    viewModel: NotificationViewModel = koinViewModel(key = "notifications-$notificationId") {
        parametersOf(notificationId)
    },
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val notifications = viewModel.notifications.collectAsLazyPagingItems()
    val unread by CommunityRepository.unreadState.collectAsStateWithLifecycle()
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val unavailableMessage = stringResource(RStrings.community_unknown_link)
    val refresh = {
        notifications.refresh()
        viewModel.refreshUnread()
    }
    LifecycleResumeEffect(userInfo.user.id) {
        // Re-entering from an artwork or group should use the server's current read flags.
        if (notifications.itemCount > 0) notifications.refresh()
        viewModel.refreshUnread()
        onPauseOrDispose {}
    }
    NotificationScaffold(
        title = title?.takeIf(String::isNotBlank)
            ?: stringResource(RStrings.community_notifications),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        onBack = navigationManager::popBackStack,
        onRefresh = refresh,
    ) { padding ->
        ContentPullToRefreshBox(
            isRefreshing = notifications.loadState.refresh is LoadState.Loading,
            hasContent = notifications.itemCount > 0,
            onRefresh = refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (unread.accountId == userInfo.user.id && unread.failed) {
                    item(key = "unread-error") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(RStrings.community_unread_failed),
                                Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::refreshUnread) {
                                Text(
                                    stringResource(
                                        RStrings.retry
                                    )
                                )
                            }
                        }
                    }
                }
                if (notifications.itemCount == 0 || notifications.loadState.refresh is LoadState.Error) {
                    item(key = "refresh-state") {
                        PagingContentState(
                            loadStates = notifications.loadState,
                            itemCount = notifications.itemCount,
                            onRetry = notifications::retry,
                            emptyMessage = stringResource(RStrings.community_empty_notifications),
                        )
                    }
                }
                items(
                    count = notifications.itemCount,
                    key = { index -> "notification-$index-${notifications.peek(index)?.id}" },
                ) { index ->
                    notifications[index]?.let { notification ->
                        NotificationRow(
                            notification = notification,
                            onViewMore = onViewMore,
                            onClick = {
                                when (val target =
                                    resolveNotificationTarget(notification.targetUrl)) {
                                    is NotificationTarget.Illust -> navigationManager.navigateToSinglePictureScreen(
                                        target.id
                                    )

                                    is NotificationTarget.Novel -> navigationManager.navigateToNovelDetailScreen(
                                        target.id
                                    )

                                    is NotificationTarget.User -> navigationManager.navigateToProfileDetailScreen(
                                        target.id
                                    )

                                    is NotificationTarget.Web -> if (runCatching {
                                            uriHandler.openUri(
                                                target.url
                                            )
                                        }.isFailure) {
                                        scope.launch { snackbar.showSnackbar(unavailableMessage) }
                                    }

                                    null -> scope.launch { snackbar.showSnackbar(unavailableMessage) }
                                }
                            },
                        )
                        HorizontalDivider(
                            Modifier.padding(start = 80.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
                if (notifications.itemCount > 0 && notifications.loadState.refresh is LoadState.NotLoading && notifications.loadState.append !is LoadState.NotLoading) {
                    item(key = "append-state") {
                        PagingAppendState(
                            notifications.loadState,
                            notifications.itemCount,
                            notifications::retry
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: PixivNotification,
    onViewMore: (Long, String?) -> Unit,
    onClick: () -> Unit,
) {
    val text = remember(notification.content.text) {
        notificationTextToAnnotatedString(notification.content.text)
    }
    Column(
        Modifier.fillMaxWidth().background(
            if (notification.isRead) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val avatar = notification.content.leftImage?.takeIf(String::isNotBlank)
            if (avatar != null) {
                LoadingImage(
                    model = avatar,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        null,
                        Modifier.padding(12.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text.ifBlank { AnnotatedString(stringResource(RStrings.community_notification)) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (notification.createdDatetime.isNotBlank()) {
                    Text(
                        runCatching { convertUtcStringToLocalDateTime(notification.createdDatetime) }.getOrDefault(
                            notification.createdDatetime
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!notification.isRead) {
                UnreadNotificationIndicator()
            }
            notification.content.rightImage?.takeIf(String::isNotBlank)?.let { image ->
                LoadingImage(
                    model = image,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        notification.viewMore?.let { more ->
            TextButton(
                onClick = { onViewMore(notification.id, more.title.takeIf(String::isNotBlank)) },
                modifier = Modifier.padding(start = 68.dp, bottom = 8.dp),
            ) {
                Text(
                    more.title.ifBlank { stringResource(RStrings.community_view_more) },
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    null,
                    Modifier.padding(start = 4.dp).size(18.dp)
                )
                if (more.unreadExists) UnreadNotificationIndicator(Modifier.padding(start = 8.dp))
            }
        }
    }
}


@Composable
private fun NotificationScaffold(
    title: String,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.back))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, stringResource(RStrings.community_refresh))
                    }
                },
            )
        },
        snackbarHost = snackbarHost,
    ) { padding -> content(padding) }
}
