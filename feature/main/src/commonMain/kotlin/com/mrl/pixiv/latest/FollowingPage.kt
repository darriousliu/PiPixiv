package com.mrl.pixiv.latest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.compose.listener.keyboardScrollerController
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.follow.FollowingScreenBody
import com.mrl.pixiv.follow.FollowingViewModel
import com.mrl.pixiv.strings.word_private
import com.mrl.pixiv.strings.word_public
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.mrl.pixiv.follow.FollowingPage as FollowingTab

@Composable
fun FollowingPage(
    uid: Long,
    refreshFlow: SharedFlow<LatestPage>,
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = koinViewModel { parametersOf(uid) },
) {
    val navigationManager = currentNavigationManager()
    val paneSizeClass = currentPaneLayoutInfo().sizeClass
    val isWidthAtLeastMedium = paneSizeClass.isWidthAtLeastMedium
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }
    var selectedPage by rememberSaveable(uid) { mutableIntStateOf(0) }
    val pageIndex = selectedPage.coerceIn(viewModel.pages.indices)
    val followingUsersByPage = viewModel.pages.map { page ->
        key(page) {
            val source = when (page) {
                FollowingTab.Public -> viewModel.publicFollowingPageSource
                FollowingTab.Private -> viewModel.privateFollowingPageSource
            }
            source.collectAsLazyPagingItems()
        }
    }
    val followingUsers = followingUsersByPage[pageIndex]
    val listState = viewModel.lazyListState[pageIndex]
    val gridState = viewModel.lazyGridState[pageIndex]
    val controller = remember(isWidthAtLeastMedium, pageIndex) {
        if (isWidthAtLeastMedium) {
            keyboardScrollerController(gridState) {
                gridState.layoutInfo.viewportSize.height.toFloat()
            }
        } else {
            keyboardScrollerController(listState) {
                listState.layoutInfo.viewportSize.height.toFloat()
            }
        }
    }

    KeyEventListener(controller)
    LaunchedEffect(refreshFlow, followingUsers) {
        refreshFlow.collect {
            followingUsers.refresh()
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        if (viewModel.pages.size > 1) {
            PrimaryTabRow(selectedTabIndex = pageIndex) {
                viewModel.pages.forEachIndexed { index, _ ->
                    Tab(
                        selected = pageIndex == index,
                        onClick = { selectedPage = index },
                        text = { Text(stringResource(if (index == 0) RStrings.word_public else RStrings.word_private)) },
                    )
                }
            }
        }
        FollowingScreenBody(
            followingUsers = followingUsers,
            navToPictureScreen = navigationManager::navigateToPictureScreen,
            navToUserProfile = navigationManager::navigateToProfileDetailScreen,
            modifier = Modifier.weight(1f),
            lazyListState = listState,
            lazyGridState = gridState,
            showIllusts = appViewMode == AppViewMode.ILLUST,
        )
    }
}
