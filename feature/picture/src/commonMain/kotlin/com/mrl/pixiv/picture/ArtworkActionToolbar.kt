package com.mrl.pixiv.picture

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.ui.BookmarkIcon
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.cancel_favorite
import com.mrl.pixiv.strings.download
import com.mrl.pixiv.strings.liked
import com.mrl.pixiv.strings.share
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ArtworkActionToolbar(
    isBookmarked: Boolean,
    isPrivateBookmark: Boolean,
    canDownload: Boolean,
    onDownload: () -> Unit,
    onBookmark: () -> Unit,
    onEditBookmark: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloadLabel = stringResource(RStrings.download)
    val bookmarkLabel = stringResource(if (isBookmarked) RStrings.cancel_favorite else RStrings.liked)
    val shareLabel = stringResource(RStrings.share)

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        floatingActionButton = {
            ArtworkActionTooltip(bookmarkLabel, preserveLongClick = true) {
                ArtworkBookmarkFab(
                    isBookmarked = isBookmarked,
                    isPrivate = isPrivateBookmark,
                    label = bookmarkLabel,
                    onClick = onBookmark,
                    onLongClick = onEditBookmark,
                )
            }
        },
    ) {
        ArtworkActionTooltip(downloadLabel) {
            IconButton(onClick = onDownload, enabled = canDownload) {
                Icon(Icons.Rounded.Download, contentDescription = downloadLabel)
            }
        }
        Spacer(Modifier.width(8.dp))
        ArtworkActionTooltip(shareLabel) {
            IconButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, contentDescription = shareLabel)
            }
        }
    }
}

@Composable
private fun ArtworkBookmarkFab(
    isBookmarked: Boolean,
    isPrivate: Boolean,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors()
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    // 与标准工具栏 FAB 的层级一致：普通、按下、聚焦为 3dp，悬停为 6dp。
    val shadowElevation by animateDpAsState(if (hovered && !pressed && !focused) 6.dp else 3.dp)
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = FloatingActionButtonDefaults.shape,
        color = colors.fabContainerColor,
        contentColor = colors.fabContentColor,
        tonalElevation = 3.dp,
        shadowElevation = shadowElevation,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
            contentAlignment = Alignment.Center,
        ) {
            BookmarkIcon(
                isBookmarked = isBookmarked,
                isPrivate = isPrivate,
                contentDescription = label,
                tint = LocalContentColor.current,
            )
        }
    }
}

@Composable
private fun ArtworkActionTooltip(
    label: String,
    preserveLongClick: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState()
    val modifier = if (preserveLongClick) {
        // 收藏提示只响应悬停，触摸长按由编辑收藏操作处理。
        val interactionSource = remember { MutableInteractionSource() }
        val hovered by interactionSource.collectIsHoveredAsState()
        LaunchedEffect(hovered) {
            if (hovered) {
                delay(500)
                state.show()
            } else {
                state.dismiss()
            }
        }
        Modifier.hoverable(interactionSource)
    } else {
        Modifier
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = state,
        modifier = modifier,
        enableUserInput = !preserveLongClick,
        content = content,
    )
}
