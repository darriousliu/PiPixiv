package com.mrl.pixiv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import com.mrl.pixiv.common.compose.layout.LocalPaneKeyEventsEnabled
import com.mrl.pixiv.common.router.MainPage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Shares page state across the compact pager and the existing wide-screen navigation host. */
@Composable
internal fun MainPageContent(
    selectedPage: MainPage,
    usePager: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (MainPage) -> Unit,
) {
    val pageStateHolder = rememberSaveableStateHolder()
    val currentContent by rememberUpdatedState(content)
    // A page keeps the same composition identity when resizing between pager and rail hosts.
    val pages = remember(pageStateHolder) {
        mainNavigationItems.mapIndexed { index, (page, _) ->
            movableContentOf {
                pageStateHolder.SaveableStateProvider(index) { currentContent(page) }
            }
        }
    }
    val pageContent: @Composable (MainPage) -> Unit = { page ->
        pages[mainNavigationItems.indexOfFirst { it.first == page }]()
    }
    if (usePager) {
        MainPagePager(selectedPage, modifier, pageContent)
    } else {
        AnimatedContent(
            targetState = selectedPage,
            modifier = modifier,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, delayMillis = 90))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            content = { pageContent(it) },
        )
    }
}

@Composable
private fun MainPagePager(
    selectedPage: MainPage,
    modifier: Modifier = Modifier,
    content: @Composable (MainPage) -> Unit,
) {
    val selectedIndex = mainNavigationItems.indexOfFirst { it.first == selectedPage }
    val pagerState = rememberPagerState(initialPage = selectedIndex) { mainNavigationItems.size }
    var preloadNeighbors by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex, pagerState) {
        // Let the dock present its first animation frame before composing a cold destination.
        withFrameNanos { }
        pagerState.animateScrollToPage(selectedIndex, animationSpec = tween(300))
        // Warm at most one page on either side after the initial navigation has settled.
        // Keep that budget during later transitions instead of disposing warmed neighbours.
        if (!preloadNeighbors) {
            delay(500.milliseconds)
            preloadNeighbors = true
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        key = { it },
        beyondViewportPageCount = if (preloadNeighbors) 1 else 0,
        // Home, Ranking and Latest already own horizontal gestures for their child tabs.
        userScrollEnabled = false,
    ) { index ->
        val active by remember(pagerState, selectedIndex, index) {
            derivedStateOf {
                index == selectedIndex && index == pagerState.settledPage &&
                        !pagerState.isScrollInProgress
            }
        }
        val lifecycleOwner = rememberLifecycleOwner(
            maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED,
        )
        CompositionLocalProvider(
            LocalLifecycleOwner provides lifecycleOwner,
            LocalPaneKeyEventsEnabled provides (LocalPaneKeyEventsEnabled.current && active),
        ) {
            content(mainNavigationItems[index].first)
        }
    }
}
