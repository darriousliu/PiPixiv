package com.mrl.pixiv.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.transparentIndicatorColors
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.compose.ui.PagingAppendState
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.cloud_history
import com.mrl.pixiv.strings.enable_history_now
import com.mrl.pixiv.strings.history_disabled_empty_desc
import com.mrl.pixiv.strings.history_disabled_empty_title
import com.mrl.pixiv.strings.history_setting
import com.mrl.pixiv.strings.local_history
import com.mrl.pixiv.strings.no_history_records
import com.mrl.pixiv.strings.search_by_title_author
import com.mrl.pixiv.strings.switch_to_illust_mode
import com.mrl.pixiv.strings.switch_to_novel_mode
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class HistorySource {
    Local,
    Cloud,
}

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val state = viewModel.asState()
    val isPremium by viewModel.isPremiumFlow.collectAsStateWithLifecycle()
    val historySettings by viewModel.userPreferenceFlow.collectAsStateWithLifecycle { historySettings }
    val historyEnabled = historySettings.enabled
    val cloudHistoryEnabled = historySettings.cloudEnabled
    val localIllustCount by viewModel.localIllustCount.collectAsStateWithLifecycle(0)
    val localNovelCount by viewModel.localNovelCount.collectAsStateWithLifecycle(0)
    var searchValue by remember { mutableStateOf(TextFieldValue(state.currentSearch)) }
    val scope = rememberCoroutineScope()
    val sources = remember(isPremium, historyEnabled, cloudHistoryEnabled) {
        if (isPremium && historyEnabled && cloudHistoryEnabled) {
            listOf(HistorySource.Local, HistorySource.Cloud)
        } else {
            listOf(HistorySource.Local)
        }
    }
    val pagerState = rememberPagerState { sources.size }
    val selectedTabIndex = pagerState.currentPage.coerceIn(0, sources.lastIndex)

    LaunchedEffect(sources.size) {
        if (pagerState.currentPage >= sources.size) {
            pagerState.scrollToPage(sources.lastIndex.coerceAtLeast(0))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                HistoryAppBar(
                    searchValue = searchValue,
                    onValueChange = {
                        searchValue = it
                        viewModel.dispatch(HistoryAction.UpdateSearch(it.text))
                    },
                    onBack = { navigationManager.popBackStack() },
                    onSettings = navigationManager::navigateToHistorySettingScreen,
                )
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    sources.forEachIndexed { index, source ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    text = stringResource(
                                        when (source) {
                                            HistorySource.Local -> RStrings.local_history
                                            HistorySource.Cloud -> RStrings.cloud_history
                                        }
                                    )
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            HistoryViewModeToggleButton(
                currentMode = state.mode,
                onModeChange = { viewModel.dispatch(HistoryAction.UpdateMode(it)) },
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            beyondViewportPageCount = sources.lastIndex.coerceAtLeast(0),
        ) { page ->
            val isActivePage = page == selectedTabIndex
            when (sources.getOrElse(page) { sources.last() }) {
                HistorySource.Local -> {
                    when (state.mode) {
                        AppViewMode.ILLUST -> {
                            val illusts = viewModel.localIllusts.collectAsLazyPagingItems()
                            IllustHistoryPage(
                                illusts = illusts,
                                lazyGridState = viewModel.localIllustGridState,
                                isActive = isActivePage,
                                showDisabledShortcut = !historyEnabled && localIllustCount == 0,
                                onOpenHistorySettings = navigationManager::navigateToHistorySettingScreen,
                                navToPictureScreen = navigationManager::navigateToPictureScreen,
                            )
                        }

                        AppViewMode.NOVEL -> {
                            val novels = viewModel.localNovels.collectAsLazyPagingItems()
                            NovelHistoryPage(
                                novels = novels,
                                lazyListState = viewModel.localNovelListState,
                                isActive = isActivePage,
                                showDisabledShortcut = !historyEnabled && localNovelCount == 0,
                                onOpenHistorySettings = navigationManager::navigateToHistorySettingScreen,
                                navToNovelDetailScreen = navigationManager::navigateToNovelDetailScreen,
                                navToNovelSeriesScreen = navigationManager::navigateToNovelSeriesScreen,
                            )
                        }
                    }
                }

                HistorySource.Cloud -> {
                    if (!historyEnabled) {
                        DisabledHistoryShortcut(
                            onOpenHistorySettings = navigationManager::navigateToHistorySettingScreen,
                        )
                    } else {
                        when (state.mode) {
                            AppViewMode.ILLUST -> {
                                val illusts = viewModel.cloudIllusts.collectAsLazyPagingItems()
                                IllustHistoryPage(
                                    illusts = illusts,
                                    lazyGridState = viewModel.cloudIllustGridState,
                                    isActive = isActivePage,
                                    showDisabledShortcut = false,
                                    onOpenHistorySettings = navigationManager::navigateToHistorySettingScreen,
                                    navToPictureScreen = navigationManager::navigateToPictureScreen,
                                )
                            }

                            AppViewMode.NOVEL -> {
                                val novels = viewModel.cloudNovels.collectAsLazyPagingItems()
                                NovelHistoryPage(
                                    novels = novels,
                                    lazyListState = viewModel.cloudNovelListState,
                                    isActive = isActivePage,
                                    showDisabledShortcut = false,
                                    onOpenHistorySettings = navigationManager::navigateToHistorySettingScreen,
                                    navToNovelDetailScreen = navigationManager::navigateToNovelDetailScreen,
                                    navToNovelSeriesScreen = navigationManager::navigateToNovelSeriesScreen,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryViewModeToggleButton(
    currentMode: AppViewMode,
    onModeChange: (AppViewMode) -> Unit,
) {
    FloatingActionButton(
        onClick = {
            onModeChange(
                when (currentMode) {
                    AppViewMode.ILLUST -> AppViewMode.NOVEL
                    AppViewMode.NOVEL -> AppViewMode.ILLUST
                }
            )
        }
    ) {
        when (currentMode) {
            AppViewMode.ILLUST -> {
                Icon(
                    imageVector = Icons.Rounded.Book,
                    contentDescription = stringResource(RStrings.switch_to_novel_mode),
                )
            }

            AppViewMode.NOVEL -> {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = stringResource(RStrings.switch_to_illust_mode),
                )
            }
        }
    }
}

@Composable
private fun IllustHistoryPage(
    illusts: LazyPagingItems<Illust>,
    lazyGridState: LazyGridState,
    isActive: Boolean,
    showDisabledShortcut: Boolean,
    onOpenHistorySettings: () -> Unit,
    navToPictureScreen: (List<Illust>, Int, String, Boolean) -> Unit,
) {
    val controller = remember(lazyGridState) {
        keyboardScrollerController(lazyGridState) {
            lazyGridState.layoutInfo.viewportSize.height.toFloat()
        }
    }
    if (isActive) {
        KeyEventListener(controller)
    }

    val layoutParams = IllustGridDefaults.relatedLayoutParameters()
    Box(modifier = Modifier.fillMaxSize()) {
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
                    .calculateBottomPadding() + 88.dp
            ),
        ) {
            illustGrid(
                illusts = illusts,
                navToPictureScreen = navToPictureScreen,
                enableLoading = false,
            )
            if (
                illusts.itemCount > 0 &&
                illusts.loadState.refresh is LoadState.NotLoading &&
                illusts.loadState.append !is LoadState.NotLoading
            ) {
                item(key = "append_state", span = { GridItemSpan(maxLineSpan) }) {
                    PagingAppendState(illusts.loadState, illusts.itemCount, illusts::retry)
                }
            }
        }
        HistoryPageOverlay(
            loadStates = illusts.loadState,
            itemCount = illusts.itemCount,
            onRetry = illusts::retry,
            showDisabledShortcut = showDisabledShortcut,
            onOpenHistorySettings = onOpenHistorySettings,
        )
        VerticalScrollbar(
            state = lazyGridState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun NovelHistoryPage(
    novels: LazyPagingItems<Novel>,
    lazyListState: LazyListState,
    isActive: Boolean,
    showDisabledShortcut: Boolean,
    onOpenHistorySettings: () -> Unit,
    navToNovelDetailScreen: (Long) -> Unit,
    navToNovelSeriesScreen: (Long) -> Unit,
) {
    val controller = remember(lazyListState) {
        keyboardScrollerController(lazyListState) {
            lazyListState.layoutInfo.viewportSize.height.toFloat()
        }
    }
    if (isActive) {
        KeyEventListener(controller)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 88.dp
            ),
        ) {
            items(
                count = novels.itemCount,
                key = novels.itemIndexKey { index, item -> "${index}_${item.id}" }
            ) { index ->
                novels[index]?.let { novel ->
                    NovelItem(
                        novel = novel,
                        onNovelClick = navToNovelDetailScreen,
                        onSeriesClick = navToNovelSeriesScreen,
                        onBookmarkClick = { isBookmarked, restrict, tags ->
                            if (isBookmarked) {
                                BookmarkState.deleteBookmarkNovel(novel.id)
                            } else {
                                BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                            }
                        }
                    )
                }
            }
            if (
                novels.itemCount > 0 &&
                novels.loadState.refresh is LoadState.NotLoading &&
                novels.loadState.append !is LoadState.NotLoading
            ) {
                item(key = "append_state") {
                    PagingAppendState(novels.loadState, novels.itemCount, novels::retry)
                }
            }
        }
        HistoryPageOverlay(
            loadStates = novels.loadState,
            itemCount = novels.itemCount,
            onRetry = novels::retry,
            showDisabledShortcut = showDisabledShortcut,
            onOpenHistorySettings = onOpenHistorySettings,
        )
        VerticalScrollbar(
            state = lazyListState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun HistoryPageOverlay(
    loadStates: CombinedLoadStates,
    itemCount: Int,
    onRetry: () -> Unit,
    showDisabledShortcut: Boolean,
    onOpenHistorySettings: () -> Unit,
) {
    if (
        showDisabledShortcut && itemCount == 0 &&
        loadStates.refresh is LoadState.NotLoading && loadStates.append is LoadState.NotLoading
    ) {
        DisabledHistoryShortcut(onOpenHistorySettings = onOpenHistorySettings)
    } else {
        PagingContentState(
            loadStates = loadStates,
            itemCount = itemCount,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize().padding(
                bottom = if (itemCount > 0) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp
                } else {
                    0.dp
                },
            ),
            emptyMessage = stringResource(RStrings.no_history_records),
        )
    }
}

@Composable
private fun DisabledHistoryShortcut(
    onOpenHistorySettings: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        ContentEmptyState(
            message = stringResource(RStrings.history_disabled_empty_title),
            description = stringResource(RStrings.history_disabled_empty_desc),
            icon = Icons.Rounded.HistoryToggleOff,
            action = {
                Button(onClick = onOpenHistorySettings) {
                    Text(text = stringResource(RStrings.enable_history_now))
                }
            },
        )
    }
}

@Composable
internal fun HistoryAppBar(
    searchValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    TopAppBar(
        title = {
            TextField(
                value = searchValue,
                onValueChange = onValueChange,
                colors = transparentIndicatorColors.copy(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        text = stringResource(RStrings.search_by_title_author),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = { onValueChange(TextFieldValue()) },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear"
                        )
                    }
                },
                singleLine = true,
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        },
        actions = {
            IconButton(onClick = onSettings, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = stringResource(RStrings.history_setting)
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}
