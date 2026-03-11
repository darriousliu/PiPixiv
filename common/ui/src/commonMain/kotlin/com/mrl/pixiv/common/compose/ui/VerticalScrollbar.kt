package com.mrl.pixiv.common.compose.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.util.isDesktop
import com.mrl.pixiv.common.util.platform
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val SCROLLBAR_WIDTH_DP = 10f
private const val SCROLLBAR_CORNER_RADIUS_DP = 4f
private const val SCROLLBAR_PADDING_DP = 2f
private const val MIN_THUMB_SIZE_FRACTION = 0.05f
private const val DESKTOP_ALPHA = 0.5f
private const val SCROLLING_ALPHA = 0.7f

private data class ScrollbarMetrics(
    val thumbOffsetFraction: Float,
    val thumbSizeFraction: Float,
)

private fun computeScrollbarMetrics(
    scrollOffset: Float,
    viewportHeight: Float,
    totalContentHeight: Float,
): ScrollbarMetrics {
    if (totalContentHeight <= 0f || viewportHeight <= 0f || totalContentHeight <= viewportHeight) {
        return ScrollbarMetrics(0f, 1f)
    }
    val thumbSizeFraction =
        (viewportHeight / totalContentHeight).coerceIn(MIN_THUMB_SIZE_FRACTION, 1f)
    if (thumbSizeFraction >= 1f) return ScrollbarMetrics(0f, 1f)
    val maxScrollOffset = totalContentHeight - viewportHeight
    val thumbOffsetFraction =
        ((scrollOffset / maxScrollOffset) * (1f - thumbSizeFraction)).coerceIn(
            0f,
            1f - thumbSizeFraction
        )
    return ScrollbarMetrics(thumbOffsetFraction, thumbSizeFraction)
}

@Composable
private fun ScrollbarImpl(
    metrics: ScrollbarMetrics,
    isScrollInProgress: Boolean,
    onScrollToFraction: suspend (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (metrics.thumbSizeFraction >= 1f) return

    val isDesktop = remember { platform.isDesktop() }
    val scope = rememberCoroutineScope()
    val thumbColor = LocalContentColor.current

    var isDragging by remember { mutableStateOf(false) }

    val targetAlpha = when {
        isDesktop -> DESKTOP_ALPHA
        isScrollInProgress || isDragging -> SCROLLING_ALPHA
        else -> 0f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(if (isScrollInProgress || isDragging || isDesktop) 150 else 800),
        label = "scrollbar_alpha"
    )

    // Smooth the thumb position during normal scroll to reduce jitter from unstable
    // avgItemHeight estimates; use snap() during drag so the thumb tracks the finger exactly.
    val smoothedThumbOffset by animateFloatAsState(
        targetValue = metrics.thumbOffsetFraction,
        animationSpec = if (isDragging) snap() else tween(durationMillis = 50),
        label = "scrollbar_thumb_offset"
    )

    val currentMetrics by rememberUpdatedState(metrics)
    val currentOnScroll by rememberUpdatedState(onScrollToFraction)

    var dragStartPointerY by remember { mutableFloatStateOf(0f) }
    var dragStartThumbOffset by remember { mutableFloatStateOf(0f) }
    // Holds the latest drag-scroll job so each new event cancels the previous one,
    // avoiding a queue of stale scrollToItem calls on fast drags.
    val scrollJobHolder = remember { object { var job: Job? = null } }

    // Only intercept pointer events when the scrollbar is visible. On mobile the
    // scrollbar is hidden (alpha == 0) when idle, so the modifier would create an
    // invisible 8 dp hit-target on the right edge that steals taps/drags.
    val isPointerEnabled = isDesktop || isScrollInProgress || isDragging

    Canvas(
        modifier = modifier
            .width((SCROLLBAR_WIDTH_DP + SCROLLBAR_PADDING_DP * 2).dp)
            .fillMaxHeight()
            .alpha(alpha)
            .pointerInput(isPointerEnabled) {
                if (!isPointerEnabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragStartPointerY = offset.y
                        dragStartThumbOffset = currentMetrics.thumbOffsetFraction
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        val delta =
                            (change.position.y - dragStartPointerY) / size.height
                        val thumbSizeFraction = currentMetrics.thumbSizeFraction
                        val newThumbOffset =
                            (dragStartThumbOffset + delta).coerceIn(
                                0f,
                                (1f - thumbSizeFraction).coerceAtLeast(0f)
                            )
                        val scrollFraction = if (thumbSizeFraction >= 1f) 0f
                        else (newThumbOffset / (1f - thumbSizeFraction)).coerceIn(0f, 1f)
                        // Cancel the previous job before launching a new one so only
                        // the latest drag position triggers a scroll (coalescing updates).
                        scrollJobHolder.job?.cancel()
                        scrollJobHolder.job = scope.launch { currentOnScroll(scrollFraction) }
                    }
                )
            }
    ) {
        if (alpha <= 0.01f) return@Canvas

        val thumbWidth = SCROLLBAR_WIDTH_DP.dp.toPx()
        val cornerRadius = SCROLLBAR_CORNER_RADIUS_DP.dp.toPx()
        val paddingX = SCROLLBAR_PADDING_DP.dp.toPx()
        val left = size.width - thumbWidth - paddingX

        val thumbHeight = size.height * currentMetrics.thumbSizeFraction
        val thumbTop = size.height * smoothedThumbOffset

        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(left, thumbTop),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        )
    }
}

@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val metrics by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty() || info.totalItemsCount == 0) {
                return@derivedStateOf ScrollbarMetrics(0f, 1f)
            }
            val avgItemHeight = run { var sum = 0L; for (item in visible) sum += item.size; sum.toFloat() / visible.size }
            val viewportHeight =
                (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            val totalHeight = info.totalItemsCount * avgItemHeight
            computeScrollbarMetrics(
                scrollOffset = state.firstVisibleItemIndex * avgItemHeight + state.firstVisibleItemScrollOffset,
                viewportHeight = viewportHeight,
                totalContentHeight = totalHeight,
            )
        }
    }

    ScrollbarImpl(
        metrics = metrics,
        isScrollInProgress = state.isScrollInProgress,
        onScrollToFraction = { fraction ->
            val info = state.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems > 0) {
                val avgItemHeight = run { val v = info.visibleItemsInfo; var sum = 0L; for (item in v) sum += item.size; sum.toFloat() / v.size }
                val viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val totalHeight = totalItems * avgItemHeight
                val targetOffset = (fraction * (totalHeight - viewportHeight)).coerceAtLeast(0f)
                val targetIndex = (targetOffset / avgItemHeight).toInt().coerceIn(0, totalItems - 1)
                val subOffset = (targetOffset - targetIndex * avgItemHeight).toInt().coerceAtLeast(0)
                state.scrollToItem(targetIndex, subOffset)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun VerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val metrics by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty() || info.totalItemsCount == 0) {
                return@derivedStateOf ScrollbarMetrics(0f, 1f)
            }
            val avgItemHeight = run { var sum = 0L; for (item in visible) sum += item.size.height; sum.toFloat() / visible.size }
            val viewportHeight =
                (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            // Estimate column count: items in the first row share the same y-offset
            val firstItemY = visible.first().offset.y
            val colCount = visible.count { it.offset.y == firstItemY }.coerceAtLeast(1)
            val totalRows = (info.totalItemsCount + colCount - 1) / colCount
            val totalHeight = totalRows * avgItemHeight
            val firstRow = state.firstVisibleItemIndex / colCount
            val scrollOffset = firstRow * avgItemHeight + state.firstVisibleItemScrollOffset
            computeScrollbarMetrics(
                scrollOffset = scrollOffset,
                viewportHeight = viewportHeight,
                totalContentHeight = totalHeight,
            )
        }
    }

    ScrollbarImpl(
        metrics = metrics,
        isScrollInProgress = state.isScrollInProgress,
        onScrollToFraction = { fraction ->
            val info = state.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems > 0) {
                val visible = info.visibleItemsInfo
                val avgItemHeight = run { var sum = 0L; for (item in visible) sum += item.size.height; sum.toFloat() / visible.size }
                val viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val firstItemY = visible.first().offset.y
                val colCount = visible.count { it.offset.y == firstItemY }.coerceAtLeast(1)
                val totalRows = (totalItems + colCount - 1) / colCount
                val totalHeight = totalRows * avgItemHeight
                val targetOffset = (fraction * (totalHeight - viewportHeight)).coerceAtLeast(0f)
                val targetRow = (targetOffset / avgItemHeight).toInt().coerceIn(0, totalRows - 1)
                val subOffset = (targetOffset - targetRow * avgItemHeight).toInt().coerceAtLeast(0)
                val targetIndex = (targetRow * colCount).coerceIn(0, totalItems - 1)
                state.scrollToItem(targetIndex, subOffset)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun VerticalScrollbar(
    state: LazyStaggeredGridState,
    modifier: Modifier = Modifier,
) {
    val metrics by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty() || info.totalItemsCount == 0) {
                return@derivedStateOf ScrollbarMetrics(0f, 1f)
            }
            val avgItemHeight = run { var sum = 0L; for (item in visible) sum += item.size.height; sum.toFloat() / visible.size }
            val viewportHeight =
                (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            // Estimate lane count from distinct x-offsets of visible items
            val laneCount = visible.distinctBy { it.offset.x }.size.coerceAtLeast(1)
            val estimatedRowCount = (info.totalItemsCount + laneCount - 1) / laneCount
            val totalHeight = estimatedRowCount * avgItemHeight
            val scrollOffset =
                (state.firstVisibleItemIndex / laneCount) * avgItemHeight + state.firstVisibleItemScrollOffset
            computeScrollbarMetrics(
                scrollOffset = scrollOffset,
                viewportHeight = viewportHeight,
                totalContentHeight = totalHeight,
            )
        }
    }

    ScrollbarImpl(
        metrics = metrics,
        isScrollInProgress = state.isScrollInProgress,
        onScrollToFraction = { fraction ->
            val info = state.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems > 0) {
                val visible = info.visibleItemsInfo
                val avgItemHeight = run { var sum = 0L; for (item in visible) sum += item.size.height; sum.toFloat() / visible.size }
                val viewportHeight = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                val laneCount = visible.distinctBy { it.offset.x }.size.coerceAtLeast(1)
                val estimatedRowCount = (totalItems + laneCount - 1) / laneCount
                val totalHeight = estimatedRowCount * avgItemHeight
                val targetOffset = (fraction * (totalHeight - viewportHeight)).coerceAtLeast(0f)
                val targetRow = (targetOffset / avgItemHeight).toInt().coerceIn(0, estimatedRowCount - 1)
                val subOffset = (targetOffset - targetRow * avgItemHeight).toInt().coerceAtLeast(0)
                val targetIndex = (targetRow * laneCount).coerceIn(0, totalItems - 1)
                state.scrollToItem(targetIndex, subOffset)
            }
        },
        modifier = modifier,
    )
}
