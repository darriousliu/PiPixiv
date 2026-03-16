package com.mrl.pixiv.common.compose.ui.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mrl.pixiv.common.compose.FavoriteDualColor
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.util.allowRgb565
import kotlin.time.Duration.Companion.seconds

/**
 * 小说列表项组件
 *
 * @param novel 小说数据
 * @param onNovelClick 点击小说时的回调
 * @param onBookmarkClick 点击收藏按钮时的回调
 * @param modifier 修饰符
 */
@Composable
fun NovelItem(
    novel: Novel,
    onNovelClick: (Long) -> Unit,
    onBookmarkClick: (Boolean, Restrict, List<String>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val isBookmarked = novel.isBookmark

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onNovelClick(novel.id) },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 左侧封面
            AsyncImage(
                model = remember {
                    ImageRequest.Builder(context)
                        .data(novel.imageUrls.medium)
                        .allowRgb565(true)
                        .crossfade(1.seconds.inWholeMilliseconds.toInt())
                        .build()
                },
                contentDescription = novel.title,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                contentScale = ContentScale.Crop
            )

            8.HSpacer

            // 中间内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                // 系列
                if (novel.series.id != null && !novel.series.title.isNullOrEmpty()) {
                    Text(
                        text = novel.series.title ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 标题
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // 作者和字数
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    4.HSpacer
                    Text(
                        text = novel.user.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    8.HSpacer
                    Icon(
                        imageVector = Icons.Rounded.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    4.HSpacer
                    Text(
                        text = "${novel.textLength}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 标签
                if (novel.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val tagsText = novel.tags.take(3).joinToString(" ") { tag ->
                            val name = tag.name
                            val translatedName = tag.translatedName.takeIf { it.isNotEmpty() }
                            if (translatedName != null) {
                                "#$name $translatedName"
                            } else {
                                "#$name"
                            }
                        }
                        Text(
                            text = tagsText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            4.HSpacer

            // 右侧收藏按钮和收藏数
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                IconButton(
                    onClick = {
                        val restrict = if (requireUserPreferenceValue.defaultPrivateBookmark) {
                            Restrict.PRIVATE
                        } else {
                            Restrict.PUBLIC
                        }
                        onBookmarkClick(isBookmarked, restrict, null)
                    }
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = FavoriteDualColor(isBookmarked),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${novel.totalBookmarks}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
