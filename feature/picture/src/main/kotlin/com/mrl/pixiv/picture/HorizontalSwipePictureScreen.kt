package com.mrl.pixiv.picture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.router.NavigationManager
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject

@Composable
fun HorizontalSwipePictureScreen(
    illusts: ImmutableList<Illust>,
    index: Int,
    prefix: String,
    enableTransition: Boolean,
    modifier: Modifier = Modifier
) {
    val browsedIllusts = remember { mutableSetOf<Long>() }
    val pagerState = rememberPagerState(index) { illusts.size }
    val navigationManager = koinInject<NavigationManager>()
    val onBack: () -> Unit = {
        IllustCacheRepo.removeList(prefix)
        navigationManager.popBackStack()
    }
    LaunchedEffect(pagerState.currentPage) {
        browsedIllusts.add(illusts[pagerState.currentPage].id)
    }
    DisposableEffect(Unit) {
        onDispose {
            launchProcess {
                PixivRepository.addIllustBrowsingHistory(browsedIllusts.toList())
            }
        }
    }
    BackHandler(onBack = onBack)
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
    ) {
        PictureScreen(
            illust = illusts[it],
            onBack = onBack,
            enableTransition = enableTransition,
        )
    }
}