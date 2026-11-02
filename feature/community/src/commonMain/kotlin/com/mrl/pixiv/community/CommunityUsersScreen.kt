package com.mrl.pixiv.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.ui.image.LoadingImage
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.discovery.CommunityUserPreview
import com.mrl.pixiv.common.data.discovery.CommunityUsersKind
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.isSelf
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.repository.util.filterBlockedTags
import com.mrl.pixiv.common.repository.viewmodel.follow.FollowState
import com.mrl.pixiv.common.repository.viewmodel.follow.isFollowing
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.community_empty_followers
import com.mrl.pixiv.strings.community_empty_users
import com.mrl.pixiv.strings.community_followers
import com.mrl.pixiv.strings.community_recommended_users
import com.mrl.pixiv.strings.community_refresh
import com.mrl.pixiv.strings.community_related_users
import com.mrl.pixiv.strings.follow
import com.mrl.pixiv.strings.followed
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CommunityUsersScreen(
    kind: CommunityUsersKind,
    modifier: Modifier = Modifier,
    seedUserId: Long? = null,
    viewModel: CommunityUsersViewModel = koinViewModel(key = "community-users-$kind-$seedUserId") {
        parametersOf(kind, seedUserId)
    },
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val users = viewModel.users.collectAsLazyPagingItems()
    val title = when (kind) {
        CommunityUsersKind.RECOMMENDED -> RStrings.community_recommended_users
        CommunityUsersKind.RELATED -> RStrings.community_related_users
        CommunityUsersKind.FOLLOWERS -> RStrings.community_followers
    }
    CommunityUsersScaffold(
        title = stringResource(title),
        modifier = modifier,
        onBack = navigationManager::popBackStack,
        onRefresh = users::refresh,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = users.loadState.refresh is LoadState.Loading,
            onRefresh = users::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (users.itemCount == 0 || users.loadState.refresh is LoadState.Error) {
                    item(key = "refresh-state", span = { GridItemSpan(maxLineSpan) }) {
                        CommunityListStatus(
                            state = users.loadState.refresh,
                            onRetry = users::retry,
                            emptyMessage = stringResource(
                                if (kind == CommunityUsersKind.FOLLOWERS) {
                                    RStrings.community_empty_followers
                                } else RStrings.community_empty_users
                            ),
                        )
                    }
                }
                items(
                    count = users.itemCount,
                    key = { index -> "user-$index-${users.peek(index)?.user?.id}" },
                ) { index ->
                    users[index]?.let { preview -> CommunityUserCard(preview, navigationManager) }
                }
                if (users.itemCount > 0 && users.loadState.append !is LoadState.NotLoading) {
                    item(key = "append-state", span = { GridItemSpan(maxLineSpan) }) {
                        CommunityListStatus(users.loadState.append, users::retry)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityUserCard(preview: CommunityUserPreview, navigationManager: NavigationManager) {
    val user = preview.user
    val preference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val locallyBlocked = BlockingRepositoryV2.collectUserBlockAsState(user.id)
    val openProfile = { navigationManager.navigateToProfileDetailScreen(user.id) }
    // Keep relationship information available without exposing muted/locally blocked previews.
    val works = if (preview.isMuted || locallyBlocked) emptyList() else preview.illusts
        .filter { it.visible != false && (preference.isR18Enabled || it.xRestrict == XRestrict.Normal) }
        .filterBlockedTags()
        .take(3)
    CommunityUserCardContent(
        user = user,
        works = works,
        following = user.isFollowing,
        showFollow = !user.isSelf,
        onProfileClick = openProfile,
        onWorkClick = navigationManager::navigateToSinglePictureScreen,
        onFollowClick = {
            if (user.isFollowing) FollowState.unFollowUser(user.id) else FollowState.followUser(user.id)
        },
    )
}

@Composable
private fun CommunityUserCardContent(
    user: User,
    works: List<Illust>,
    following: Boolean,
    showFollow: Boolean = true,
    onProfileClick: () -> Unit = {},
    onWorkClick: (Long) -> Unit = {},
    onFollowClick: () -> Unit = {},
) {
    Card(
        onClick = onProfileClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (works.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                works.forEach { illust ->
                    LoadingImage(
                        model = illust.imageUrls.squareMedium.ifBlank { illust.imageUrls.medium },
                        contentDescription = illust.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f)
                            .aspectRatio(if (works.size == 1) 2.4f else 1f).clickable {
                                onWorkClick(illust.id)
                            },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (user.profileImageUrls.medium.isBlank()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        null,
                        Modifier.padding(10.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                UserAvatar(
                    user.profileImageUrls.medium,
                    Modifier.size(44.dp),
                    onClick = onProfileClick
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    user.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.account.isNotBlank()) Text(
                    "@${user.account}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showFollow) {
                if (following) {
                    OutlinedButton(
                        onClick = onFollowClick,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(stringResource(RStrings.followed))
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(stringResource(RStrings.follow))
                    }
                }
            }
        }
    }
}


@Composable
private fun CommunityUsersScaffold(
    title: String,
    modifier: Modifier = Modifier,
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
    ) { padding -> content(padding) }
}
