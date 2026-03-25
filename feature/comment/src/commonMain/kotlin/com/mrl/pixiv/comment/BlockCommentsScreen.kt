package com.mrl.pixiv.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.comment.components.CommentItem
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.hPadding
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.block_comments
import com.mrl.pixiv.strings.no_blocked_items
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun BlockCommentsScreen(
    modifier: Modifier = Modifier,
) {
    val navigationManager = koinInject<NavigationManager>()
    val blockedComments by BlockingRepositoryV2.blockCommentsFlow
        .collectAsStateWithLifecycle(emptyList())

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.block_comments))
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack
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
        if (blockedComments.isEmpty()) {
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
            contentPadding = 8.hPadding,
            verticalArrangement = 8.spaceBy
        ) {
            itemsIndexed(
                items = blockedComments,
                key = { _, item -> item.id }
            ) { index, comment ->
                CommentItem(
                    comment = comment,
                    onReplyComment = {},
                    onBlockComment = {},
                    onReportComment = {},
                    onNavToUserProfile = {
                        navigationManager.navigateToProfileDetailScreen(comment.user.id)
                    },
                    onDeleteComment = {},
                    isBlockScreen = true,
                    onRemoveBlock = {
                        BlockingRepositoryV2.removeBlockComment(comment.id)
                    }
                )
                if (index != blockedComments.size - 1) {
                    8.VSpacer
                    HorizontalDivider()
                }
            }
        }
    }
}
