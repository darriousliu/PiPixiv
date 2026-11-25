package com.mrl.pixiv.picture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.IllustGridSpacing
import com.mrl.pixiv.common.compose.theme.tabularNumbers
import com.mrl.pixiv.common.compose.ui.FollowButton
import com.mrl.pixiv.common.compose.ui.TagItem
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.liked
import com.mrl.pixiv.strings.view_comments
import com.mrl.pixiv.strings.viewed
import org.jetbrains.compose.resources.stringResource

private const val KEY_ILLUST_TITLE = "illust_title"
private const val KEY_ILLUST_DATA = "illust_data"
private const val KEY_ILLUST_TAGS = "illust_tags"

internal fun LazyListScope.artworkInfoItems(
    illust: Illust,
    inset: Modifier,
    onSearch: (Tag) -> Unit,
    series: @Composable (Modifier) -> Unit,
) {
    item(key = KEY_ILLUST_TITLE) {
        ArtworkTitleSection(illust, modifier = inset.padding(top = 20.dp))
    }
    if (illust.caption.isNotBlank()) {
        item(key = KEY_ILLUST_DATA) {
            ArtworkCaption(illust.caption, modifier = inset.padding(top = 16.dp))
        }
    }
    if (illust.series?.id?.let { it > 0 } == true) {
        item(key = "manga_series_context") { series(inset.padding(top = 16.dp)) }
    }
    val tags = illust.tags.orEmpty()
    if (tags.isNotEmpty()) {
        item(key = KEY_ILLUST_TAGS) {
            ArtworkTags(tags = tags, onSearch = onSearch, modifier = inset.padding(top = 16.dp))
        }
    }
}

@Composable
private fun ArtworkTitleSection(illust: Illust, modifier: Modifier = Modifier) {
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = illust.title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlexBox(
                modifier = Modifier.fillMaxWidth(),
                config = {
                    wrap(FlexWrap.Wrap)
                    gap(row = 6.dp, column = 16.dp)
                },
            ) {
                ArtworkStat(Icons.Rounded.Visibility, illust.totalView.toString(), stringResource(RStrings.viewed))
                ArtworkStat(Icons.Rounded.Favorite, illust.totalBookmarks.toString(), stringResource(RStrings.liked))
                ArtworkStat(Icons.Rounded.CalendarMonth, convertUtcStringToLocalDateTime(illust.createDate), null)
            }
        }
    }
}

@Composable
private fun ArtworkStat(icon: ImageVector, value: String, label: String?) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = color)
        Text(text = value, style = MaterialTheme.typography.labelLarge.tabularNumbers, color = color)
    }
}

@Composable
private fun ArtworkCaption(caption: String, modifier: Modifier = Modifier) {
    val text = remember(caption) { htmlToAnnotatedString(html = caption, compactMode = true) }
    SelectionContainer(modifier) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArtworkTags(tags: List<Tag>, onSearch: (Tag) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val tagMaxWidth = maxWidth
        FlexBox(
            modifier = Modifier.fillMaxWidth(),
            config = {
                wrap(FlexWrap.Wrap)
                gap(8.dp)
            },
        ) {
            tags.forEach { tag ->
                TagItem(
                    tag = tag,
                    onClick = { onSearch(tag) },
                    modifier = Modifier.widthIn(max = tagMaxWidth),
                )
            }
        }
    }
}

@Composable
internal fun ArtworkAuthor(
    user: User,
    isFollowed: Boolean,
    isUpdating: Boolean,
    onOpen: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            url = user.profileImageUrls.medium,
            modifier = Modifier.size(48.dp),
            onClick = onOpen,
            contentDescription = user.name,
        )
        SelectionContainer(Modifier.weight(1f)) {
            Column {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "ID: ${user.id}",
                    style = MaterialTheme.typography.labelMedium.tabularNumbers,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FollowButton(isFollowing = isFollowed, onClick = onFollow, isUpdating = isUpdating, emphasized = false)
    }
}

@Composable
internal fun ArtworkComments(count: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SegmentedListItem(
        onClick = throttleClick(onClick = onClick),
        modifier = modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = {
            Icon(
                Icons.AutoMirrored.Rounded.Comment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = count?.let {
            { Text(text = it.toString(), style = MaterialTheme.typography.labelLarge.tabularNumbers) }
        },
    ) {
        Text(stringResource(RStrings.view_comments))
    }
}

@Composable
internal fun artworkAuthorColumnCount(paneWidth: Dp, edgePadding: Dp, isSplit: Boolean): Int {
    val density = LocalDensity.current
    val cells = IllustGridDefaults.userWorkCells(paneWidth, isSplit)
    return with(cells) {
        with(density) {
            val available = (paneWidth.roundToPx() - 2 * edgePadding.roundToPx()).coerceAtLeast(0)
            calculateCrossAxisCellSizes(available, IllustGridSpacing.Thumbnail.roundToPx()).size
        }
    }
}

@Composable
internal fun ArtworkWorkRow(
    columns: Int,
    itemCount: Int,
    spacing: Dp,
    modifier: Modifier = Modifier,
    item: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
        val cell = Modifier.weight(1f)
        repeat(itemCount) { index -> item(index, cell) }
        repeat(columns - itemCount) { Spacer(cell) }
    }
}
