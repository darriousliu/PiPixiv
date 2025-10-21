package com.mrl.pixiv.common.compose.ui.bar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.animation.DefaultIntOffsetAnimationSpec
import com.mrl.pixiv.common.router.MainScreenPage

@Composable
fun HomeBottomBar(
    layoutType: NavigationSuiteType,
    currentPage: MainScreenPage,
    onSwitch: (MainScreenPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screens = listOf(
        MainScreenPage.HOME,
        MainScreenPage.LATEST,
        MainScreenPage.SEARCH,
        MainScreenPage.PROFILE,
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = if (layoutType == NavigationSuiteType.NavigationBar) {
            slideInVertically(DefaultIntOffsetAnimationSpec) { it }
        } else {
            slideInHorizontally(DefaultIntOffsetAnimationSpec) { if (isRtl) it else -it }
        },
        exit = if (layoutType == NavigationSuiteType.NavigationBar) {
            slideOutVertically(DefaultIntOffsetAnimationSpec) { it }
        } else {
            slideOutHorizontally(DefaultIntOffsetAnimationSpec) { if (isRtl) it else -it }
        },
    ) {
        NavigationSuite(
            layoutType = layoutType,
            colors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = Color.Transparent
            ),
            content = {
                screens.forEach { screen ->
                    item(
                        selected = currentPage == screen,
                        onClick = {
                            if (currentPage != screen) {
                                onSwitch(screen)
                            }
                        },
                        icon = screen.icon,
                        modifier = Modifier.requiredHeightIn(max = 56.dp),
                        label = {
                            Text(text = stringResource(screen.title))
                        }
                    )
                }
            }
        )
    }
}