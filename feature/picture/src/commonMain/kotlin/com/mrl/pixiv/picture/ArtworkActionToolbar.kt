package com.mrl.pixiv.picture

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        ArtworkActionTooltip(downloadLabel) {
            FilledTonalIconButton(onClick = onDownload, enabled = canDownload) {
                Icon(Icons.Rounded.Download, contentDescription = downloadLabel)
            }
        }
        Spacer(Modifier.width(8.dp))
        ArtworkActionTooltip(bookmarkLabel, preserveLongClick = true) {
            IconButton(onClick = onBookmark, onLongClick = onEditBookmark) {
                BookmarkIcon(
                    isBookmarked = isBookmarked,
                    isPrivate = isPrivateBookmark,
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    contentDescription = bookmarkLabel,
                )
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
private fun ArtworkActionTooltip(
    label: String,
    preserveLongClick: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState()
    val modifier = if (preserveLongClick) {
        // The tooltip's default touch handling would consume the edit-bookmark gesture.
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
