package com.mrl.pixiv.profile.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.liked
import com.mrl.pixiv.strings.novel_table_of_contents
import com.mrl.pixiv.strings.view_all
import com.mrl.pixiv.strings.word_count
import org.jetbrains.compose.resources.stringResource

private const val MAX_PROFILE_NOVEL_PREVIEW_COUNT = 3

internal fun <T> previewProfileNovels(novels: List<T>): List<T> =
    novels.take(MAX_PROFILE_NOVEL_PREVIEW_COUNT)

@Composable
internal fun ProfileNovelPreview(
    title: String,
    novels: List<Novel>,
    showAuthor: Boolean,
    onAllClick: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            TextButton(
                onClick = throttleClick(onClick = onAllClick),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = stringResource(RStrings.view_all),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).size(12.dp),
                )
            }
        }
        HorizontalDivider()
        previewProfileNovels(novels).forEachIndexed { index, novel ->
            if (index > 0) {
                HorizontalDivider()
            }
            ProfileNovelPreviewItem(
                novel = novel,
                showAuthor = showAuthor,
                onNovelClick = onNovelClick,
                onSeriesClick = onSeriesClick,
            )
        }
    }
}

@Composable
private fun ProfileNovelPreviewItem(
    novel: Novel,
    showAuthor: Boolean,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
) {
    val seriesId = novel.series.id?.takeIf { it > 0L }
    val seriesTitle = novel.series.title?.takeIf { it.isNotEmpty() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = throttleClick { onNovelClick(novel.id) })
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = novel.imageUrls.medium,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 64.dp, height = 90.dp)
                .clip(MaterialTheme.shapes.small),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (seriesId != null && seriesTitle != null) {
                Text(
                    text = seriesTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = stringResource(RStrings.novel_table_of_contents),
                            onClick = throttleClick { onSeriesClick(seriesId) },
                        )
                        .padding(vertical = 4.dp),
                )
            }
            if (showAuthor) {
                Text(
                    text = novel.user.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NovelStatistic(
                    icon = Icons.Rounded.TextFields,
                    label = stringResource(RStrings.word_count),
                    count = novel.textLength,
                    modifier = Modifier.weight(1f, fill = false),
                )
                NovelStatistic(
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(RStrings.liked),
                    count = novel.totalBookmarks,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (novel.tags.isNotEmpty()) {
                Text(
                    text = novel.tags.joinToString(" ") { "#${it.name}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NovelStatistic(
    icon: ImageVector,
    label: String,
    count: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
