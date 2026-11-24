package com.mrl.pixiv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import com.mrl.pixiv.common.compose.layout.LocalPaneKeyEventsEnabled
import com.mrl.pixiv.common.router.MainPage

/**
 * 统一承载主页面内容，在紧凑横向滑页和大屏淡入淡出之间复用同一个组合宿主与保存状态。
 *
 * 这里不再使用 HorizontalPager + movableContentOf：HorizontalPager 底层通过 SubcomposeLayout
 * 复用失活槽位，跨页切换时可能在同一测量阶段将已失活槽位中的 movableContent 重新挂载到新页，
 * 触发 ApplyOnDeactivatedNodeAssertion 崩溃。
 */
@Composable
internal fun MainPageContent(
    selectedPage: MainPage,
    usePager: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (MainPage) -> Unit,
) {
    val pageStateHolder = rememberSaveableStateHolder()
    val currentContent by rememberUpdatedState(content)
    var deferredPage by remember { mutableStateOf(selectedPage) }

    LaunchedEffect(selectedPage, usePager) {
        if (!usePager || deferredPage == selectedPage) {
            deferredPage = selectedPage
            return@LaunchedEffect
        }
        // 让底部导航栏先呈现首帧过渡，再组合冷启动目标页。
        withFrameNanos { }
        deferredPage = selectedPage
    }

    val displayedPage = if (usePager) deferredPage else selectedPage

    AnimatedContent(
        targetState = displayedPage,
        modifier = modifier.fillMaxSize().clipToBounds(),
        transitionSpec = {
            val transform = if (usePager) {
                val initialIndex = mainNavigationItems.indexOfFirst { it.first == initialState }
                val targetIndex = mainNavigationItems.indexOfFirst { it.first == targetState }
                val direction = if (targetIndex >= initialIndex) {
                    AnimatedContentTransitionScope.SlideDirection.Start
                } else {
                    AnimatedContentTransitionScope.SlideDirection.End
                }
                slideIntoContainer(
                    towards = direction,
                    animationSpec = tween(300),
                ).togetherWith(
                    slideOutOfContainer(
                        towards = direction,
                        animationSpec = tween(300),
                    )
                )
            } else {
                fadeIn(animationSpec = tween(220, delayMillis = 90))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            }
            transform.using(null)
        },
        label = "main_page_content",
    ) { page ->
        val index = mainNavigationItems.indexOfFirst { it.first == page }
        val active by remember(transition, selectedPage, page) {
            computedStateOf {
                page == selectedPage &&
                    transition.currentState == EnterExitState.Visible &&
                    transition.targetState == EnterExitState.Visible
            }
        }
        val lifecycleOwner = rememberLifecycleOwner(
            maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED,
        )
        CompositionLocalProvider(
            LocalLifecycleOwner provides lifecycleOwner,
            LocalPaneKeyEventsEnabled provides (LocalPaneKeyEventsEnabled.current && active),
        ) {
            pageStateHolder.SaveableStateProvider(index) {
                currentContent(page)
            }
        }
    }
}
