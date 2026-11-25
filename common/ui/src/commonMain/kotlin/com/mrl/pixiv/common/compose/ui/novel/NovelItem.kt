package com.mrl.pixiv.common.compose.ui.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.mrl.pixiv.common.compose.ui.BookmarkIcon
import com.mrl.pixiv.common.compose.ui.NovelBottomBookmarkSheet
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isPrivateBookmark
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.allowRgb565
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.strings.view_more
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * 小说列表项组件
 *
 * @param novel 小说数据
 * @param onNovelClick 点击小说时的回调
 * @param onSeriesClick 点击系列标题时的回调
 * @param onBookmarkClick 点击收藏按钮时的回调
 * @param modifier 修饰符
 * @param markerPageLabel 阅读书签页码；为空时不展示书签操作
 * @param onMarkerClick 点击阅读书签按钮时的回调
 * @param compactTitle 是否使用紧凑标题字号
 * @param compactMarkerLayout 在窄屏书签列表中缩小封面并分行显示作者、字数
 */
@Composable
fun NovelItem(
    novel: Novel,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onBookmarkClick: (Boolean, Restrict, List<String>?) -> Unit,
    modifier: Modifier = Modifier,
    markerPageLabel: String? = null,
    onMarkerClick: (() -> Unit)? = null,
    compactTitle: Boolean = false,
    compactMarkerLayout: Boolean = false,
    readLaterAction: @Composable () -> Unit = { NovelReadLaterButton(novel = novel) },
) {
    val isBookmarked = novel.isBookmark
    val isNovelBlocked = BlockingRepositoryV2.collectNovelBlockAsState(novel.id)
    var showBookmarkSheet by rememberSaveable { mutableStateOf(false) }
    var showMetadata by rememberSaveable(novel.id, isNovelBlocked) { mutableStateOf(false) }

    NovelItemContent(
        novel = novel,
        onNovelClick = onNovelClick,
        onSeriesClick = onSeriesClick,
        onBookmarkClick = {
            val restrict = if (requireUserPreferenceValue.defaultPrivateBookmark) {
                Restrict.PRIVATE
            } else {
                Restrict.PUBLIC
            }
            onBookmarkClick(isBookmarked, restrict, null)
        },
        onBookmarkLongClick = { showBookmarkSheet = true },
        onMetadataClick = if (isNovelBlocked) null else ({ showMetadata = true }),
        readLaterAction = readLaterAction,
        modifier = modifier,
        markerPageLabel = markerPageLabel,
        onMarkerClick = onMarkerClick,
        compactTitle = compactTitle,
        compactMarkerLayout = compactMarkerLayout,
    )

    if (showMetadata && !isNovelBlocked) {
        NovelMetadataBottomSheet(
            novel = novel,
            onDismissRequest = { showMetadata = false },
            onSeriesClick = onSeriesClick,
        )
    }

    if (showBookmarkSheet) {
        val bottomSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
        NovelBottomBookmarkSheet(
            hideBottomSheet = { showBookmarkSheet = false },
            novel = novel,
            bottomSheetState = bottomSheetState,
            onBookmarkClick = { restrict, tags, isEdit ->
                if (isEdit || !isBookmarked) {
                    BookmarkState.bookmarkNovel(novel.id, restrict, tags)
                } else {
                    BookmarkState.deleteBookmarkNovel(novel.id)
                }
            }
        )
    }
}

@Composable
internal fun NovelItemContent(
    novel: Novel,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onBookmarkClick: () -> Unit,
    onBookmarkLongClick: () -> Unit,
    onMetadataClick: (() -> Unit)?,
    readLaterAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    markerPageLabel: String? = null,
    onMarkerClick: (() -> Unit)? = null,
    compactTitle: Boolean = false,
    compactMarkerLayout: Boolean = false,
) {
    val context = LocalPlatformContext.current
    val isBookmarked = novel.isBookmark
    val isAI = novel.novelAiType == AiType.AiGeneratedWorks
    val seriesId = validNovelSeriesId(novel.series.id)
    val coverWidthFraction = 0.24f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNovelClick(novel.id) }
                .height(IntrinsicSize.Min)
        ) {
            // 封面跟随右侧内容的高度，图片加载后也不改变卡片尺寸。
            Box(
                modifier = Modifier
                    .weight(coverWidthFraction)
                    .fillMaxHeight()
                    .padding(8.dp),
            ) {
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
                        .matchParentSize()
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(Modifier.weight(1f - coverWidthFraction)) {
                Row(Modifier.fillMaxWidth()) {
                    // 中间内容
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        // 系列
                        if (seriesId != null && !novel.series.title.isNullOrEmpty()) {
                            Text(
                                text = novel.series.title ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    onSeriesClick(seriesId)
                                },
                            )
                        }

                        // 标题
                        Text(
                            text = novel.title,
                            style = if (compactTitle) {
                                MaterialTheme.typography.titleSmall
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Narrow marker cards keep the author readable beside the action column.
                        if (compactMarkerLayout) {
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    4.HSpacer
                                    Text(
                                        text = novel.user.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.TextFields,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    4.HSpacer
                                    Text(
                                        text = "${novel.textLength}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (isAI) {
                                        8.HSpacer
                                        NovelAiBadge()
                                    }
                                }
                            }
                        } else {
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
                                if (isAI) {
                                    8.HSpacer
                                    NovelAiBadge()
                                }
                            }
                        }

                        Text(
                            text = remember(novel.createDate) {
                                convertUtcStringToLocalDateTime(novel.createDate)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )

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
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        readLaterAction()
                        IconButton(
                            onClick = onBookmarkClick,
                            onLongClick = onBookmarkLongClick,
                        ) {
                            BookmarkIcon(
                                isBookmarked = isBookmarked,
                                isPrivate = novel.isPrivateBookmark,
                                iconSize = 24.dp,
                            )
                        }
                        Text(
                            text = "${novel.totalBookmarks}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (markerPageLabel != null && onMarkerClick != null) {
                            IconButton(onClick = onMarkerClick) {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = markerPageLabel,
                                )
                            }
                            Text(
                                text = markerPageLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (onMetadataClick != null) {
                    TextButton(
                        onClick = onMetadataClick,
                        modifier = Modifier.align(Alignment.End).padding(end = 8.dp),
                    ) {
                        Text(stringResource(RStrings.view_more))
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelAiBadge() {
    Badge(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = "AI",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}
