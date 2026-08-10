package com.mrl.pixiv.profile.detail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.novel_description
import com.mrl.pixiv.strings.novels
import com.mrl.pixiv.strings.view_all
import org.jetbrains.compose.resources.stringResource

private const val MAX_NOVEL_WORKS_PREVIEW_COUNT = 3

internal fun shouldShowNovelWorks(novels: Collection<*>): Boolean = novels.isNotEmpty()

internal fun <T> previewNovelWorks(novels: List<T>): List<T> =
    novels.take(MAX_NOVEL_WORKS_PREVIEW_COUNT)

@Composable
fun NovelWorksWidget(
    novels: List<Novel>,
    onAllClick: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(RStrings.novels),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .throttleClick(onClick = onAllClick),
            ) {
                Text(
                    text = stringResource(RStrings.view_all),
                    fontSize = 12.sp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(12.dp),
                    tint = Color.Blue,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 5.dp))
        previewNovelWorks(novels).forEach { novel ->
            NovelWorkPreviewItem(
                novel = novel,
                onNovelClick = onNovelClick,
                onSeriesClick = onSeriesClick,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun NovelWorkPreviewItem(
    novel: Novel,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seriesId = novel.series.id?.takeIf { it > 0L }
    val seriesTitle = novel.series.title?.takeIf { it.isNotEmpty() }

    Column(modifier = modifier) {
        Row {
            AsyncImage(
                modifier = Modifier
                    .size(width = 64.dp, height = 90.dp)
                    .throttleClick { onNovelClick(novel.id) },
                model = novel.imageUrls.medium,
                contentDescription = novel.title,
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (seriesId != null && seriesTitle != null) {
                    Text(
                        text = seriesTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                        modifier = Modifier.throttleClick {
                            onSeriesClick(seriesId)
                        },
                    )
                }
                Text(
                    text = novel.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = if (seriesTitle == null) 0.dp else 5.dp)
                        .throttleClick { onNovelClick(novel.id) },
                )
                Text(
                    text = "by ${novel.user.name}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(
                    text = stringResource(
                        RStrings.novel_description,
                        novel.textLength,
                        novel.tags.joinToString(" ") { "#${it.name}" },
                    ),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp),
        )
    }
}
