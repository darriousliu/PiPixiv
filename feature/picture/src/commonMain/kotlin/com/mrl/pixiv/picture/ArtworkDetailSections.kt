package com.mrl.pixiv.picture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
private val InfoGroupPadding = 16.dp

internal fun LazyListScope.artworkInfoItems(
    illust: Illust,
    inset: Modifier,
    onSearch: (Tag) -> Unit,
    series: @Composable (Modifier) -> Unit,
) {
    item(key = KEY_ILLUST_TITLE) {
        ArtworkInfoGroup(modifier = inset.padding(top = 12.dp)) {
            ArtworkTitleSection(illust)
            if (illust.caption.isNotBlank()) {
                ArtworkCaption(illust.caption)
            }
            val tags = illust.tags.orEmpty()
            if (tags.isNotEmpty()) {
                ArtworkTags(tags = tags, onSearch = onSearch)
            }
        }
    }
    if (illust.series?.id?.let { it > 0 } == true) {
        item(key = "manga_series_context") { series(inset.padding(top = 12.dp)) }
    }
}

@Composable
internal fun ArtworkInfoGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(InfoGroupPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun ArtworkTitleSection(illust: Illust) {
    SelectionContainer {
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
private fun ArtworkCaption(caption: String) {
    val text = remember(caption) { htmlToAnnotatedString(html = caption, compactMode = true) }
    SelectionContainer {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ArtworkTags(tags: List<Tag>, onSearch: (Tag) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
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
internal fun ArtworkAuthorGroup(
    columns: Int,
    workCount: Int,
    modifier: Modifier = Modifier,
    author: @Composable () -> Unit,
    work: @Composable (Int, Modifier) -> Unit,
) {
    ArtworkInfoGroup(modifier) {
        author()
        if (workCount > 0) {
            ArtworkWorkRow(
                columns = columns,
                itemCount = workCount,
                spacing = IllustGridSpacing.Thumbnail,
                item = work,
            )
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
    Surface(
        onClick = throttleClick(onClick = onClick),
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Comment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(RStrings.view_comments),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge.tabularNumbers,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun artworkAuthorColumnCount(paneWidth: Dp, edgePadding: Dp, isSplit: Boolean): Int {
    val density = LocalDensity.current
    val cells = IllustGridDefaults.userWorkCells(paneWidth, isSplit)
    return with(cells) {
        with(density) {
            val inset = edgePadding.roundToPx() + InfoGroupPadding.roundToPx()
            val available = (paneWidth.roundToPx() - 2 * inset).coerceAtLeast(0)
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
