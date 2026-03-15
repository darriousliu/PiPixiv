package com.mrl.pixiv.latest

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.follow.FollowingScreenBody
import com.mrl.pixiv.follow.FollowingViewModel
import kotlinx.coroutines.flow.SharedFlow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FollowingPage(
    uid: Long,
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = koinViewModel { parametersOf(uid) },
    latestViewModel: LatestViewModel = koinViewModel()
) {
    val navigationManager = koinInject<NavigationManager>()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val isWidthAtLeastMedium = windowAdaptiveInfo.isWidthAtLeastMedium
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }
    val followingUsers = viewModel.publicFollowingPageSource.collectAsLazyPagingItems()
    val controller = remember(isWidthAtLeastMedium) {
        if (isWidthAtLeastMedium) {
            keyboardScrollerController(latestViewModel.followingLazyGirdState) {
                latestViewModel.followingLazyGirdState.layoutInfo.viewportSize.height.toFloat()
            }
        } else {
            keyboardScrollerController(latestViewModel.followingLazyListState) {
                latestViewModel.followingLazyListState.layoutInfo.viewportSize.height.toFloat()
            }
        }
    }

    KeyEventListener(controller)
    LaunchedEffect(Unit) {
        refreshFlow.collect {
            followingUsers.refresh()
        }
    }
    FollowingScreenBody(
        followingUsers = followingUsers,
        navToPictureScreen = navigationManager::navigateToPictureScreen,
        navToUserProfile = navigationManager::navigateToProfileDetailScreen,
        modifier = modifier,
        lazyListState = latestViewModel.followingLazyListState,
        lazyGridState = latestViewModel.followingLazyGirdState,
        showIllusts = appViewMode == AppViewMode.ILLUST,
    )
}