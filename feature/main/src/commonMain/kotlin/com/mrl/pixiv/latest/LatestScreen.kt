package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.analytics.logEvent
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.layout.isWidthCompact
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.follow.FollowingScreenBody
import com.mrl.pixiv.follow.FollowingViewModel
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.latest_tab_following
import com.mrl.pixiv.strings.latest_tab_trend
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LatestScreen(
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val pages = remember { LatestPage.entries }
    val pagerState = viewModel.pagerState
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val refreshFlow = remember { MutableSharedFlow<LatestPage>() }

    LaunchedEffect(pagerState.currentPage) {
        logEvent("screen_view", buildMap {
            put("screen_name", "Latest")
            put("page_name", pages[pagerState.currentPage].name)
        })
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            val windowAdaptiveInfo = currentWindowAdaptiveInfo()
            val scrollState = when (pages[pagerState.currentPage]) {
                LatestPage.Trend -> viewModel.trendingLazyGirdState
                LatestPage.Collection -> viewModel.collectionLazyGirdState
                LatestPage.Following -> if (windowAdaptiveInfo.isWidthAtLeastMedium) {
                    viewModel.followingLazyGirdState
                } else {
                    viewModel.followingLazyListState
                }
            }
            BackToTopButton(
                visibility = scrollState.canScrollBackward,
                modifier = Modifier,
                onBackToTop = {
                    when (scrollState) {
                        is LazyListState -> scope.launch { scrollState.scrollToItem(0) }
                        is LazyGridState -> scope.launch { scrollState.scrollToItem(0) }
                        is LazyStaggeredGridState -> scope.launch { scrollState.scrollToItem(0) }
                    }
                },
                onRefresh = {
                    scope.launch {
                        refreshFlow.emit(pages[pagerState.currentPage])
                    }
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) {
        Column(modifier = Modifier.padding(it)) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth(if (windowAdaptiveInfo.isWidthCompact) 1f else 0.5f)
                    .padding(horizontal = 16.dp)
            ) {
                pages.forEachIndexed { index, it ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(
                                when (it) {
                                    LatestPage.Trend -> RStrings.latest_tab_trend
                                    LatestPage.Collection -> RStrings.collection
                                    LatestPage.Following -> RStrings.latest_tab_following
                                }
                            )
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                val page = pages[index]
                when (page) {
                    LatestPage.Trend -> {
                        TrendingPage(refreshFlow=refreshFlow)
                    }

                    LatestPage.Collection -> {
                        CollectionPage(
                            uid = userInfo.user.id,
                            refreshFlow=refreshFlow
                        )
                    }

                    LatestPage.Following -> {
                        val followingViewModel =
                            koinViewModel<FollowingViewModel> { parametersOf(userInfo.user.id) }
                        val followingUsers =
                            followingViewModel.publicFollowingPageSource.collectAsLazyPagingItems()
                        LaunchedEffect(Unit) {
                            refreshFlow.collect {
                                followingUsers.refresh()
                            }
                        }
                        FollowingScreenBody(
                            followingUsers = followingUsers,
                            navToPictureScreen = navigationManager::navigateToPictureScreen,
                            navToUserProfile = navigationManager::navigateToProfileDetailScreen,
                            lazyListState = viewModel.followingLazyListState,
                            lazyGridState = viewModel.followingLazyGirdState,
                        )
                    }
                }
            }
        }
    }
}