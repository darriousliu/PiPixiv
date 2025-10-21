package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.bookmark.isBookmark
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val KEY_TOP_SPACE = "top_space"

@Composable
fun TrendingPage(
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
) {
    val navigationManager = koinInject<NavigationManager>()
    val pullRefreshState = rememberPullToRefreshState()
    val illustsFollowing = viewModel.illustsFollowing.collectAsLazyPagingItems()
    val trendingFilter by viewModel.trendingFilter.collectAsStateWithLifecycle()
    val layoutParams = RecommendGridDefaults.coverLayoutParameters()

    PullToRefreshBox(
        isRefreshing = illustsFollowing.loadState.refresh is LoadState.Loading,
        onRefresh = { illustsFollowing.refresh() },
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState
    ) {
        Box {
            LazyVerticalStaggeredGrid(
                columns = layoutParams.gridCells,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 10.dp),
                verticalItemSpacing = layoutParams.verticalArrangement.spacing,
                horizontalArrangement = layoutParams.horizontalArrangement,
            ) {
                item(
                    key = KEY_TOP_SPACE,
                    span = StaggeredGridItemSpan.FullLine
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                }
                items(
                    count = illustsFollowing.itemCount,
                    key = illustsFollowing.itemKey { it.id }
                ) { index ->
                    val illust = illustsFollowing[index] ?: return@items
                    val isBookmarked = illust.isBookmark
                    RectangleIllustItem(
                        illust = illust,
                        isBookmarked = isBookmarked,
                        navToPictureScreen = { prefix ->
                            navigationManager.navigateToPictureScreen(
                                illustsFollowing.itemSnapshotList.items,
                                index,
                                prefix
                            )
                        },
                        onBookmarkClick = { restrict, tags ->
                            if (isBookmarked) {
                                BookmarkState.deleteBookmarkIllust(illust.id)
                            } else {
                                BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                            }
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalArrangement = 8f.spaceBy
            ) {
                val options = listOf(
                    RString.all to Restrict.ALL,
                    RString.word_public to Restrict.PUBLIC,
                    RString.word_private to Restrict.PRIVATE,
                )
                options.forEach { (label, restrict) ->
                    FilterChip(
                        selected = trendingFilter == restrict,
                        onClick = {
                            viewModel.updateRestrict(restrict)
                            illustsFollowing.refresh()
                        },
                        label = {
                            Text(
                                text = stringResource(id = label)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}