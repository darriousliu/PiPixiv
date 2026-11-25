package com.mrl.pixiv.common.compose.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.listener.KeyEventListener
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back_to_top
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackToTopButton(
    visibility: Boolean,
    onBackToTop: suspend () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    AnimatedContent(
        targetState = visibility,
        modifier = modifier,
        transitionSpec = {
            slideInVertically { it / 2 } + fadeIn() togetherWith
                    slideOutVertically { it / 2 } + fadeOut()
        },
    ) {
        if (it) {
            KeyEventListener {
                if (it.type != KeyEventType.KeyUp) return@KeyEventListener
                if (it.key == Key.R) {
                    onBackToTop()
                }
            }
            val shape = FloatingActionButtonDefaults.shape
            FilledTonalIconButton(
                modifier = Modifier.size(48.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
                shape = shape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                onClick = {
                    scope.launch {
                        onBackToTop()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(RStrings.back_to_top),
                )
            }
        } else {
            KeyEventListener {
                if (it.type != KeyEventType.KeyUp) return@KeyEventListener
                if (it.key == Key.R) {
                    onRefresh()
                }
            }
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}
