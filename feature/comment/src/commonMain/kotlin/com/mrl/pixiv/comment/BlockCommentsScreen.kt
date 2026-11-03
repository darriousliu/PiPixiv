package com.mrl.pixiv.comment

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.comment.components.CommentItem
import com.mrl.pixiv.common.compose.ui.ContentEmptyState
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.hPadding
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.block_comments
import com.mrl.pixiv.strings.no_blocked_items
import org.jetbrains.compose.resources.stringResource

@Composable
fun BlockCommentsScreen(
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    val navigationManager = currentNavigationManager()
    val blockedComments by BlockingRepositoryV2.blockCommentsFlow
        .collectAsStateWithLifecycle(emptyList())
    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        BlockCommentsContent(
            blockedComments = blockedComments,
            onNavToUserProfile = navigationManager::navigateToProfileDetailScreen,
            onRemoveBlock = BlockingRepositoryV2::removeBlockComment,
            modifier = contentModifier,
        )
    }
    if (embedded) {
        content(modifier)
    } else {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(RStrings.block_comments)) },
                    navigationIcon = {
                        IconButton(onClick = navigationManager::popBackStack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        }
                    },
                )
            },
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}

@Composable
internal fun BlockCommentsContent(
    blockedComments: List<Comment>,
    onNavToUserProfile: (Long) -> Unit,
    onRemoveBlock: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (blockedComments.isEmpty()) {
        ContentEmptyState(
            modifier = modifier.fillMaxSize(),
            message = stringResource(RStrings.no_blocked_items),
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = 8.hPadding,
        verticalArrangement = 8.spaceBy,
    ) {
        itemsIndexed(items = blockedComments, key = { _, item -> item.id }) { index, comment ->
            CommentItem(
                comment = comment,
                onReplyComment = {},
                onBlockComment = {},
                onReportComment = {},
                onNavToUserProfile = { onNavToUserProfile(comment.user.id) },
                onDeleteComment = {},
                isBlockScreen = true,
                onRemoveBlock = { onRemoveBlock(comment.id) },
            )
            if (index != blockedComments.lastIndex) {
                8.VSpacer
                HorizontalDivider()
            }
        }
    }
}
