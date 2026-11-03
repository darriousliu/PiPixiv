package com.mrl.pixiv.search.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.compose.ui.ContentErrorState
import com.mrl.pixiv.common.compose.ui.ContentLoadingState
import com.mrl.pixiv.common.repository.feed.PagedFeedState

@Composable
internal fun ManualSearchResultsStatus(
    state: PagedFeedState<*>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContent = state.items.isNotEmpty()
    if (hasContent && (state.error == null || state.isLoading)) return
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (hasContent) Alignment.BottomCenter else Alignment.Center,
    ) {
        when {
            !hasContent && state.isLoading -> ContentLoadingState()
            state.error != null -> ContentErrorState(
                onRetry = onRetry,
                compact = hasContent,
                modifier = if (hasContent) Modifier.widthIn(max = 480.dp)
                    .padding(16.dp) else Modifier,
            )

            !hasContent -> ContentEmptyState()
        }
    }
}
