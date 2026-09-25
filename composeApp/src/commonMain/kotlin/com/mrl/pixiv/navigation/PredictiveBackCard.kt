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

/** Identity distinguishes successive gestures between the same two visits. */
internal class PredictiveBackCardTransition(
    private val initialKey: Any,
    private val targetKey: Any,
    val swipeEdge: Int,
) {
    fun contains(key: Any): Boolean = key == initialKey || key == targetKey

    fun matches(initial: Any, target: Any): Boolean =
        initial == initialKey && target == targetKey
}

@Composable
internal fun PredictiveBackCard(
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
    val scrim = transition.animateFloat(
        transitionSpec = {
            if (card != null) tween(300, easing = LinearEasing) else snap()
        },
        label = "predictiveBackCardScrim",
    ) { state ->
        if (card != null && state == EnterExitState.PreEnter) 0.18f else 0f
    }

    // Nav3 retains both scenes until their child animations settle. On commit
    // the outgoing scene is disposed; on cancellation the preview is disposed.
    DisposableEffect(card) {
        onDispose { card?.let(onFinished) }
    }

    val background = MaterialTheme.colorScheme.background
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
                    if (scrim.value > 0f) drawRect(Color.Black.copy(alpha = scrim.value))
                }
                .background(if (card != null) background else Color.Transparent),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}
