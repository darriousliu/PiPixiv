package com.mrl.pixiv.collection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.CombinedLoadStates
import com.mrl.pixiv.common.compose.ui.PagingContentState
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.discovery_no_results
import org.jetbrains.compose.resources.stringResource

/** Shared by the profile collection and Latest collection grids, including advanced results. */
@Composable
fun CollectionResultsStatus(
    loadStates: CombinedLoadStates,
    itemCount: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PagingContentState(
        loadStates = loadStates,
        itemCount = itemCount,
        onRetry = onRetry,
        modifier = modifier,
        emptyMessage = stringResource(RStrings.discovery_no_results),
    )
}
