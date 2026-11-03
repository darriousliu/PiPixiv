package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mrl.pixiv.common.util.isAndroid
import com.mrl.pixiv.common.util.platform

// 此阈值仅用于决定完成方式，不得为判断手势类型而延迟绘制进度。
private const val QuickBackDurationMillis = 120L

/** 将快速返回的完成方式与 Nav3 可随手势调整进度的预览动画分开处理。 */
@Composable
internal fun <T : Any> PredictiveBackNavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    entryDecorators: List<NavEntryDecorator<T>> = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    sceneStrategies: List<SceneStrategy<T>>,
    sharedTransitionScope: SharedTransitionScope,
    enableQuickBack: Boolean = platform.isAndroid(),
    entryProvider: (T) -> NavEntry<T>,
) {
    require(backStack.isNotEmpty()) { "Navigation back stack cannot be empty" }
    // 导航条目及场景状态不随下方动画容器的标识重建。
    // SceneState 持有可移动内容，使平板上仍可见的来源页面保留 remember 状态。
    val entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = sceneStrategies,
        sharedTransitionScope = sharedTransitionScope,
        onBack = onBack,
    )
    val scene = sceneState.currentScene
    val currentInfo = SceneInfo(scene)
    val backInfo = sceneState.previousScenes.map { SceneInfo(it) }
    val gestureState = rememberNavigationEventState(currentInfo, backInfo)
    var isLongGesture by remember { mutableStateOf(false) }
    var gestureSequence by remember { mutableIntStateOf(0) }
    var displaySequence by remember { mutableIntStateOf(0) }
    // 计时器只关心 Idle 与 InProgress 之间的切换。每次进度更新由 NavDisplay 自行观察，
    // 避免管理导航条目与历史记录的这一层随每个进度事件重组。
    val inProgress by remember(gestureState) {
        derivedStateOf { gestureState.transitionState is InProgress }
    }
    val observedSequence = gestureSequence

    LaunchedEffect(inProgress, observedSequence, enableQuickBack) {
        if (inProgress && enableQuickBack) {
            val startedAt = withFrameMillis { it }
            do {
                val elapsed = withFrameMillis { it } - startedAt
            } while (elapsed < QuickBackDurationMillis)
            if (gestureSequence == observedSequence && gestureState.transitionState is InProgress) {
                isLongGesture = true
            }
        }
    }

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCancelled = {
            isLongGesture = false
            // 即使在同一帧内取消并重试，也要使上一次手势的分类计时器失效。
            gestureSequence++
        },
        onBackCompleted = {
            val popCount = (entries.size - scene.previousEntries.size).coerceAtLeast(0)
            if (enableQuickBack && !isLongGesture && popCount > 0) {
                // 仅重建动画容器，直接显示确认返回后的场景。
                // 只修改返回动画参数仍会等待子动画及共享条目动画结束。
                displaySequence++
            }
            isLongGesture = false
            gestureSequence++
            repeat(popCount) { onBack() }
        },
    )

    key(displaySequence) {
        NavDisplay(
            sceneState = sceneState,
            // 从第一个进度事件开始跟随手势；若在此等待，页面会先停顿，
            // 随后突然跳到手指已经到达的位置。
            navigationEventState = gestureState,
            modifier = modifier,
        )
    }
}
