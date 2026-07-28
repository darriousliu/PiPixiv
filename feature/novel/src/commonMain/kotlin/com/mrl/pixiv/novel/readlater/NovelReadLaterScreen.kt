package com.mrl.pixiv.novel.readlater

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.repository.NovelReadLaterItem
import com.mrl.pixiv.common.repository.NovelReadLaterRepository
import com.mrl.pixiv.common.repository.NovelReadLaterState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.strings.ai_translation_config_required
import com.mrl.pixiv.strings.ai_translation_failed
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.delete
import com.mrl.pixiv.strings.read_later
import com.mrl.pixiv.strings.read_later_empty
import com.mrl.pixiv.strings.read_later_status_failed
import com.mrl.pixiv.strings.read_later_status_pending
import com.mrl.pixiv.strings.read_later_status_ready
import com.mrl.pixiv.strings.read_later_status_running
import com.mrl.pixiv.strings.retry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun NovelReadLaterScreen(
    modifier: Modifier = Modifier,
    repository: NovelReadLaterRepository = koinInject(),
    navigationManager: NavigationManager = koinInject(),
) {
    val items by repository.observeItems()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.read_later)) },
                navigationIcon = {
                    IconButton(onClick = navigationManager::popBackStack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(RStrings.back),
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(RStrings.read_later_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                items(
                    items = items,
                    key = { "${it.novelId}_${it.targetLanguage}" },
                ) { item ->
                    NovelReadLaterItem(
                        item = item,
                        onOpen = {
                            navigationManager.navigateToNovelDetailScreen(
                                novelId = item.novelId,
                                readLaterTargetLanguage = item.targetLanguage,
                            )
                        },
                        onRetry = {
                            scope.launch {
                                try {
                                    repository.retry(
                                        novelId = item.novelId,
                                        targetLanguage = item.targetLanguage,
                                    )
                                } catch (cancellation: CancellationException) {
                                    throw cancellation
                                } catch (_: IllegalArgumentException) {
                                    ToastUtil.safeShortToast(
                                        RStrings.ai_translation_config_required
                                    )
                                } catch (throwable: Throwable) {
                                    ToastUtil.safeShortToast(
                                        RStrings.ai_translation_failed,
                                        throwable.message.orEmpty(),
                                    )
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                repository.remove(
                                    novelId = item.novelId,
                                    targetLanguage = item.targetLanguage,
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NovelReadLaterItem(
    item: NovelReadLaterItem,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.novelTitle,
                modifier = Modifier.size(width = 72.dp, height = 96.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.novelTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.novelAuthorName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.novelCaption.isNotBlank()) {
                    Text(
                        text = item.novelCaption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${item.targetLanguage} · ${item.provider.name} · ${item.model}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(item.state.stringResource()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.state == NovelReadLaterState.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                item.lastError?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column {
                if (item.state == NovelReadLaterState.FAILED) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(RStrings.retry),
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(RStrings.delete),
                    )
                }
            }
        }
    }
}

private fun NovelReadLaterState.stringResource(): StringResource = when (this) {
    NovelReadLaterState.PENDING -> RStrings.read_later_status_pending
    NovelReadLaterState.RUNNING -> RStrings.read_later_status_running
    NovelReadLaterState.READY -> RStrings.read_later_status_ready
    NovelReadLaterState.FAILED -> RStrings.read_later_status_failed
}
