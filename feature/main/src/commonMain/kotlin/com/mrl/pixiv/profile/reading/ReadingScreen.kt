package com.mrl.pixiv.profile.reading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.data.setting.ReadingTab
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.manga.MangaWatchlistContent
import com.mrl.pixiv.manga.MangaWatchlistViewModel
import com.mrl.pixiv.novel.readlater.NovelReadLaterPage
import com.mrl.pixiv.profile.marker.NovelMarkersPage
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.novel_markers
import com.mrl.pixiv.strings.read_later
import com.mrl.pixiv.strings.reading_manga_watchlist
import com.mrl.pixiv.strings.reading_title
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReadingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val stateHolder = rememberSaveableStateHolder()
    ReadingContent(
        initialTab = requireUserPreferenceValue.readingTab,
        onSelectTab = { tab ->
            navigationManager.closeCurrentDetailBranch()
            SettingRepository.updateSettings { copy(readingTab = tab) }
        },
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    ) { tab ->
        stateHolder.SaveableStateProvider(tab.name) {
            when (tab) {
                ReadingTab.READ_LATER -> NovelReadLaterPage(navigationManager = navigationManager)
                ReadingTab.NOVEL_MARKERS -> NovelMarkersPage(navigationManager = navigationManager)
                ReadingTab.MANGA_WATCHLIST -> {
                    val viewModel = koinViewModel<MangaWatchlistViewModel>()
                    val watchlist = viewModel.series.collectAsLazyPagingItems()
                    MangaWatchlistContent(watchlist, navigationManager = navigationManager)
                }
            }
        }
    }
}

@Composable
internal fun ReadingContent(
    initialTab: ReadingTab,
    onSelectTab: (ReadingTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ReadingTab) -> Unit,
) {
    val pagerState =
        rememberPagerState(initialPage = initialTab.ordinal) { ReadingTab.entries.size }
    val scope = rememberCoroutineScope()
    val currentOnSelectTab by rememberUpdatedState(onSelectTab)
    LaunchedEffect(pagerState) {
        // Restoring a page must keep its existing detail; only a completed user change persists.
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            currentOnSelectTab(ReadingTab.entries[page])
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.reading_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(RStrings.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    edgePadding = 16.dp,
                    minTabWidth = 72.dp,
                ) {
                    ReadingTab.entries.forEach { tab ->
                        Tab(
                            selected = pagerState.currentPage == tab.ordinal,
                            onClick = { scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                            text = {
                                Text(
                                    text = stringResource(
                                        when (tab) {
                                            ReadingTab.READ_LATER -> RStrings.read_later
                                            ReadingTab.NOVEL_MARKERS -> RStrings.novel_markers
                                            ReadingTab.MANGA_WATCHLIST -> RStrings.reading_manga_watchlist
                                        }
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { ReadingTab.entries[it].name },
            ) { page ->
                content(ReadingTab.entries[page])
            }
        }
    }
}
