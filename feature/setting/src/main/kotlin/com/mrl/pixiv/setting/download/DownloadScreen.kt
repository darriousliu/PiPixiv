package com.mrl.pixiv.setting.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
    viewModel: DownloadViewModel = koinViewModel(),
) {
    val state = viewModel.asState()
    val downloads by viewModel.currentDownloads.collectAsStateWithLifecycle()
    val tabs = remember {
        listOf(
            DownloadState.FILTER_ALL to RString.status_all,
            DownloadStatus.SUCCESS.value to RString.status_completed,
            DownloadStatus.FAILED.value to RString.status_failed,
            DownloadStatus.RUNNING.value to RString.status_running,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RString.download_manager)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val selectedIndex =
                tabs.indexOfFirst { it.first == state.filterStatus }.coerceAtLeast(0)
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, (status, titleRes) ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.changeFilterStatus(status) },
                        text = { Text(stringResource(titleRes)) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = downloads, key = { "${it.illustId}_${it.index}" }) { item ->
                    DownloadItem(
                        item = item,
                        onRetry = { viewModel.retryDownload(item) },
                        onDelete = { viewModel.deleteDownload(item) },
                        modifier = Modifier
                            .animateItem()
                            .throttleClick {
                                navigationManager.navigateToSinglePictureScreen(item.illustId)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItem(
    item: DownloadEntity,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = item.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (item.status == DownloadStatus.RUNNING.value ||
                    item.status == DownloadStatus.PENDING.value
                ) {
                    LinearWavyProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (icon, tint) = when (item.status) {
                            DownloadStatus.SUCCESS.value -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
                            DownloadStatus.FAILED.value -> Icons.Rounded.Error to MaterialTheme.colorScheme.error
                            else -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.onSurface
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                when (item.status) {
                                    DownloadStatus.SUCCESS.value -> RString.status_completed
                                    DownloadStatus.FAILED.value -> RString.status_failed
                                    else -> RString.status_running
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = tint
                        )
                    }
                }
            }
            Column {
                if (item.status == DownloadStatus.FAILED.value) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(RString.retry)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(RString.delete)
                    )
                }
            }
        }
    }
}
