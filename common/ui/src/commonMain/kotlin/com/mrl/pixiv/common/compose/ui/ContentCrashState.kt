package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.page_unavailable_message
import com.mrl.pixiv.strings.page_unavailable_title
import org.jetbrains.compose.resources.stringResource

/** A recoverable page failure with user-facing text and a caller-owned recovery action. */
@Composable
fun ContentCrashState(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(RStrings.page_unavailable_message),
    actionText: String = stringResource(RStrings.back),
    title: String = stringResource(RStrings.page_unavailable_title),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.Center,
            ) {
                ContentErrorState(
                    onRetry = onAction,
                    message = title,
                    description = message,
                    icon = Icons.Rounded.ErrorOutline,
                    action = {
                        FilledTonalButton(onClick = onAction) {
                            Text(actionText, textAlign = TextAlign.Center)
                        }
                    },
                )
            }
        }
    }
}
