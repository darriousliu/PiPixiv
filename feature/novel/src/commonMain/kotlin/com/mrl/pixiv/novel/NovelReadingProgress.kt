package com.mrl.pixiv.novel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.repository.NovelReadingProgress
import kotlin.math.roundToInt

@Composable
internal fun ReadingProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val percent = (progress.coerceIn(0f, 1f) * 100f)
        .roundToInt()
        .coerceIn(0, 100)
    val density = LocalDensity.current
    val bottomPadding = with(density) {
        WindowInsets.systemBars.getBottom(this).toDp()
    }

    AnimatedVisibility(
        visible = percent > 0,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = bottomPadding + 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

internal fun paragraphStartItemIndex(
    hasSeriesTitle: Boolean,
    hasCaption: Boolean
): Int {
    // cover + title + author + stats + create_date + tags + comments + divider
    var itemCountBeforeParagraphs = 8
    if (hasSeriesTitle) itemCountBeforeParagraphs += 1
    if (hasCaption) itemCountBeforeParagraphs += 1
    return itemCountBeforeParagraphs
}

internal data class ParagraphLayoutCacheKey(
    val novelId: Long?,
    val paragraphs: List<String>,
    val fontSize: Int,
    val lineSpacingSp: Int,
)

internal fun NovelState.paragraphLayoutCacheKey(): ParagraphLayoutCacheKey =
    ParagraphLayoutCacheKey(
        novelId = novel?.id,
        paragraphs = paragraphs,
        fontSize = fontSize,
        lineSpacingSp = lineSpacingSp,
    )

internal fun buildVisibleReadingProgress(
    listState: LazyListState,
    paragraphStartIndex: Int,
    paragraphCount: Int,
    paragraphLayouts: Map<Int, TextLayoutResult>,
    paragraphs: List<String>
): NovelReadingProgress? {
    if (paragraphCount <= 0) return null
    val layoutInfo = listState.layoutInfo
    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    val contentRange = paragraphStartIndex until (paragraphStartIndex + paragraphCount)
    if (firstVisibleItem.index !in contentRange) {
        return null
    }

    val textVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
        val paragraphIndex = itemInfo.index - paragraphStartIndex
        paragraphIndex in 0 until paragraphCount && paragraphLayouts[paragraphIndex] != null
    } ?: return null

    val paragraphIndex =
        (textVisibleItem.index - paragraphStartIndex).coerceIn(0, paragraphCount - 1)
    val paragraphLayout = paragraphLayouts[paragraphIndex] ?: return null

    // 计算视口顶部相对于段落的Y坐标
    val yInParagraph = (layoutInfo.viewportStartOffset - textVisibleItem.offset)
        .coerceIn(0, textVisibleItem.size - 1)
        .toFloat()

    // 获取视口顶部对应的字符位置
    val charAtViewportTop = paragraphLayout.getOffsetForPosition(
        position = Offset(x = 0f, y = yInParagraph)
    )

    // 获取该字符所在的行号
    val lineIndex = paragraphLayout.getLineForOffset(charAtViewportTop)

    // 获取该行的第一个字符位置（行首字符）
    val lineStartChar = paragraphLayout.getLineStart(lineIndex)

    Logger.d(tag = "NovelScreen") {
        "Save: paragraphIndex=$paragraphIndex, lineIndex=$lineIndex, " +
                "lineStartChar=$lineStartChar, yInParagraph=$yInParagraph"
    }

    val paragraphHash = paragraphs[paragraphIndex].hashCode()
    return NovelReadingProgress(
        paragraphIndex = paragraphIndex,
        charIndex = lineStartChar,
        paragraphHash = paragraphHash
    )
}

internal fun buildCumulativeParagraphLengths(paragraphs: List<String>): LongArray {
    val result = LongArray(paragraphs.size + 1)
    paragraphs.forEachIndexed { index, paragraph ->
        result[index + 1] = result[index] + paragraph.length
    }
    return result
}

internal fun buildBottomReadingProgressFraction(
    listState: LazyListState,
    paragraphStartIndex: Int,
    paragraphCount: Int,
    paragraphLayouts: Map<Int, TextLayoutResult>,
    paragraphs: List<String>,
    cumulativeParagraphLengths: LongArray,
): Float {
    if (paragraphCount <= 0 || paragraphs.isEmpty() || cumulativeParagraphLengths.size < 2) {
        return 0f
    }

    val totalTextLength = cumulativeParagraphLengths.last().coerceAtLeast(1L)
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0f

    val paragraphEndIndex = paragraphStartIndex + paragraphCount - 1
    val viewportBottom = layoutInfo.viewportEndOffset
    val bottomParagraphItem = visibleItems.lastOrNull { itemInfo ->
        itemInfo.index in paragraphStartIndex..paragraphEndIndex &&
                itemInfo.offset < viewportBottom
    } ?: return when {
        visibleItems.last().index < paragraphStartIndex -> 0f
        visibleItems.first().index > paragraphEndIndex -> 1f
        else -> 0f
    }

    val paragraphIndex = (bottomParagraphItem.index - paragraphStartIndex)
        .coerceIn(0, paragraphCount - 1)
    val paragraphLength = paragraphs[paragraphIndex].length
    val bottomYInParagraph = viewportBottom - bottomParagraphItem.offset
    val charIndex = when {
        paragraphLength <= 0 -> 0
        bottomYInParagraph >= bottomParagraphItem.size -> paragraphLength
        else -> {
            val layout = paragraphLayouts[paragraphIndex]
            if (layout != null) {
                layout.visibleEndCharAtY(
                    y = bottomYInParagraph.toFloat(),
                    paragraphLength = paragraphLength,
                )
            } else {
                val fraction = bottomYInParagraph.toFloat() /
                        bottomParagraphItem.size.coerceAtLeast(1).toFloat()
                (paragraphLength * fraction).roundToInt().coerceIn(0, paragraphLength)
            }
        }
    }

    val textBeforeParagraph = cumulativeParagraphLengths
        .getOrElse(paragraphIndex) { 0L }
    return ((textBeforeParagraph + charIndex).toDouble() / totalTextLength)
        .toFloat()
        .coerceIn(0f, 1f)
}

private fun TextLayoutResult.visibleEndCharAtY(
    y: Float,
    paragraphLength: Int,
): Int {
    if (paragraphLength <= 0) return 0
    if (y >= size.height) return paragraphLength

    val maxY = (size.height - 1).coerceAtLeast(0).toFloat()
    val offset = getOffsetForPosition(
        position = Offset(x = 0f, y = y.coerceIn(0f, maxY))
    )
    val lineIndex = getLineForOffset(offset)
    return getLineEnd(lineIndex, visibleEnd = true).coerceIn(0, paragraphLength)
}
