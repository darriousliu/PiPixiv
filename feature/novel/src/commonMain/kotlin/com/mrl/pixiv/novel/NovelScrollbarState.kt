package com.mrl.pixiv.novel

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import com.mrl.pixiv.common.compose.ui.ScrollbarMetrics
import com.mrl.pixiv.common.compose.ui.ScrollbarState
import kotlin.math.abs

private const val NOVEL_THUMB_SIZE = 0.05f
private const val METADATA_WEIGHT = 128

/** 用全文的固定权重定位，避免可见段落高度变化时重新估算整篇长度。 */
internal class NovelScrollContent(weights: List<Int>) {
    private val boundaries = DoubleArray(weights.size + 1).apply {
        weights.forEachIndexed { index, weight ->
            this[index + 1] = this[index] + weight.coerceAtLeast(1)
        }
    }
    val size: Int get() = boundaries.size - 1
    val total: Double get() = boundaries.last()

    fun position(index: Int, itemFraction: Double): Double {
        if (index < 0) return 0.0
        if (index >= size) return total
        return boundaries[index] +
                (boundaries[index + 1] - boundaries[index]) * itemFraction.coerceIn(0.0, 1.0)
    }

    fun target(fraction: Float): NovelScrollTarget? {
        if (size == 0) return null
        val position = fraction.coerceIn(0f, 1f) * total
        var low = 0
        var high = size - 1
        while (low < high) {
            val middle = (low + high) / 2
            if (boundaries[middle + 1] <= position) low = middle + 1 else high = middle
        }
        val itemFraction = (position - boundaries[low]) /
                (boundaries[low + 1] - boundaries[low])
        return NovelScrollTarget(low, itemFraction.coerceIn(0.0, 1.0))
    }

    fun metrics(start: Double, end: Double): ScrollbarMetrics {
        val before = start.coerceIn(0.0, total)
        val after = (total - end).coerceIn(0.0, total)
        if (before + after <= 0.0) return ScrollbarMetrics(0f, 1f)
        // 滑块使用固定长度；可见区前后的内容比例保证向下阅读时进度不会倒退。
        val fraction = (before / (before + after)).toFloat()
        return ScrollbarMetrics(fraction * (1f - NOVEL_THUMB_SIZE), NOVEL_THUMB_SIZE)
    }

    /** 根据实际可见范围反求顶部位置，兼容封面、短段落与长正文混排。 */
    suspend fun seek(
        fraction: Float,
        visibleRange: () -> Pair<Double, Double>?,
        scrollTo: suspend (NovelScrollTarget) -> Unit,
    ) {
        if (size == 0 || !fraction.isFinite()) return
        val progress = fraction.coerceIn(0f, 1f).toDouble()
        var lower = 0.0
        var upper = total
        repeat(16) {
            val (start, end) = visibleRange() ?: return
            val remaining = total - (end - start)
            if (remaining <= 0.0) return
            val current = start / remaining
            if (abs(current - progress) < 0.0005) return
            if (current < progress) lower = start else upper = start
            // 优先按当前视口求解；跨段落时用边界约束避免估算反复越过目标。
            val estimate = progress * remaining
            val position = if (estimate > lower && estimate < upper) estimate else (lower + upper) / 2
            val target = target((position / total).toFloat()) ?: return
            scrollTo(target)
            val next = visibleRange() ?: return
            if (abs(next.first - start) < 0.00001) return
        }
    }
}

internal data class NovelScrollTarget(val index: Int, val itemFraction: Double)

internal fun novelScrollContent(
    spans: List<NovelSpanData>,
    hasSeriesTitle: Boolean,
    caption: String,
): NovelScrollContent = NovelScrollContent(buildList {
    // 顺序与阅读列表的封面、标题、作者、系列、统计、日期、标签、简介及操作区一致。
    repeat(3) { add(METADATA_WEIGHT) }
    if (hasSeriesTitle) add(METADATA_WEIGHT)
    repeat(3) { add(METADATA_WEIGHT) }
    if (caption.isNotEmpty()) add(caption.length.coerceAtLeast(METADATA_WEIGHT))
    repeat(2) { add(METADATA_WEIGHT) }
    spans.forEach { span ->
        add(when (span) {
            is NovelSpanData.Text -> span.value.length.coerceAtLeast(1)
            is NovelSpanData.JumpUri -> span.value.length.coerceAtLeast(1)
            is NovelSpanData.PixivImage, is NovelSpanData.UploadedImage -> METADATA_WEIGHT
            NovelSpanData.NewPage -> 1
        })
    }
    add(1)
})

internal class NovelScrollbarState(
    private val listState: LazyListState,
    private val content: NovelScrollContent,
) : ScrollbarState {
    override val isScrollInProgress: Boolean get() = listState.isScrollInProgress

    override fun computeMetrics(): ScrollbarMetrics {
        val (start, end) = visibleRange() ?: return ScrollbarMetrics(0f, 1f)
        return content.metrics(start, end)
    }

    private fun visibleRange(): Pair<Double, Double>? {
        val layout = listState.layoutInfo
        if (layout.totalItemsCount != content.size ||
            (!listState.canScrollBackward && !listState.canScrollForward)
        ) return null
        val visible = layout.visibleItemsInfo
        val first = visible.firstOrNull() ?: return null
        val last = visible.last()
        val start = if (!listState.canScrollBackward) 0.0 else content.position(
            first.index,
            (layout.viewportStartOffset - first.offset).toDouble() / first.size.coerceAtLeast(1),
        )
        val end = if (!listState.canScrollForward) content.total else content.position(
            last.index,
            (layout.viewportEndOffset - last.offset).toDouble() / last.size.coerceAtLeast(1),
        )
        return start to end
    }

    override suspend fun scrollToFraction(fraction: Float) {
        if (!fraction.isFinite() || content.size == 0 ||
            listState.layoutInfo.totalItemsCount != content.size
        ) return
        val progress = fraction.coerceIn(0f, 1f)
        if (progress == 0f) {
            listState.scrollToItem(0)
            return
        }
        if (progress == 1f) {
            listState.scrollToItem(content.size - 1)
            val layout = listState.layoutInfo
            listState.scrollBy((layout.viewportEndOffset - layout.viewportStartOffset).toFloat())
            return
        }
        content.seek(progress, ::visibleRange, ::scrollTo)
    }

    private suspend fun scrollTo(target: NovelScrollTarget) {
        // 已可见的段落直接相对滚动，避免拖动时反复回到段落开头。
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == target.index }) {
            listState.scrollToItem(target.index)
        }
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == target.index } ?: return
        val distance = item.offset + target.itemFraction * item.size -
                layout.viewportStartOffset
        listState.scrollBy(distance.toFloat())
    }
}
