package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mrl.pixiv.collection.CollectionAction
import com.mrl.pixiv.collection.CollectionViewModel
import com.mrl.pixiv.collection.components.FilterDialog
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.RectangleIllustItem
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private const val KEY_TOP_SPACE = "top_space"

@Composable
fun CollectionPage(
    uid: Long,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(uid) },
    latestViewModel: LatestViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val userBookmarksIllusts = viewModel.userBookmarksIllusts.collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()
    val state = viewModel.asState()
    var showFilterDialog by remember { mutableStateOf(false) }
    val layoutParams = RecommendGridDefaults.coverLayoutParameters()
    val isRefreshing = userBookmarksIllusts.loadState.refresh is LoadState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { userBookmarksIllusts.refresh() },
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        Box {
            LazyVerticalStaggeredGrid(
                columns = layoutParams.gridCells,
                modifier = Modifier.fillMaxSize(),
                state = latestViewModel.collectionLazyGirdState,
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
                    count = userBookmarksIllusts.itemCount,
                    key = userBookmarksIllusts.itemKey { it.id }
                ) { index ->
                    val illust = userBookmarksIllusts[index] ?: return@items
                    val isBookmarked = illust.isBookmark
                    RectangleIllustItem(
                        illust = illust,
                        isBookmarked = isBookmarked,
                        navToPictureScreen = { prefix, enableTransition ->
                            navigationManager.navigateToPictureScreen(
                                userBookmarksIllusts.itemSnapshotList.items,
                                index,
                                prefix,
                                enableTransition
                            )
                        },
                        onBookmarkClick = { restrict, tags, isEdit ->
                            if (isEdit || !isBookmarked) {
                                BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                            } else {
                                BookmarkState.deleteBookmarkIllust(illust.id)
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
                    RString.word_public to Restrict.PUBLIC,
                    RString.word_private to Restrict.PRIVATE,
                )
                options.forEach { (label, restrict) ->
                    FilterChip(
                        selected = state.restrict == restrict,
                        onClick = {
                            viewModel.updateFilterTag(restrict, state.filterTag)
                            userBookmarksIllusts.refresh()
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
                IconButton(
                    onClick = { showFilterDialog = true },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null
                    )
                }
            }
        }
    }
    if (showFilterDialog) {
        FilterDialog(
            onDismissRequest = { showFilterDialog = false },
            userBookmarkTagsIllust = state.userBookmarkTagsIllust,
            privateBookmarkTagsIllust = state.privateBookmarkTagsIllust,
            restrict = state.restrict,
            filterTag = state.filterTag,
            onLoadUserBookmarksTags = {
                viewModel.dispatch(CollectionAction.LoadUserBookmarksTagsIllust(it))
            },
            onSelected = { restrict, tag ->
                viewModel.updateFilterTag(restrict, tag)
                userBookmarksIllusts.refresh()
            }
        )
    }
}
