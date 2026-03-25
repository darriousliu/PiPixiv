package com.mrl.pixiv.setting.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.lightBlue
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.block_illust
import com.mrl.pixiv.strings.block_novel
import com.mrl.pixiv.strings.block_tags
import com.mrl.pixiv.strings.block_user
import com.mrl.pixiv.strings.no_blocked_items
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun BlockIllustScreen(
    modifier: Modifier = Modifier,
) {
    val blockedIllusts by BlockingRepositoryV2.blockIllustItemsFlow
        .collectAsStateWithLifecycle(emptyList())
    BlockTextScreen(
        title = stringResource(RStrings.block_illust),
        items = blockedIllusts,
        key = { it.illustId },
        onRemove = { item ->
            BlockingRepositoryV2.removeBlockIllust(item.illustId)
        },
        modifier = modifier,
        itemContent = {
            Text(
                text = it.title.ifBlank { it.illustId.toString() },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
    )
}

@Composable
fun BlockNovelScreen(
    modifier: Modifier = Modifier,
) {
    val blockedNovels by BlockingRepositoryV2.blockNovelItemsFlow
        .collectAsStateWithLifecycle(emptyList())
    BlockTextScreen(
        title = stringResource(RStrings.block_novel),
        items = blockedNovels,
        key = { it.novelId },
        onRemove = { item ->
            BlockingRepositoryV2.removeBlockNovel(item.novelId)
        },
        modifier = modifier,
        itemContent = {
            Text(
                text = it.title.ifBlank { it.novelId.toString() },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
    )
}

@Composable
fun BlockUserScreen(
    modifier: Modifier = Modifier,
) {
    val blockedUsers by BlockingRepositoryV2.blockUserItemsFlow
        .collectAsStateWithLifecycle(emptyList())
    BlockTextScreen(
        title = stringResource(RStrings.block_user),
        items = blockedUsers,
        key = { it.userId },
        onRemove = { item ->
            BlockingRepositoryV2.removeBlockUser(item.userId)
        },
        modifier = modifier,
        itemContent = {
            Text(
                text = it.name.ifBlank { it.userId.toString() },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
    )
}

@Composable
fun BlockTagScreen(
    modifier: Modifier = Modifier,
) {
    val blockedTags by BlockingRepositoryV2.blockTagItemsFlow.collectAsStateWithLifecycle(emptyList())
    val tags = blockedTags.sortedBy { it.tag.lowercase() }
    BlockTextScreen(
        title = stringResource(RStrings.block_tags),
        items = tags,
        key = { it.tag },
        onRemove = { item -> BlockingRepositoryV2.removeBlockTag(item.tag) },
        modifier = modifier,
        itemContent = {
            Text(
                text = it.tag,
                modifier = Modifier.weight(1f),
                color = if (it.isRegex) lightBlue else Color.Unspecified,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    )
}

@Composable
private fun <T> BlockTextScreen(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    onRemove: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable RowScope.(T) -> Unit = {},
) {
    val navigationManager = koinInject<NavigationManager>()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title)
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(RStrings.no_blocked_items),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> key(item) }
            ) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    itemContent(item)
                    IconButton(onClick = { onRemove(item) }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null
                        )
                    }
                }
                if (index != items.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
