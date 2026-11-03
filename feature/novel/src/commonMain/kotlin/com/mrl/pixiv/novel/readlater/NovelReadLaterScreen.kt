package com.mrl.pixiv.novel.readlater

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.repository.NovelReadLaterItem
import com.mrl.pixiv.common.repository.NovelReadLaterRepository
import com.mrl.pixiv.common.repository.NovelReadLaterState
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
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
    navigationManager: NavigationManager = currentNavigationManager(),
) {
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
                },
            )
        },
    ) { paddingValues ->
        NovelReadLaterPage(
            modifier = Modifier.padding(paddingValues),
            repository = repository,
            navigationManager = navigationManager,
        )
    }
}

/** Repository-backed body shared by the standalone page and My Reading. */
@Composable
fun NovelReadLaterPage(
    modifier: Modifier = Modifier,
    repository: NovelReadLaterRepository = koinInject(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val itemsFlow = remember(repository) { repository.observeItems() }
    val items by itemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    NovelReadLaterContent(
        items = items,
        modifier = modifier,
        onOpen = { item ->
            navigationManager.navigateToNovelDetailScreen(
                novelId = item.novelId,
                readLaterTargetLanguage = item.targetLanguage,
            )
        },
        onRetry = { item ->
            scope.launch {
                try {
                    repository.retry(item.novelId, item.targetLanguage)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: IllegalArgumentException) {
                    ToastUtil.safeShortToast(RStrings.ai_translation_config_required)
                } catch (throwable: Throwable) {
                    ToastUtil.safeShortToast(
                        RStrings.ai_translation_failed,
                        throwable.message.orEmpty(),
                    )
                }
            }
        },
        onDelete = { item ->
            scope.launch { repository.remove(item.novelId, item.targetLanguage) }
        },
    )
}

@Composable
fun NovelReadLaterContent(
    items: List<NovelReadLaterItem>,
    onOpen: (NovelReadLaterItem) -> Unit,
    onRetry: (NovelReadLaterItem) -> Unit,
    onDelete: (NovelReadLaterItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (items.isEmpty()) {
            ContentEmptyState(
                message = stringResource(RStrings.read_later_empty),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            ) {
                items(items = items, key = { "${it.novelId}_${it.targetLanguage}" }) { item ->
                    NovelReadLaterItem(
                        item = item,
                        onOpen = { onOpen(item) },
                        onRetry = { onRetry(item) },
                        onDelete = { onDelete(item) },
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
