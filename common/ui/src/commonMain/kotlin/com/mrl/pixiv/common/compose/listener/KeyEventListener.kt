package com.mrl.pixiv.common.compose.listener

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.mrl.pixiv.common.compose.LocalKeyEventFlow
import com.mrl.pixiv.common.compose.layout.LocalPaneKeyEventsEnabled
import kotlinx.coroutines.launch

@Composable
fun KeyEventListener(
    block: suspend (KeyEvent) -> Unit,
) {
    val flow = LocalKeyEventFlow.current
    val scope = rememberCoroutineScope()
    val updatedBlock by rememberUpdatedState(block)
    val acceptsEvents by rememberUpdatedState(LocalPaneKeyEventsEnabled.current)
    LaunchedEffect(Unit) {
        flow.collect {
            if (acceptsEvents) {
                scope.launch {
                    updatedBlock(it)
                }
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

/** 在导航宿主注册一次，将 Esc 交给当前启用的返回处理器。 */
@Composable
fun EscBackHandler() {
    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current).navigationEventDispatcher
    val input = remember(dispatcher) { DirectNavigationEventInput() }
    DisposableEffect(input) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
    KeyEventListener {
        if (it.key == Key.Escape && it.type == KeyEventType.KeyUp && input.hasEnabledBackHandlers) {
            input.backCompleted()
        }
    }
}
