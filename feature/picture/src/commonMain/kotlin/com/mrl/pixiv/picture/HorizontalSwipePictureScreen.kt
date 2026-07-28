package com.mrl.pixiv.picture

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.router.NavigationManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.compose.koinInject

@Composable
fun HorizontalSwipePictureScreen(
    illusts: ImmutableList<Illust>,
    index: Int,
    prefix: String,
    enableTransition: Boolean,
    modifier: Modifier = Modifier
) {
    val browsedIllusts = remember { mutableMapOf<Long, Illust>() }
    val navigationManager = koinInject<NavigationManager>()
    val browsingHistoryRepository = koinInject<BrowsingHistoryRepository>()
    val onBack: () -> Unit = {
        IllustCacheRepo.removeList(prefix)
        navigationManager.popBackStack()
    }
    if (illusts.isEmpty()) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }
    val safeIndex = index.coerceIn(0, illusts.lastIndex)
    val pagerState = rememberPagerState(safeIndex) { illusts.size }
    LaunchedEffect(pagerState.currentPage, illusts) {
        val current = illusts.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        browsedIllusts[current.id] = current
    }
    DisposableEffect(Unit) {
        onDispose {
            launchProcess(Dispatchers.IO) {
                browsingHistoryRepository.recordIllusts(browsedIllusts.values.toList())
            }
        }
    }
    NavigationEventHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = onBack
    )
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
    ) {
        val illust = illusts.getOrNull(it) ?: return@HorizontalPager
        PictureScreen(
            illust = illust,
            onBack = onBack,
            enableTransition = enableTransition,
        )
    }
}
