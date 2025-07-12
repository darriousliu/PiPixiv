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
import com.mrl.pixiv.common.router.Destination

@Composable
fun HomeBottomBar(
    bottomBarVisibility: Boolean,
    layoutType: NavigationSuiteType,
    currentRoute: Destination,
    onSwitch: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screens = listOf(
        Destination.HomeScreen,
        Destination.LatestScreen,
        Destination.SearchPreviewScreen,
        Destination.ProfileScreen,
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        visible = bottomBarVisibility,
        modifier = modifier,
        enter = if (layoutType == NavigationSuiteType.NavigationBar) slideInVertically { it } else slideInHorizontally { if (isRtl) it else -it },
        exit = if (layoutType == NavigationSuiteType.NavigationBar) slideOutVertically { it } else slideOutHorizontally { if (isRtl) it else -it },
    ) {
        NavigationSuite(
            layoutType = layoutType,
            colors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = Color.Transparent
            ),
            content = {
                screens.forEach { screen ->
                    item(
                        selected = currentRoute == screen,
                        onClick = {
                            if (currentRoute != screen) {
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