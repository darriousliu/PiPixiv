package com.mrl.pixiv.common.compose.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BackToTopButton(
    visibility: Boolean,
    onAction: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    AnimatedContent(
        targetState = visibility,
        transitionSpec = {
            slideInVertically { it / 2 } + fadeIn() togetherWith
                    slideOutVertically { it / 2 } + fadeOut()
        },
    ) {
        if (it) {
            FloatingActionButton(
                modifier = modifier,
                onClick = {
                    scope.launch {
                        onAction()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = null
                )
            }
        } else {
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}