package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.datasource.local.mmkv.requireUserInfoFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.follow.FollowingPage
import com.mrl.pixiv.follow.FollowingScreenBody
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun LatestScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val pages = remember { LatestPage.entries.toList() }
    val pagerState = rememberPagerState { pages.size }
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        floatingActionButton = {},
        floatingActionButtonPosition = FabPosition.Center
    ) {
        Column(modifier = Modifier.padding(it)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.padding(horizontal = 16.dp)
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
                                    LatestPage.TREND -> RString.latest_tab_trend
                                    LatestPage.COLLECTION -> RString.collection
                                    LatestPage.FOLLOWING -> RString.latest_tab_following
                                }
                            )
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) {
                val page = pages[it]
                when (page) {
                    LatestPage.TREND -> {
                        TrendingPage()
                    }

                    LatestPage.COLLECTION -> {
                        CollectionPage(
                            uid = userInfo.user.id,
                        )
                    }

                    LatestPage.FOLLOWING -> {
                        val pages = remember {
                            persistentListOf(FollowingPage.PUBLIC, FollowingPage.PRIVATE)
                        }
                        val pagerState = rememberPagerState { pages.size }
                        FollowingScreenBody(
                            uid = userInfo.user.id,
                            navToPictureScreen = navigationManager::navigateToPictureScreen,
                            navToUserProfile = navigationManager::navigateToProfileDetailScreen,
                            pages = pages,
                            pagerState = pagerState,
                            userScrollEnabled = false,
                        )
                    }
                }
            }
        }
    }
}