package com.mrl.pixiv.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEvent
import com.mrl.pixiv.common.compose.LocalNavigationSharedContentEnabled

/** 通过实例身份区分同一对导航记录之间连续发生的返回手势。 */
internal class PredictiveBackCardTransition(
    private val initialKey: Any,
    private val targetKey: Any,
    val swipeEdge: Int,
) {
    fun contains(key: Any): Boolean = key == initialKey || key == targetKey

    fun isTarget(key: Any): Boolean = key == targetKey

    fun matches(initial: Any, target: Any): Boolean =
        initial == initialKey && target == targetKey
}

@Composable
internal fun PredictiveBackCard(
    sceneKey: Any,
    card: PredictiveBackCardTransition?,
    onFinished: (PredictiveBackCardTransition) -> Unit,
    content: @Composable () -> Unit,
) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val exitProgress = transition.animateFloat(
        transitionSpec = {
            if (card != null) tween(300, easing = LinearEasing) else snap()
        },
        label = "predictiveBackCardExit",
    ) { state ->
        if (card != null && state == EnterExitState.PostExit) 1f else 0f
    }
    // 按住手势到 100% 时，目标场景已经进入 Visible 状态。
    // 因此通过场景标识识别返回目的地，让蒙层一直保留到手势完成或取消。
    val showDestinationScrim = card?.isTarget(sceneKey) == true

    // Nav3 会保留两个场景，直到子动画结束。
    // 确认返回后移除离开的场景，取消返回后移除预览场景，此时清理手势状态。
    DisposableEffect(card) {
        onDispose { card?.let(onFinished) }
    }

    val background = MaterialTheme.colorScheme.background
    // 纯黑蒙层无法区分两个黑色页面。使用主题对比色压暗浅色目的地，
    // 并略微提亮深色目的地，保持前景卡片边界清晰。
    val destinationScrim = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
    CompositionLocalProvider(LocalNavigationSharedContentEnabled provides (card == null)) {
        Box(
            modifier = Modifier.fillMaxSize()
                .graphicsLayer {
                    val p = exitProgress.value
                    scaleX = 1f - 0.1f * p
                    scaleY = scaleX
                    val direction = when (card?.swipeEdge) {
                        NavigationEvent.EDGE_LEFT -> 1f
                        NavigationEvent.EDGE_RIGHT -> -1f
                        else -> 0f
                    }
                    translationX = direction * 12.dp.toPx() * p
                    shape = RoundedCornerShape(32.dp * p)
                    clip = p > 0f
                }
                .drawWithContent {
                    drawContent()
                    if (showDestinationScrim) drawRect(destinationScrim)
                }
                .background(if (card != null) background else Color.Transparent),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}
