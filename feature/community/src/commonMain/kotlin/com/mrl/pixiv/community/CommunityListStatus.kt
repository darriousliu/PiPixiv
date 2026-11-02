package com.mrl.pixiv.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.community_load_failed
import com.mrl.pixiv.strings.retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CommunityListStatus(
    state: LoadState,
    onRetry: () -> Unit,
    emptyMessage: String? = null,
) {
    if (state is LoadState.NotLoading && emptyMessage == null) return
    Column(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = if (emptyMessage != null) 240.dp else 0.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator(Modifier.size(32.dp))
            is LoadState.Error -> {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        null,
                        Modifier.padding(16.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                // Avoid exposing server headers, response bodies or account details in error UI.
                Text(stringResource(RStrings.community_load_failed), textAlign = TextAlign.Center)
                TextButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
            }

            is LoadState.NotLoading -> emptyMessage?.let {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Icon(
                        Icons.Rounded.Inbox,
                        null,
                        Modifier.padding(16.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    it,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
