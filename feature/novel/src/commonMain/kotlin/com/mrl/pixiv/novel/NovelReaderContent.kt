package com.mrl.pixiv.novel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.ui.TagItem
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.bookmarked
import com.mrl.pixiv.strings.cover
import com.mrl.pixiv.strings.view_comments
import com.mrl.pixiv.strings.view_comments_count
import org.jetbrains.compose.resources.stringResource

private const val KEY_COVER = "cover"
private const val KEY_TITLE = "title"
private const val KEY_SERIES_TITLE = "series_title"
private const val KEY_AUTHOR = "author"
private const val KEY_STATS = "stats"
private const val KEY_CREATE_DATE = "create_date"
private const val KEY_TAGS = "tags"
private const val KEY_CAPTION = "caption"
private const val KEY_VIEW_COMMENTS = "view_comments"
private const val KEY_DIVIDER = "divider"
private const val KEY_TRANSLATION_WAITING = "translation_waiting"
private const val KEY_TRANSLATION_STREAMING_LOADING = "translation_streaming_loading"
private const val KEY_SPACER_END = "spacer_end"

@Composable
internal fun NovelReaderContent(
    state: NovelState,
    listState: LazyListState,
    readingProgressFraction: Float,
    modifier: Modifier = Modifier,
    onParagraphTextLayout: (Int, TextLayoutResult) -> Unit,
    onContentClick: () -> Unit = {},
    onTagClick: (String) -> Unit,
    onPixivImageClick: (Long) -> Unit,
    onAuthorClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onCaptionLinkClick: (String) -> Unit,
    onCommentClick: () -> Unit,
) {
    val novel = state.novel ?: return
    val isBookmarked = novel.isBookmark
    val totalBookmarks = (novel.totalBookmarks + when {
        isBookmarked && !novel.isBookmarked -> 1L
        !isBookmarked && novel.isBookmarked -> -1L
        else -> 0L
    }).coerceAtLeast(0L)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Vertical)
                .asPaddingValues(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 封面图
            item(key = KEY_COVER) {
                val isWidthAtLeastMedium = currentWindowAdaptiveInfoV2().isWidthAtLeastMedium
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(novel.imageUrls.medium)
                        .build(),
                    contentDescription = stringResource(RStrings.cover),
                    modifier = Modifier
                        .padding(top = 56.dp)
                        .fillMaxWidth(if (isWidthAtLeastMedium) 0.2f else 0.4f),
                    contentScale = ContentScale.FillWidth,
                    placeholder = rememberVectorPainter(Icons.Rounded.Refresh),
                    error = rememberVectorPainter(Icons.Rounded.ErrorOutline),
                )
            }

            // 标题
            item(key = KEY_TITLE) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )
            }

            item(key = KEY_AUTHOR) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .throttleClick { onAuthorClick(novel.user.id) }
                ) {
                    UserAvatar(
                        url = novel.user.profileImageUrls.medium,
                        modifier = Modifier.size(36.dp),
                        onClick = { onAuthorClick(novel.user.id) }
                    )
                    8.HSpacer
                    Text(
                        text = novel.user.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
            }

            // 系列标题
            val seriesId = novel.series.id?.takeIf { it > 0L }
            novel.series.title?.let { seriesTitle ->
                item(key = KEY_SERIES_TITLE) {
                    Text(
                        text = seriesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .then(
                                if (seriesId != null) {
                                    Modifier.throttleClick {
                                        onSeriesClick(seriesId)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }

            // 收藏数和观看数
            item(key = KEY_STATS) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = stringResource(RStrings.bookmarked),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    4.HSpacer
                    Text(
                        text = totalBookmarks.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    16.HSpacer

                    Icon(
                        Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    4.HSpacer
                    Text(
                        text = novel.totalView.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 创建时间
            item(key = KEY_CREATE_DATE) {
                Text(
                    text = convertUtcStringToLocalDateTime(novel.createDate),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // 标签
            item(key = KEY_TAGS) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    horizontalArrangement = 5f.spaceBy,
                    verticalArrangement = 5f.spaceBy,
                ) {
                    novel.tags.forEach { tag ->
                        TagItem(
                            tag = tag,
                            onClick = {
                                onTagClick(tag.name)
                            }
                        )
                    }
                }
            }

            // Caption卡片(如果有内容)
            if (novel.caption.isNotEmpty()) {
                item(key = KEY_CAPTION) {
                    val linkColor = MaterialTheme.colorScheme.primary
                    val caption = remember(novel.caption, linkColor, onCaptionLinkClick) {
                        novelCaptionToAnnotatedString(
                            html = novel.caption,
                            linkColor = linkColor,
                            onLinkClick = onCaptionLinkClick,
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = state.fontSize.sp,
                                lineHeight = (state.fontSize + state.lineSpacingSp + 8).sp
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item(key = KEY_VIEW_COMMENTS) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .fillMaxWidth()
                        .throttleClick(indication = ripple()) {
                            onCommentClick()
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = stringResource(RStrings.view_comments)
                    )
                    5.HSpacer
                    Text(
                        text = if (novel.totalComments != null) {
                            stringResource(
                                RStrings.view_comments_count,
                                novel.totalComments!!
                            )
                        } else {
                            stringResource(RStrings.view_comments)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // 正文分隔线
            item(key = KEY_DIVIDER) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                )
            }

            when (val presentation = state.translationPresentation) {
                NovelTranslationPresentation.Idle -> {
                    items(
                        count = state.paragraphSpans.size,
                        // 两个段落内容相同，hashcode也一样，这样就会导致列表状态异常，所以这里直接用index作为key
                        key = { it },
                    ) { index ->
                        NovelParagraph(
                            paragraphIndex = index,
                            fontSize = state.fontSize,
                            lineSpacingSp = state.lineSpacingSp,
                            span = state.paragraphSpans[index],
                            onParagraphTextLayout = onParagraphTextLayout,
                            onContentClick = onContentClick,
                            onPixivImageClick = onPixivImageClick,
                        )
                    }

                    item(key = KEY_SPACER_END) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                NovelTranslationPresentation.Waiting -> {
                    item(key = KEY_TRANSLATION_WAITING) {
                        NovelTranslationLoading(
                            centered = true,
                            modifier = Modifier.fillParentMaxHeight(),
                        )
                    }
                }

                is NovelTranslationPresentation.Streaming -> {
                    items(
                        count = presentation.spans.size,
                        key = { it },
                    ) { index ->
                        NovelParagraph(
                            paragraphIndex = index,
                            fontSize = state.fontSize,
                            lineSpacingSp = state.lineSpacingSp,
                            span = presentation.spans[index],
                            onParagraphTextLayout = { _, _ -> },
                            onContentClick = onContentClick,
                            onPixivImageClick = onPixivImageClick,
                        )
                    }

                    item(key = KEY_TRANSLATION_STREAMING_LOADING) {
                        NovelTranslationLoading(centered = false)
                    }
                }
            }
        }

        if (!state.isTranslating) {
            ReadingProgressIndicator(
                progress = readingProgressFraction,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
