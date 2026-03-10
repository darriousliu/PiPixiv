package com.mrl.pixiv.common.compose.listener

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.KeyEvent
import com.mrl.pixiv.common.compose.LocalKeyEventFlow
import kotlinx.coroutines.launch

@Composable
fun KeyEventListener(
    block: suspend (KeyEvent) -> Unit,
) {
    val flow = LocalKeyEventFlow.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(
        Unit
    ) {
        flow.collect {
            scope.launch {
                block(it)
            }
        }
    }
}