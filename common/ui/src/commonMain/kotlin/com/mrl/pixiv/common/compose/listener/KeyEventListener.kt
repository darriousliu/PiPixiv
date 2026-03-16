package com.mrl.pixiv.common.compose.listener

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.mrl.pixiv.common.compose.LocalKeyEventFlow
import kotlinx.coroutines.launch

@Composable
fun KeyEventListener(
    block: suspend (KeyEvent) -> Unit,
) {
    val flow = LocalKeyEventFlow.current
    val scope = rememberCoroutineScope()
    val updatedBlock by rememberUpdatedState(block)
    LaunchedEffect(Unit) {
        flow.collect {
            scope.launch {
                updatedBlock(it)
            }
        }
    }
}

fun keyboardScrollerController(
    scrollableState: ScrollableState,
    viewPortHeightFunc: () -> Float,
): suspend (KeyEvent) -> Unit {
    return block@{
        val viewPortHeight = viewPortHeightFunc()
        if (it.type != KeyEventType.KeyDown) return@block
        when (it.key) {
            Key.DirectionDown -> {
                scrollableState.animateScrollBy(viewPortHeight * 0.4f)
            }

            Key.DirectionUp -> {
                scrollableState.animateScrollBy(viewPortHeight * -0.4f)
            }

            Key.Spacebar, Key.PageDown -> {
                scrollableState.animateScrollBy(viewPortHeight)
            }

            Key.PageUp -> {
                scrollableState.animateScrollBy(-viewPortHeight)
            }
        }
    }
}

@Composable
fun EscBackHandler(
    onBack: () -> Unit,
) {
    val updatedOnBack by rememberUpdatedState(onBack)
    KeyEventListener {
        if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) {
            updatedOnBack()
        }
    }
}