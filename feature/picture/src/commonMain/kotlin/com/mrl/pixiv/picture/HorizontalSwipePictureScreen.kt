package com.mrl.pixiv.picture

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.router.currentNavigationManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.compose.koinInject

@Composable
fun HorizontalSwipePictureScreen(
    illusts: ImmutableList<Illust>,
    index: Int,
    enableTransition: Boolean,
    modifier: Modifier = Modifier
) {
    val browsedIllusts = remember { mutableMapOf<Long, Illust>() }
    val navigationManager = currentNavigationManager()
    val browsingHistoryRepository = koinInject<BrowsingHistoryRepository>()
    val onBack: () -> Unit = {
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
    var selectedIllustId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pagerState.settledPage, illusts) {
        val id = illusts.getOrNull(pagerState.settledPage)?.id ?: return@LaunchedEffect
        if (selectedIllustId != null && selectedIllustId != id) {
            navigationManager.closeCurrentDetailBranch()
        }
        selectedIllustId = id
    }
    DisposableEffect(Unit) {
        onDispose {
            launchProcess(Dispatchers.IO) {
                browsingHistoryRepository.recordIllusts(browsedIllusts.values.toList())
            }
        }
    }
    PicturePager(
        illusts = illusts,
        modifier = modifier,
        state = pagerState,
    ) { illust ->
        PictureScreen(
            illust = illust,
            onBack = onBack,
            enableTransition = enableTransition,
        )
    }
}

@Composable
internal fun PicturePager(
    illusts: ImmutableList<Illust>,
    state: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable (Illust) -> Unit,
) {
    HorizontalPager(
        state = state,
        modifier = modifier,
        key = { page -> "$page-${illusts[page].id}" },
    ) { page ->
        val illust = illusts.getOrNull(page) ?: return@HorizontalPager
        // 每个作品拥有独立的 Store，页面移出组合时清理分页缓存和动图状态。
        val owner = rememberViewModelStoreOwner()
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            content(illust)
        }
    }
}
