package com.mrl.pixiv.follow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.deepBlue
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.layout.isWidthCompact
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.compose.ui.illust.SquareIllustItem
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.datasource.local.mmkv.isSelf
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.viewmodel.follow.FollowState
import com.mrl.pixiv.common.viewmodel.follow.isFollowing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

enum class FollowingPage {
    PUBLIC,
    PRIVATE,
}

@Composable
fun FollowingScreen(
    uid: Long,
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = koinViewModel { parametersOf(uid) },
    navigationManager: NavigationManager = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val pages = remember {
        if (uid.isSelf) listOf(FollowingPage.PUBLIC, FollowingPage.PRIVATE)
        else listOf(FollowingPage.PUBLIC)
    }
    val pagerState = rememberPagerState { pages.size }
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RString.followed))
                },
                navigationIcon = {
                    IconButton(
                        onClick = rememberThrottleClick {
                            navigationManager.popBackStack()
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
        ) {
            if (pages.size > 1) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth(if (windowAdaptiveInfo.isWidthCompact) 1f else 0.5f),
                ) {
                    pages.forEachIndexed { index, page ->
                        Tab(
                            text = { Text(text = stringResource(if (page == FollowingPage.PUBLIC) RString.word_public else RString.word_private)) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage == index) return@launch
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        )
                    }
                }
            }
            FollowingScreenBody(
                uid = uid,
                navToPictureScreen = navigationManager::navigateToPictureScreen,
                navToUserProfile = navigationManager::navigateToProfileDetailScreen,
                pages = pages.toImmutableList(),
                pagerState = pagerState,
                modifier = Modifier.weight(1f),
                viewModel = viewModel,
            )
        }
    }
}

@Composable
fun FollowingScreenBody(
    uid: Long,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    navToUserProfile: (Long) -> Unit,
    pages: ImmutableList<FollowingPage>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = koinViewModel { parametersOf(uid) },
    userScrollEnabled: Boolean = true,
    lazyListState: List<LazyListState> = List(pagerState.pageCount) { rememberLazyListState() },
    lazyGridState: List<LazyGridState> = List(pagerState.pageCount) { rememberLazyGridState() },
) {
    val pullRefreshState = rememberPullToRefreshState()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = userScrollEnabled
    ) {
        val page = pages[it]
        val followingUsers = if (page == FollowingPage.PUBLIC) {
            viewModel.publicFollowingPageSource.collectAsLazyPagingItems()
        } else {
            viewModel.privateFollowingPageSource.collectAsLazyPagingItems()
        }
        PullToRefreshBox(
            isRefreshing = followingUsers.loadState.refresh is LoadState.Loading,
            onRefresh = { followingUsers.refresh() },
            state = pullRefreshState
        ) {
            if (windowAdaptiveInfo.isWidthAtLeastMedium) {
                val layoutParams = IllustGridDefaults.userFollowingParameters()
                LazyVerticalGrid(
                    columns = layoutParams.gridCells,
                    state = lazyGridState[it],
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 10.dp,
                        end = 16.dp,
                        bottom = 20.dp
                    ),
                    horizontalArrangement = layoutParams.horizontalArrangement,
                    verticalArrangement = layoutParams.verticalArrangement,
                ) {
                    items(
                        followingUsers.itemCount,
                        key = followingUsers.itemKey { it.user.id }
                    ) {
                        val userPreview = followingUsers[it] ?: return@items
                        FollowingUserCard(
                            illusts = userPreview.illusts.toImmutableList(),
                            userName = userPreview.user.name,
                            userId = userPreview.user.id,
                            userAvatar = userPreview.user.profileImageUrls.medium,
                            isFollowed = userPreview.user.isFollowing,
                            navToPictureScreen = navToPictureScreen,
                            navToUserProfile = {
                                navToUserProfile(userPreview.user.id)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState[it],
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 10.dp,
                        end = 16.dp,
                        bottom = 20.dp
                    ),
                    verticalArrangement = 10f.spaceBy,
                ) {
                    items(
                        followingUsers.itemCount,
                        key = followingUsers.itemKey { it.user.id }
                    ) {
                        val userPreview = followingUsers[it] ?: return@items
                        FollowingUserCard(
                            illusts = userPreview.illusts.toImmutableList(),
                            userName = userPreview.user.name,
                            userId = userPreview.user.id,
                            userAvatar = userPreview.user.profileImageUrls.medium,
                            isFollowed = userPreview.user.isFollowing,
                            navToPictureScreen = navToPictureScreen,
                            navToUserProfile = {
                                navToUserProfile(userPreview.user.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingUserCard(
    illusts: ImmutableList<Illust>,
    userName: String,
    userId: Long,
    userAvatar: String,
    isFollowed: Boolean,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    navToUserProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row {
            illusts.take(3).forEachIndexed { index, it ->
                val isBookmarked = it.isBookmark
                SquareIllustItem(
                    illust = it,
                    isBookmarked = isBookmarked,
                    onBookmarkClick = { restrict, tags ->
                        if (isBookmarked) {
                            BookmarkState.deleteBookmarkIllust(it.id)
                        } else {
                            BookmarkState.bookmarkIllust(it.id, restrict, tags)
                        }
                    },
                    navToPictureScreen = { prefix, enableTransition ->
                        navToPictureScreen(illusts, index, prefix, enableTransition)
                    },
                    modifier = Modifier.weight(1f),
                    elevation = 0.dp,
                    shape = RectangleShape
                )
            }
        }
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = 8f.spaceBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                url = userAvatar,
                onClick = navToUserProfile,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = userName,
                modifier = Modifier.weight(1f)
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .then(
                        if (isFollowed)
                            Modifier.background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium
                            )
                        else
                            Modifier.border(
                                width = 1.dp,
                                color = deepBlue,
                                shape = MaterialTheme.shapes.medium
                            )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .throttleClick {
                        if (isFollowed) {
                            FollowState.unFollowUser(userId)
                        } else {
                            FollowState.followUser(userId)
                        }
                    },
                text = stringResource(if (isFollowed) RString.followed else RString.follow),
                style = TextStyle(
                    color = if (isFollowed) Color.White else deepBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

        }
    }
}

@Preview
@Composable
private fun FollowingUserCardPreview() {
    FollowingUserCard(
        illusts = persistentListOf(),
        userName = "asdasd",
        userId = 0,
        userAvatar = "http://iph.href.lu/200x200",
        isFollowed = false,
        navToPictureScreen = { _, _, _, _ -> },
        navToUserProfile = { },
    )
}