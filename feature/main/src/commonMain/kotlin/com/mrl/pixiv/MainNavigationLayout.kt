package com.mrl.pixiv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.layout.LocalMainNavigationBottomInset
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.home
import com.mrl.pixiv.strings.my
import com.mrl.pixiv.strings.new_artworks
import com.mrl.pixiv.strings.ranking
import com.mrl.pixiv.strings.search
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

internal val mainNavigationItems = listOf(
    MainPage.Home to RStrings.home,
    MainPage.Ranking to RStrings.ranking,
    MainPage.Latest to RStrings.new_artworks,
    MainPage.Search to RStrings.search,
    MainPage.Profile to RStrings.my,
)

internal val NavigationSuiteType.isBottomNavigation: Boolean
    get() = this == NavigationSuiteType.NavigationBar ||
            this == NavigationSuiteType.ShortNavigationBarCompact ||
            this == NavigationSuiteType.ShortNavigationBarMedium

@Composable
internal fun MainNavigationLayout(
    selectedPage: MainPage,
    hasNewVersion: Boolean,
    layoutType: NavigationSuiteType,
    onSelectPage: (MainPage) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val floatingNavigation = layoutType.isBottomNavigation
    val backdrop = if (isRuntimeShaderSupported()) rememberLayerBackdrop() else null
    var navigationHeightPx by remember { mutableIntStateOf(0) }
    val bottomInset = with(LocalDensity.current) {
        if (floatingNavigation) navigationHeightPx.toDp() else 0.dp
    }

    // Keep the content in the same composition when switching between a bar and a rail.
    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = if (floatingNavigation) NavigationSuiteType.None else layoutType,
        navigationSuiteItems = {
            mainNavigationItems.forEach { (page, title) ->
                item(
                    selected = selectedPage == page,
                    onClick = { onSelectPage(page) },
                    icon = { MainNavigationIcon(page, hasNewVersion) },
                    label = { Text(stringResource(title), textAlign = TextAlign.Center) },
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalMainNavigationBottomInset provides bottomInset) {
                Box(
                    Modifier.fillMaxSize().then(
                        if (floatingNavigation && backdrop != null) {
                            Modifier.layerBackdrop(backdrop)
                        } else Modifier
                    )
                ) {
                    content()
                }
            }
            if (floatingNavigation) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { navigationHeightPx = it.height }
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                        .padding(horizontal = 12.dp),
                ) {
                    FloatingGlassNavigationBar(
                        selectedPage = selectedPage,
                        hasNewVersion = hasNewVersion,
                        backdrop = backdrop,
                        onSelectPage = onSelectPage,
                        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun MainNavigationIcon(
    page: MainPage,
    hasNewVersion: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    BadgedBox(
        modifier = modifier,
        badge = { if (page == MainPage.Profile && hasNewVersion) Badge() },
    ) {
        Icon(page.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
    }
}
