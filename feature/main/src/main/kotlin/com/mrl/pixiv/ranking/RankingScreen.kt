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
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.asState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
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

    var showDatePicker by remember { mutableStateOf(false) }

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
            initialSelectedDateMillis = viewModel.getRankingDate(state.currentMode)
                ?.atTime(0, 0)
                ?.toInstant(TimeZone.UTC)?.toEpochMilliseconds(),
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
                            viewModel.changeDate(date)
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
                    viewModel.selectMode(mode)
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
                        val r18Enabled by requireUserPreferenceFlow.collectAsStateWithLifecycle { isR18Enabled }
                        LaunchedEffect(r18Enabled) {
                            if (!r18Enabled && state.showR18) {
                                viewModel.toggleR18()
                            }
                        }
                        if (r18Enabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = stringResource(RString.r18))
                                5.HSpacer
                                Switch(
                                    checked = state.showR18,
                                    onCheckedChange = { viewModel.toggleR18() }
                                )
                            }
                        }
                    }
                )
                Row {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage.coerceAtMost(
                            state.availableModes.lastIndex.coerceAtLeast(0)
                        ),
                        modifier = Modifier.weight(1f),
                        edgePadding = 0.dp
                    ) {
                        state.availableModes.forEachIndexed { index, mode ->
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
                state = pullRefreshState
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
