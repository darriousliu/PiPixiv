package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// ── 公共视觉类型 ───────────────────────────────────────────────────────────

data class ScrollbarMetrics(
    val thumbOffsetFraction: Float,
    val thumbSizeFraction: Float,
)

/**
 * 定义滚动条的视觉样式。
 *
 * @param minimalHeight 最小滑块高度，以便其保持可抓取
 * @param thickness 滚动条轨道和滑块的交叉轴尺寸
 * @param shape 滑块的形状
 * @param hoverDurationMillis 悬停时的颜色过渡动画持续时间（毫秒）
 * @param scrollDurationMillis 静止到滚动时的颜色过渡动画持续时间（毫秒）
 * @param hideDurationMillis 滚动到静止（消失）时的颜色过渡动画持续时间（毫秒）
 * @param unhoverColor 空闲时的滑块颜色（在移动设备上可以为 [Color.Transparent]）
 * @param hoverColor 滚动、拖动或悬停时的滑块颜色
 */
@Immutable
data class ScrollbarStyle(
    val minimalHeight: Dp,
    val thickness: Dp,
    val shape: Shape,
    val hoverDurationMillis: Int,
    val scrollDurationMillis: Int,
    val hideDurationMillis: Int,
    val unhoverColor: Color,
    val hoverColor: Color,
    val hideDelayMillis: Int,
)

/** 简单的默认 [ScrollbarStyle]。在您的主题中通过 [LocalScrollbarStyle] 进行覆盖。 */
fun defaultScrollbarStyle() = ScrollbarStyle(
    minimalHeight = 16.dp,
    thickness = 8.dp,
    shape = RoundedCornerShape(4.dp),
    hoverDurationMillis = 300,
    scrollDurationMillis = 300,
    hideDurationMillis = 1000,
    unhoverColor = Color.Transparent,
    hoverColor = Color.Black.copy(alpha = 0.50f),
    hideDelayMillis = 1000,
)

/**
 * [androidx.compose.runtime.CompositionLocal] 用于在树中向下传递 [ScrollbarStyle]。
 * 在您的主题 composable 中设置此项以一次性自定义所有滚动条。
 */
val LocalScrollbarStyle = staticCompositionLocalOf { defaultScrollbarStyle() }

// ── 内部滚动适配器（模仿 Compose Desktop v2） ───────────────────────────────

private const val MIN_THUMB_SIZE_FRACTION = 0.05f

/**
 * 仿照 Compose Desktop v2 ScrollbarAdapter 的内部适配器接口。
 * 以绝对像素单位表示滚动位置，将 [ScrollbarState.computeMetrics]
 * 和 [ScrollbarState.scrollToFraction] 与具体的布局逻辑解耦。
 */
internal interface ScrollbarAdapter {
    /** 当前滚动偏移量（以像素为单位，即距离内容开始处的距离）。 */
    val scrollOffset: Double

    /** 可滚动内容的总大小（以像素为单位）。 */
    val contentSize: Double

    /** 可见视口的大小（以像素为单位）。 */
    val viewportSize: Double

    /** 立即滚动到距离内容开始处 [scrollOffset] 像素的位置。 */
    suspend fun scrollTo(scrollOffset: Double)
}

internal val ScrollbarAdapter.maxScrollOffset: Double
    get() = (contentSize - viewportSize).coerceAtLeast(0.0)

/**
 * 懒加载布局的基础适配器，将内容组织为“行”
 *（垂直布局为行，水平布局为列）。
 *
 * 移植自 Compose Desktop v2 实现。相比简单的平均高度估计，主要改进如下：
 * - 正确考虑了 [lineSpacing] (`mainAxisItemSpacing`)
 * - 通过识别第一个“浮动”（非固定）项来处理粘性标题
 * - 对于较小的滚动距离使用 [scrollBy]，以避免在项目大小不均匀时产生跳变伪影
 */
internal abstract class LazyLineContentAdapter : ScrollbarAdapter {

    class VisibleLine(val index: Int, val offset: Int)

    protected abstract fun firstVisibleLine(): VisibleLine?
    protected abstract fun totalLineCount(): Int
    protected abstract fun contentPadding(): Int
    protected abstract suspend fun snapToLine(lineIndex: Int, scrollOffset: Int)
    protected abstract suspend fun scrollBy(value: Float)
    protected abstract fun averageVisibleLineSize(): Double
    protected abstract val lineSpacing: Int

    // derivedStateOf 确保重组仅跟踪实际对该派生值产生贡献的 lazy 状态值。
    private val averageVisibleLineSizeState by derivedStateOf {
        if (totalLineCount() == 0) 0.0 else averageVisibleLineSize()
    }

    private val averageVisibleLineSizeWithSpacing
        get() = averageVisibleLineSizeState + lineSpacing

    override val scrollOffset: Double
        get() {
            val firstVisibleLine = firstVisibleLine() ?: return 0.0
            return firstVisibleLine.index * averageVisibleLineSizeWithSpacing - firstVisibleLine.offset
        }

    override val contentSize: Double
        get() {
            val totalLineCount = totalLineCount()
            return averageVisibleLineSizeState * totalLineCount +
                lineSpacing * (totalLineCount - 1).coerceAtLeast(0) +
                contentPadding()
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val distance = scrollOffset - this.scrollOffset
        // 对于较短的距离，使用 scrollBy 以避免项目大小不均匀时产生视觉跳变；
        // 对于较长的距离，直接 snap 以避免重新构建所有中间项。
        if (abs(distance) <= viewportSize) {
            scrollBy(distance.toFloat())
        } else {
            snapTo(scrollOffset)
        }
    }

    private suspend fun snapTo(scrollOffset: Double) {
        val coerced = scrollOffset.coerceIn(0.0, maxScrollOffset)
        val index = (coerced / averageVisibleLineSizeWithSpacing)
            .toInt().coerceAtLeast(0).coerceAtMost(totalLineCount() - 1)
        val offset = (coerced - index * averageVisibleLineSizeWithSpacing)
            .toInt().coerceAtLeast(0)
        snapToLine(lineIndex = index, scrollOffset = offset)
    }
}

// ── 具体内部适配器 ───────────────────────────────────────────────────────────

private class LazyListAdapter(
    private val state: LazyListState,
) : LazyLineContentAdapter() {

    override val viewportSize: Double
        get() = (state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset).toDouble()

    /**
     * 返回 `visibleItemsInfo` 中第一个非固定项的索引。
     * 粘性标题与其邻居之间的索引/偏移量连续性会断开，因此我们跳过它。
     */
    private fun firstFloatingVisibleItemIndex() = with(state.layoutInfo.visibleItemsInfo) {
        when (size) {
            0 -> null
            1 -> 0
            else -> {
                val first = this[0]; val second = this[1]
                if (first.index < second.index - 1 ||
                    first.offset + first.size + lineSpacing > second.offset
                ) 1 else 0
            }
        }
    }

    override fun firstVisibleLine(): VisibleLine? {
        val idx = firstFloatingVisibleItemIndex() ?: return null
        val item = state.layoutInfo.visibleItemsInfo[idx]
        return VisibleLine(item.index, item.offset)
    }

    override fun totalLineCount() = state.layoutInfo.totalItemsCount

    override fun contentPadding() =
        with(state.layoutInfo) { beforeContentPadding + afterContentPadding }

    override suspend fun snapToLine(lineIndex: Int, scrollOffset: Int) =
        state.scrollToItem(lineIndex, scrollOffset)

    override suspend fun scrollBy(value: Float) { state.scrollBy(value) }

    override fun averageVisibleLineSize(): Double {
        val items = state.layoutInfo.visibleItemsInfo
        val firstIdx = firstFloatingVisibleItemIndex() ?: return 0.0
        val first = items[firstIdx]; val last = items.last()
        val count = items.size - firstIdx
        return (last.offset + last.size - first.offset - (count - 1) * lineSpacing).toDouble() / count
    }

    override val lineSpacing get() = state.layoutInfo.mainAxisItemSpacing
}

private class LazyGridAdapter(
    private val state: LazyGridState,
) : LazyLineContentAdapter() {

    override val viewportSize: Double
        get() = (state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset).toDouble()

    // 从共享相同 y 偏移的项目推断列数。
    // 这避免了访问内部的 `slotsPerLine` 属性。
    private val slotsPerLine: Int
        get() {
            val visible = state.layoutInfo.visibleItemsInfo
            val firstKnownY = visible.firstOrNull { it.row >= 0 }?.offset?.y ?: return 1
            return visible.count { it.offset.y == firstKnownY }.coerceAtLeast(1)
        }

    private fun lineOfIndex(index: Int) = index / slotsPerLine
    private fun indexOfFirstInLine(line: Int) = line * slotsPerLine

    override fun firstVisibleLine(): VisibleLine? =
        // row < 0 表示该项正在进行退出动画 —— 跳过它
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.row >= 0 }
            ?.let { VisibleLine(it.row, it.offset.y) }

    override fun totalLineCount(): Int {
        val count = state.layoutInfo.totalItemsCount
        return if (count == 0) 0 else lineOfIndex(count - 1) + 1
    }

    override fun contentPadding() =
        with(state.layoutInfo) { beforeContentPadding + afterContentPadding }

    override suspend fun snapToLine(lineIndex: Int, scrollOffset: Int) =
        state.scrollToItem(indexOfFirstInLine(lineIndex), scrollOffset)

    override suspend fun scrollBy(value: Float) { state.scrollBy(value) }

    override fun averageVisibleLineSize(): Double {
        val items = state.layoutInfo.visibleItemsInfo
        val firstKnown = items.indexOfFirst { it.row >= 0 }
        if (firstKnown == -1) return 0.0
        val real = items.subList(firstKnown, items.size)
        val lastRow = real.last().row
        // 最后一行可能具有不同高度的项目；取最高的一个。
        val lastLineSize = real.asReversed().asSequence()
            .takeWhile { it.row == lastRow }
            .maxOf { it.size.height }
        val first = real.first(); val last = real.last()
        val lineCount = last.row - first.row + 1
        return (last.offset.y + lastLineSize - first.offset.y - (lineCount - 1) * lineSpacing)
            .toDouble() / lineCount
    }

    override val lineSpacing get() = state.layoutInfo.mainAxisItemSpacing
}

private class LazyStaggeredGridAdapter(
    private val state: LazyStaggeredGridState,
) : LazyLineContentAdapter() {

    override val viewportSize: Double
        get() = (state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset).toDouble()

    private fun firstFloatingVisibleItemIndex() = with(state.layoutInfo.visibleItemsInfo) {
        when (size) {
            0 -> null
            1 -> 0
            else -> {
                val first = this[0]; val second = this[1]
                if (first.index < second.index - 1 ||
                    first.offset.y + first.size.height + lineSpacing > second.offset.y
                ) 1 else 0
            }
        }
    }

    override fun firstVisibleLine(): VisibleLine? {
        val idx = firstFloatingVisibleItemIndex() ?: return null
        val item = state.layoutInfo.visibleItemsInfo[idx]
        return VisibleLine(item.index, item.offset.y)
    }

    override fun totalLineCount() = state.layoutInfo.totalItemsCount

    override fun contentPadding() =
        with(state.layoutInfo) { beforeContentPadding + afterContentPadding }

    override suspend fun snapToLine(lineIndex: Int, scrollOffset: Int) =
        state.scrollToItem(lineIndex, scrollOffset)

    override suspend fun scrollBy(value: Float) { state.scrollBy(value) }

    override fun averageVisibleLineSize(): Double {
        val items = state.layoutInfo.visibleItemsInfo
        val firstIdx = firstFloatingVisibleItemIndex() ?: return 0.0
        val first = items[firstIdx]; val last = items.last()
        val count = items.size - firstIdx
        return (last.offset.y + last.size.height - first.offset.y - (count - 1) * lineSpacing)
            .toDouble() / count
    }

    override val lineSpacing get() = state.layoutInfo.mainAxisItemSpacing
}

// ── 公共状态接口 ─────────────────────────────────────────────────────────────

/**
 * 垂直滚动条的状态接口。类似于 [androidx.compose.foundation.gestures.ScrollableState]，
 * 这是每个滚动条状态必须履行的基础合约，无论底层列表是懒加载还是全量组合。
 *
 * 非懒加载容器（例如 [androidx.compose.foundation.ScrollState]）直接实现此接口；
 * 懒加载容器扩展自 [LazyScrollbarState]。
 */
@Stable
interface ScrollbarState {
    val isScrollInProgress: Boolean

    /** 计算当前滑块指标。应在 [derivedStateOf] 块内调用。 */
    fun computeMetrics(): ScrollbarMetrics

    /** 滚动视口，使其对应于 [fraction]（0f = 顶部，1f = 底部）。 */
    suspend fun scrollToFraction(fraction: Float)
}

/**
 * 由懒加载布局支撑的滚动条状态基类。
 *
 * 将滚动位置和内容大小的计算委托给内部的 [LazyLineContentAdapter]（移植自 Compose Desktop v2 实现）。
 * 这提供了能够正确处理行间距、内容内边距、粘性标题以及平滑与即时滚动的准确指标 —— 
 * 简单的平均高度估计无法正确处理这些情况。
 *
 * 构造函数是 `internal` 的，因此只有此模块中的具体子类可以扩展此类，从而将适配器作为实现细节。
 *
 * 具体子类：
 * - [LazyListScrollbarState] – 包装 [LazyListState]
 * - [LazyGridScrollbarState] – 包装 [LazyGridState]
 * - [LazyStaggeredGridScrollbarState] – 包装 [LazyStaggeredGridState]
 */
@Stable
abstract class LazyScrollbarState internal constructor(
    private val adapter: ScrollbarAdapter,
) : ScrollbarState {

    override fun computeMetrics(): ScrollbarMetrics {
        val contentSize = adapter.contentSize
        val viewportSize = adapter.viewportSize
        if (contentSize <= 0.0 || viewportSize <= 0.0 || contentSize <= viewportSize) {
            return ScrollbarMetrics(0f, 1f)
        }
        val thumbSizeFraction = (viewportSize / contentSize).toFloat()
            .coerceIn(MIN_THUMB_SIZE_FRACTION, 1f)
        if (thumbSizeFraction >= 1f) return ScrollbarMetrics(0f, 1f)
        val maxScrollOffset = adapter.maxScrollOffset
        val thumbOffsetFraction = if (maxScrollOffset <= 0.0) 0f
        else ((adapter.scrollOffset / maxScrollOffset) * (1.0 - thumbSizeFraction))
            .toFloat().coerceIn(0f, 1f - thumbSizeFraction)
        return ScrollbarMetrics(thumbOffsetFraction, thumbSizeFraction)
    }

    override suspend fun scrollToFraction(fraction: Float) {
        adapter.scrollTo(fraction.toDouble() * adapter.maxScrollOffset)
    }
}

// ── 具体懒加载状态类 ─────────────────────────────────────────────────────────

@Stable
class LazyListScrollbarState(
    val lazyListState: LazyListState,
) : LazyScrollbarState(LazyListAdapter(lazyListState)) {

    override val isScrollInProgress: Boolean
        get() = lazyListState.isScrollInProgress
}

@Stable
class LazyGridScrollbarState(
    val lazyGridState: LazyGridState,
) : LazyScrollbarState(LazyGridAdapter(lazyGridState)) {

    override val isScrollInProgress: Boolean
        get() = lazyGridState.isScrollInProgress
}

@Stable
class LazyStaggeredGridScrollbarState(
    val lazyStaggeredGridState: LazyStaggeredGridState,
) : LazyScrollbarState(LazyStaggeredGridAdapter(lazyStaggeredGridState)) {

    override val isScrollInProgress: Boolean
        get() = lazyStaggeredGridState.isScrollInProgress
}

// ── 工厂助手 ─────────────────────────────────────────────────────────────────

@Composable
fun rememberLazyListScrollbarState(lazyListState: LazyListState): LazyListScrollbarState =
    remember(lazyListState) { LazyListScrollbarState(lazyListState) }

@Composable
fun rememberLazyGridScrollbarState(lazyGridState: LazyGridState): LazyGridScrollbarState =
    remember(lazyGridState) { LazyGridScrollbarState(lazyGridState) }

@Composable
fun rememberLazyStaggeredGridScrollbarState(
    lazyStaggeredGridState: LazyStaggeredGridState,
): LazyStaggeredGridScrollbarState =
    remember(lazyStaggeredGridState) { LazyStaggeredGridScrollbarState(lazyStaggeredGridState) }
