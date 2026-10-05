package com.mrl.pixiv.community

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.ui.image.LoadingImage
import com.mrl.pixiv.common.data.notification.PixivNotification
import com.mrl.pixiv.common.repository.CommunityRepository
import com.mrl.pixiv.common.repository.NotificationTarget
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.repository.resolveNotificationTarget
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.community_empty_notifications
import com.mrl.pixiv.strings.community_notification
import com.mrl.pixiv.strings.community_notifications
import com.mrl.pixiv.strings.community_refresh
import com.mrl.pixiv.strings.community_unknown_link
import com.mrl.pixiv.strings.community_unread
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
    notificationId: Long? = null,
    title: String? = null,
    modifier: Modifier = Modifier,
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title?.takeIf(String::isNotBlank) ?: stringResource(RStrings.community_notifications)) },
                navigationIcon = {
                    IconButton(onClick = navigationManager::popBackStack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.back))
                    }
                },
                actions = {
                    IconButton(onClick = refresh) {
                        Icon(Icons.Rounded.Refresh, stringResource(RStrings.community_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = notifications.loadState.refresh is LoadState.Loading,
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
                            Text(stringResource(RStrings.community_unread_failed), Modifier.weight(1f))
                            TextButton(onClick = viewModel::refreshUnread) { Text(stringResource(RStrings.retry)) }
                        }
                    }
                }
                if (notifications.itemCount == 0 || notifications.loadState.refresh is LoadState.Error) {
                    item(key = "refresh-state") {
                        CommunityListStatus(
                            state = notifications.loadState.refresh,
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
                                when (val target = resolveNotificationTarget(notification.targetUrl)) {
                                    is NotificationTarget.Illust -> navigationManager.navigateToSinglePictureScreen(target.id)
                                    is NotificationTarget.Novel -> navigationManager.navigateToNovelDetailScreen(target.id)
                                    is NotificationTarget.User -> navigationManager.navigateToProfileDetailScreen(target.id)
                                    is NotificationTarget.Web -> if (runCatching { uriHandler.openUri(target.url) }.isFailure) {
                                        scope.launch { snackbar.showSnackbar(unavailableMessage) }
                                    }
                                    null -> scope.launch { snackbar.showSnackbar(unavailableMessage) }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
                if (notifications.itemCount > 0 && notifications.loadState.append !is LoadState.NotLoading) {
                    item(key = "append-state") {
                        CommunityListStatus(notifications.loadState.append, notifications::retry)
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
    val unreadDescription = stringResource(RStrings.community_unread)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            notification.content.leftImage?.takeIf(String::isNotBlank)?.let { image ->
                LoadingImage(
                    model = image,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    notification.content.text.ifBlank { stringResource(RStrings.community_notification) },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (notification.createdDatetime.isNotBlank()) {
                    Text(notification.createdDatetime, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (!notification.isRead) {
                Badge(Modifier.semantics { contentDescription = unreadDescription })
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
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
            ) {
                Text(more.title.ifBlank { stringResource(RStrings.community_view_more) })
                if (more.unreadExists) Badge(Modifier.padding(start = 8.dp).semantics {
                    contentDescription = unreadDescription
                })
            }
        }
    }
}
