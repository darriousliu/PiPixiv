package com.mrl.pixiv.latest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.manga.MangaWatchlistContent
import kotlinx.coroutines.flow.SharedFlow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MangaWatchlistPage(
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: LatestViewModel = koinViewModel(),
) {
    val watchlist = viewModel.mangaWatchlist.collectAsLazyPagingItems()
    val listState = viewModel.watchlistMangaLazyListState
    val controller = remember(listState) {
        keyboardScrollerController(listState) {
            listState.layoutInfo.viewportSize.height.toFloat()
        }
    }

    KeyEventListener(controller)

    LaunchedEffect(refreshFlow, watchlist) {
        refreshFlow.collect { page ->
            if (page == LatestPage.MangaWatchlist) watchlist.refresh()
        }
    }

    MangaWatchlistContent(
        watchlist = watchlist,
        listState = listState,
        modifier = modifier,
    )
}
