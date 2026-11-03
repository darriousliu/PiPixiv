package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.content_empty_title
import com.mrl.pixiv.strings.content_error_description
import com.mrl.pixiv.strings.content_error_title
import com.mrl.pixiv.strings.content_loading
import com.mrl.pixiv.strings.content_loading_more
import com.mrl.pixiv.strings.retry
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContentLoadingState(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    message: String? = null,
) {
    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularWavyProgressIndicator(Modifier.size(24.dp))
            Text(
                message ?: stringResource(RStrings.content_loading_more),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        ContentStateLayout(modifier) {
            CircularWavyProgressIndicator(Modifier.size(40.dp))
            Text(
                message ?: stringResource(RStrings.content_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ContentEmptyState(
    message: String? = null,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = Icons.Rounded.Inbox,
    action: (@Composable () -> Unit)? = null,
) {
    ContentStateLayout(modifier) {
        ContentStateIcon(icon)
        Text(
            message ?: stringResource(RStrings.content_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        description?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}

@Composable
fun ContentErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    stringResource(RStrings.content_error_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
            }
        }
    } else {
        ContentStateLayout(modifier) {
            ContentStateIcon(Icons.Rounded.CloudOff)
            Text(
                stringResource(RStrings.content_error_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(RStrings.content_error_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                Text(stringResource(RStrings.retry), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ContentStateLayout(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 280.dp).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ContentStateIcon(icon: ImageVector) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(20.dp).size(32.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
