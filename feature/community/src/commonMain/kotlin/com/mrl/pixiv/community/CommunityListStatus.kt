package com.mrl.pixiv.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator()
            is LoadState.Error -> {
                // Avoid exposing server headers, response bodies or account details in error UI.
                Text(stringResource(RStrings.community_load_failed), textAlign = TextAlign.Center)
                TextButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
            }
            is LoadState.NotLoading -> emptyMessage?.let {
                Text(it, textAlign = TextAlign.Center)
            }
        }
    }
}
