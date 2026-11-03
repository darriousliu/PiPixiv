package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

internal enum class PagingStatus { None, Loading, Empty, Error }

internal data class PagingPresentation(
    val page: PagingStatus = PagingStatus.None,
    val refreshError: Boolean = false,
    val append: PagingStatus = PagingStatus.None,
)

internal fun pagingPresentation(states: CombinedLoadStates, itemCount: Int): PagingPresentation {
    if (itemCount == 0) {
        val status = when {
            states.refresh is LoadState.Loading -> PagingStatus.Loading
            states.refresh is LoadState.Error -> PagingStatus.Error
            states.append is LoadState.Loading -> PagingStatus.Loading
            states.append is LoadState.Error -> PagingStatus.Error
            else -> PagingStatus.Empty
        }
        return PagingPresentation(page = status)
    }
    return when (states.refresh) {
        is LoadState.Loading -> PagingPresentation()
        is LoadState.Error -> PagingPresentation(refreshError = true)
        is LoadState.NotLoading -> PagingPresentation(
            append = when (states.append) {
                is LoadState.Loading -> PagingStatus.Loading
                is LoadState.Error -> PagingStatus.Error
                is LoadState.NotLoading -> PagingStatus.None
            },
        )
    }
}

@Composable
fun PagingContentState(
    loadStates: CombinedLoadStates,
    itemCount: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
) {
    val presentation = pagingPresentation(loadStates, itemCount)
    if (presentation.page == PagingStatus.None && !presentation.refreshError) return
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (presentation.refreshError) Alignment.BottomCenter else Alignment.Center,
    ) {
        when (presentation.page) {
            PagingStatus.Loading -> ContentLoadingState()
            PagingStatus.Empty -> ContentEmptyState(message = emptyMessage)
            PagingStatus.Error -> ContentErrorState(onRetry = onRetry)
            PagingStatus.None -> ContentErrorState(
                onRetry = onRetry,
                modifier = Modifier.widthIn(max = 480.dp).padding(16.dp),
                compact = true,
            )
        }
    }
}

@Composable
fun PagingAppendState(
    loadStates: CombinedLoadStates,
    itemCount: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (pagingPresentation(loadStates, itemCount).append) {
        PagingStatus.Loading -> ContentLoadingState(modifier = modifier, compact = true)
        PagingStatus.Error -> ContentErrorState(
            onRetry = onRetry,
            modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            compact = true,
        )

        else -> Unit
    }
}
