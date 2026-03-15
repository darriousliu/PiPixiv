package com.mrl.pixiv.ranking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.analytics.logEvent
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.compose.ui.BackToTopButton
import com.mrl.pixiv.common.compose.ui.VerticalScrollbar
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.compose.ui.novel.NovelItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.itemIndexKey
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.main.components.ViewModeToggleButton
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.r18
import com.mrl.pixiv.strings.ranking
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@Composable
fun RankingScreen(
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val scope = rememberCoroutineScope()
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }
    val availableModes = state.availableModes(appViewMode)

    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        val selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= Clock.System.now().minus(1.days).toEpochMilliseconds()
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault()).year
                }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = when (appViewMode) {
                AppViewMode.ILLUST -> viewModel.getIllustRankingDate(state.currentMode)
                AppViewMode.NOVEL -> viewModel.getNovelRankingDate(state.currentMode)
            }?.atTime(0, 0)?.toInstant(TimeZone.UTC)?.toEpochMilliseconds(),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                            viewModel.changeDate(date, appViewMode)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(RStrings.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(RStrings.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val pagerState = key(availableModes) {
        rememberPagerState(
            initialPage = 0,
            pageCount = { availableModes.size }
        )
    }

    // Sync external mode selection (if any) with pager
    LaunchedEffect(state.currentMode, availableModes) {
        val index = availableModes.indexOf(state.currentMode)
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.scrollToPage(index)
        }
    }

    // Sync pager scroll with mode selection
    LaunchedEffect(pagerState, availableModes) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (page in availableModes.indices) {
                val mode = availableModes[page]
                if (state.currentMode != mode) {
                    viewModel.selectMode(mode)
                }
                logEvent("screen_view", buildMap {
                    put("screen_name", "Ranking")
                    put("mode", mode.value)
                })
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = stringResource(RStrings.ranking)) },
                    actions = {
                        val r18Enabled by requireUserPreferenceFlow.collectAsStateWithLifecycle { isR18Enabled }
                        LaunchedEffect(Unit) {
                            requireUserPreferenceFlow.map { it.isR18Enabled }.distinctUntilChanged()
                                .collect { r18Enabled ->
                                    if (!r18Enabled && state.showR18) {
                                        viewModel.toggleR18()
                                    }
                                }
                        }
                        if (r18Enabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = stringResource(RStrings.r18))
                                5.HSpacer
                                Switch(
                                    checked = state.showR18,
                                    onCheckedChange = { viewModel.toggleR18() }
                                )
                                8.HSpacer
                            }
                        }
                    }
                )
                Row {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage.coerceAtMost(
                            availableModes.lastIndex.coerceAtLeast(0)
                        ),
                        modifier = Modifier.weight(1f),
                        edgePadding = 0.dp
                    ) {
                        availableModes.forEachIndexed { index, mode ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch {
                                        pagerState.scrollToPage(index)
                                    }
                                },
                                text = {
                                    Text(text = stringResource(mode.title))
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = { showDatePicker = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            Column {
                val mode = availableModes.getOrNull(pagerState.currentPage)
                if (mode != null) {
                    val scrollToTop = suspend {
                        when (appViewMode) {
                            AppViewMode.ILLUST -> viewModel.getLazyStaggeredGridState(mode)
                                .scrollToItem(0)

                            AppViewMode.NOVEL -> viewModel.getLazyListState(mode).scrollToItem(0)
                        }
                    }
                    BackToTopButton(
                        visibility = when (appViewMode) {
                            AppViewMode.ILLUST -> viewModel.getLazyStaggeredGridState(mode).canScrollBackward
                            AppViewMode.NOVEL -> viewModel.getLazyListState(mode).canScrollBackward
                        },
                        modifier = Modifier,
                        onBackToTop = scrollToTop,
                        onRefresh = { viewModel.refresh(mode) }
                    )
                }
                8.VSpacer
                ViewModeToggleButton(
                    currentMode = appViewMode,
                    onModeChange = viewModel::switchViewMode
                )
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) { page ->
            val mode = availableModes.getOrNull(page) ?: return@HorizontalPager

            when (appViewMode) {
                AppViewMode.ILLUST -> {
                    IllustMode(
                        mode = mode,
                        navigateToPictureScreen = navigationManager::navigateToPictureScreen,
                        viewModel = viewModel,
                    )
                }

                AppViewMode.NOVEL -> {
                    NovelMode(
                        mode = mode,
                        navigateToNovelDetailScreen = navigationManager::navigateToNovelDetailScreen,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun IllustMode(
    mode: RankingMode,
    navigateToPictureScreen: NavigateToHorizontalPictureScreen,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = koinViewModel(),
) {
    val lazyStaggeredGridState = viewModel.getLazyStaggeredGridState(mode)
    val rankingList = viewModel.getIllustRankingFlow(mode).collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()
    val onRefresh = rankingList::refresh
    val isRefreshing = rankingList.loadState.refresh is LoadState.Loading
    val controller = remember {
        keyboardScrollerController(lazyStaggeredGridState) {
            lazyStaggeredGridState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(mode) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is RankingSideEffect.Refresh -> {
                    if (sideEffect.mode == mode) {
                        rankingList.refresh()
                    }
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
        val layoutParams = RecommendGridDefaults.coverLayoutParameters()
        LazyVerticalStaggeredGrid(
            state = lazyStaggeredGridState,
            contentPadding = PaddingValues(5.dp),
            columns = layoutParams.gridCells,
            verticalItemSpacing = layoutParams.verticalArrangement.spacing,
            horizontalArrangement = layoutParams.horizontalArrangement,
            modifier = Modifier.fillMaxSize()
        ) {
            illustGrid(
                illusts = rankingList,
                navToPictureScreen = navigateToPictureScreen
            )
        }
        VerticalScrollbar(
            state = lazyStaggeredGridState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun NovelMode(
    mode: RankingMode,
    navigateToNovelDetailScreen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = koinViewModel(),
) {
    val lazyListState = viewModel.getLazyListState(mode)
    val rankingList = viewModel.getNovelRankingFlow(mode).collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()
    val onRefresh = rankingList::refresh
    val isRefreshing = rankingList.loadState.refresh is LoadState.Loading
    val controller = remember {
        keyboardScrollerController(lazyListState) {
            lazyListState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(mode) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is RankingSideEffect.Refresh -> {
                    if (sideEffect.mode == mode) {
                        rankingList.refresh()
                    }
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
        ) {
            items(
                count = rankingList.itemCount,
                key = rankingList.itemIndexKey { index, item -> "${index}_${item.id}" }
            ) { index ->
                rankingList[index]?.let { novel ->
                    NovelItem(
                        novel = novel,
                        isBookmarked = novel.isBookmarked,
                        onNovelClick = { novelId ->
                            navigateToNovelDetailScreen(novelId)
                        },
                        onBookmarkClick = { restrict, tags ->
                            if (novel.isBookmarked) {
                                BookmarkState.deleteBookmarkNovel(novel.id)
                            } else {
                                BookmarkState.bookmarkNovel(novel.id, restrict, tags)
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
