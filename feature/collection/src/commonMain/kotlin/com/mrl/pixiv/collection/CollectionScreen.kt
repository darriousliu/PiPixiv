package com.mrl.pixiv.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.collection.components.FilterDialog
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.isSelf
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.illusts
import com.mrl.pixiv.strings.jump
import com.mrl.pixiv.strings.jump_to_page_hint
import com.mrl.pixiv.strings.jump_to_page_invalid_input
import com.mrl.pixiv.strings.jump_to_page_out_of_range
import com.mrl.pixiv.strings.novels
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CollectionScreen(
    uid: Long,
    isNovel: Boolean,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel { parametersOf(uid) },
    navigationManager: NavigationManager = koinInject()
) {
    val state = viewModel.asState()
    val userBookmarksIllusts = viewModel.userBookmarksIllusts.collectAsLazyPagingItems()
    val userBookmarksNovels = viewModel.userBookmarksNovels.collectAsLazyPagingItems()
    val dispatch = viewModel::dispatch
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    val lazyGridState = rememberLazyGridState()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(if (isNovel) 1 else 0) { 2 }
    val isIllustPage = pagerState.currentPage == 0

    val illustRefreshTrigger by viewModel.illustRefreshTrigger.collectAsState()
    val novelRefreshTrigger by viewModel.novelRefreshTrigger.collectAsState()

    LaunchedEffect(illustRefreshTrigger) {
        if (illustRefreshTrigger > 0) {
            userBookmarksIllusts.refresh()
            lazyGridState.scrollToItem(0)
        }
    }

    LaunchedEffect(novelRefreshTrigger) {
        if (novelRefreshTrigger > 0) {
            userBookmarksNovels.refresh()
            lazyListState.scrollToItem(0)
        }
    }

    val illustController = remember {
        keyboardScrollerController(lazyGridState) {
            lazyGridState.layoutInfo.viewportSize.height.toFloat()
        }
    }
    val novelController = remember {
        keyboardScrollerController(lazyListState) {
            lazyListState.layoutInfo.viewportSize.height.toFloat()
        }
    }
    val activeController = if (isIllustPage) illustController else novelController
    KeyEventListener(activeController)

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                CollectionTopAppBar(
                    uid = uid,
                    showFilterDialog = { showFilterDialog = true },
                    onBack = { navigationManager.popBackStack() }
                )
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = isIllustPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text(text = stringResource(RStrings.illusts)) }
                    )
                    Tab(
                        selected = !isIllustPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text(text = stringResource(RStrings.novels)) }
                    )
                }
                JumpToPageRow(
                    isLoading = state.isJumpingPage,
                    error = state.jumpPageError,
                    onJump = { page ->
                        if (isIllustPage) {
                            dispatch(CollectionAction.JumpToPageIllust(page))
                        } else {
                            dispatch(CollectionAction.JumpToPageNovel(page))
                        }
                    },
                    onClearError = {
                        dispatch(CollectionAction.ClearJumpPageError)
                    }
                )
            }
        },
        floatingActionButton = {
            val canScrollBackward = if (isIllustPage)
                lazyGridState.canScrollBackward
            else
                lazyListState.canScrollBackward
            BackToTopButton(
                visibility = canScrollBackward,
                modifier = Modifier,
                onBackToTop = {
                    scope.launch {
                        if (isIllustPage) lazyGridState.scrollToItem(0)
                        else lazyListState.scrollToItem(0)
                    }
                },
                onRefresh = {
                    if (isIllustPage) userBookmarksIllusts.refresh()
                    else userBookmarksNovels.refresh()
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(paddingValues),
        ) { page ->
            when (page) {
                0 -> {
                    val layoutParams = IllustGridDefaults.relatedLayoutParameters()
                    val pullRefreshState = rememberPullToRefreshState()
                    val isRefreshing = userBookmarksIllusts.loadState.refresh is LoadState.Loading
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { userBookmarksIllusts.refresh() },
                        state = pullRefreshState,
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                state = pullRefreshState,
                                isRefreshing = isRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                    ) {
                        LazyVerticalGrid(
                            state = lazyGridState,
                            modifier = Modifier.fillMaxSize(),
                            columns = layoutParams.gridCells,
                            verticalArrangement = layoutParams.verticalArrangement,
                            horizontalArrangement = layoutParams.horizontalArrangement,
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                top = 8.dp,
                                end = 8.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            ),
                        ) {
                            illustGrid(
                                illusts = userBookmarksIllusts,
                                navToPictureScreen = navigationManager::navigateToPictureScreen,
                            )
                        }
                        VerticalScrollbar(
                            state = lazyGridState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }

                1 -> {
                    val novelPullRefreshState = rememberPullToRefreshState()
                    val isNovelRefreshing =
                        userBookmarksNovels.loadState.refresh is LoadState.Loading
                    PullToRefreshBox(
                        isRefreshing = isNovelRefreshing,
                        onRefresh = { userBookmarksNovels.refresh() },
                        state = novelPullRefreshState,
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                state = novelPullRefreshState,
                                isRefreshing = isNovelRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            ),
                        ) {
                            items(
                                count = userBookmarksNovels.itemCount,
                                key = userBookmarksNovels.itemIndexKey { index, item ->
                                    "${index}_${item.id}"
                                }
                            ) { index ->
                                userBookmarksNovels[index]?.let { novel ->
                                    NovelItem(
                                        novel = novel,
                                        onNovelClick = { novelId ->
                                            navigationManager.navigateToNovelDetailScreen(novelId)
                                        },
                                        onBookmarkClick = { isBookmarked, restrict, tags ->
                                            if (isBookmarked) {
                                                BookmarkState.deleteBookmarkNovel(novel.id)
                                            } else {
                                                BookmarkState.bookmarkNovel(
                                                    novel.id,
                                                    restrict,
                                                    tags
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            state = lazyListState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        if (showFilterDialog) {
            if (isIllustPage) {
                FilterDialog(
                    onDismissRequest = { showFilterDialog = false },
                    userBookmarkTags = state.userBookmarkTagsIllust,
                    privateBookmarkTags = state.privateBookmarkTagsIllust,
                    restrict = state.restrict,
                    filterTag = state.filterTag,
                    onLoadUserBookmarksTags = {
                        dispatch(CollectionAction.LoadUserBookmarksTagsIllust(it))
                    },
                    onSelected = { restrict, tag ->
                        viewModel.updateFilterTag(restrict, tag)
                        userBookmarksIllusts.refresh()
                    }
                )
            } else {
                FilterDialog(
                    onDismissRequest = { showFilterDialog = false },
                    userBookmarkTags = state.userBookmarkTagsNovel,
                    privateBookmarkTags = state.privateBookmarkTagsNovel,
                    restrict = state.novelRestrict,
                    filterTag = state.novelFilterTag,
                    onLoadUserBookmarksTags = {
                        dispatch(CollectionAction.LoadUserBookmarksTagsNovel(it))
                    },
                    onSelected = { restrict, tag ->
                        viewModel.updateNovelFilterTag(restrict, tag)
                        userBookmarksNovels.refresh()
                    }
                )
            }
        }
    }
}

@Composable
private fun JumpToPageRow(
    isLoading: Boolean,
    error: JumpPageError?,
    onJump: (Int) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageInput by rememberSaveable { mutableStateOf("") }
    var inputError by rememberSaveable { mutableStateOf(false) }

    val errorOutOfRange = stringResource(RStrings.jump_to_page_out_of_range)
    val errorInvalidInput = stringResource(RStrings.jump_to_page_invalid_input)

    val errorMessage = when {
        inputError -> errorInvalidInput
        error == JumpPageError.OUT_OF_RANGE -> errorOutOfRange
        error == JumpPageError.INVALID_INPUT -> errorInvalidInput
        else -> null
    }

    fun attemptJump() {
        val page = pageInput.trim().toIntOrNull()
        if (page == null || page < 1) {
            inputError = true
        } else {
            inputError = false
            onJump(page)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = pageInput,
            onValueChange = {
                pageInput = it
                inputError = false
                if (error != null) onClearError()
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = stringResource(RStrings.jump_to_page_hint)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { msg ->
                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(
                onGo = { attemptJump() }
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(),
        )
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedVisibility(visible = isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        AnimatedVisibility(visible = !isLoading) {
            Button(
                onClick = { attemptJump() },
                enabled = !isLoading,
            ) {
                Text(text = stringResource(RStrings.jump))
            }
        }
    }
}

@Composable
private fun CollectionTopAppBar(
    uid: Long,
    showFilterDialog: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    TopAppBar(
        modifier = Modifier.shadow(4.dp),
        title = {
            Text(text = stringResource(RStrings.collection))
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            if (uid.isSelf) {
                IconButton(
                    onClick = showFilterDialog,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Rounded.FilterList, contentDescription = null)
                }
            }
        }
    )
}