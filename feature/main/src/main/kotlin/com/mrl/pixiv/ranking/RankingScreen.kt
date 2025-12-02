package com.mrl.pixiv.ranking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.RecommendGridDefaults
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.asState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RankingScreen(
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val scope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ISO_DATE)
                            viewModel.dispatch(RankingAction.ChangeDate(date))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(RString.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(RString.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { state.availableModes.size }
    )

    // Sync external mode selection (if any) with pager
    LaunchedEffect(state.currentMode) {
        val index = state.availableModes.indexOf(state.currentMode)
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.scrollToPage(index)
        }
    }

    // Sync pager scroll with mode selection
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (page in state.availableModes.indices) {
                val mode = state.availableModes[page]
                if (state.currentMode != mode) {
                    viewModel.dispatch(RankingAction.SelectMode(mode))
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = stringResource(RString.ranking)) },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(RString.r18))
                            Switch(
                                checked = state.showR18,
                                onCheckedChange = { viewModel.dispatch(RankingAction.ToggleR18) }
                            )
                        }
                    }
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 0.dp
                ) {
                    state.availableModes.forEachIndexed { index, mode ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.scrollToPage(index)
                                }
                                if (mode == RankingMode.PAST) {
                                    showDatePicker = true
                                }
                            },
                            text = {
                                if (mode == RankingMode.PAST && state.date != null) {
                                    Text(text = state.date)
                                } else {
                                    Text(text = stringResource(mode.title))
                                }
                            }
                        )
                    }
                }
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
            val mode = state.availableModes.getOrNull(page) ?: return@HorizontalPager
            val rankingList = viewModel.getRankingFlow(mode).collectAsLazyPagingItems()
            val lazyStaggeredGridState = viewModel.getLazyStaggeredGridState(mode)
            val pullRefreshState = rememberPullToRefreshState()
            val onRefresh = rankingList::refresh
            val isRefreshing = rankingList.loadState.refresh is LoadState.Loading

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
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
                        navToPictureScreen = navigationManager::navigateToPictureScreen
                    )
                }
            }
        }
    }
}
