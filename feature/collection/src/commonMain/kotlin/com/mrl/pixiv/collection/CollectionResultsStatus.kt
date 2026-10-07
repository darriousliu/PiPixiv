package com.mrl.pixiv.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.discovery_load_failed
import com.mrl.pixiv.strings.discovery_no_results
import com.mrl.pixiv.strings.discovery_retry
import org.jetbrains.compose.resources.stringResource

/** Shared by the profile collection and Latest collection grids, including advanced results. */
@Composable
fun CollectionResultsStatus(
    loadStates: CombinedLoadStates,
    itemCount: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val failed = loadStates.refresh is LoadState.Error || loadStates.append is LoadState.Error
    val empty = itemCount == 0 && loadStates.refresh is LoadState.NotLoading
    if (failed || empty) {
        Box(modifier.fillMaxSize(), contentAlignment = if (itemCount == 0) Alignment.Center else Alignment.BottomCenter) {
            Surface(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(if (failed) RStrings.discovery_load_failed else RStrings.discovery_no_results))
                    if (failed) TextButton(onClick = onRetry) { Text(stringResource(RStrings.discovery_retry)) }
                }
            }
        }
    }
}
