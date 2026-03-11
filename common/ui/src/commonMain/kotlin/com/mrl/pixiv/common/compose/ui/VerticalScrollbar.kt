package com.mrl.pixiv.common.compose.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
private fun ScrollbarImpl(
    metrics: ScrollbarMetrics,
    isScrollInProgress: Boolean,
    onScrollToFraction: suspend (Float) -> Unit,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    if (metrics.thumbSizeFraction >= 1f) return

    val scope = rememberCoroutineScope()

    // 跟踪活动的拖动交互，以便在销毁时取消它，并通过 interactionSource 将其暴露给调用者（与 ScrollableState / Compose Desktop 行为一致）。
    val dragInteraction = remember { mutableStateOf<DragInteraction.Start?>(null) }
    DisposableEffect(interactionSource) {
        onDispose {
            dragInteraction.value?.let { interaction ->
                interactionSource.tryEmit(DragInteraction.Cancel(interaction))
                dragInteraction.value = null
            }
        }
    }

    val isHovered by interactionSource.collectIsHoveredAsState()
    val isDragging by remember { derivedStateOf { dragInteraction.value != null } }
    val isHighlighted = isScrollInProgress || isDragging || isHovered

    val color by animateColorAsState(
        targetValue = if (isHighlighted) style.hoverColor else style.unhoverColor,
        animationSpec = TweenSpec(durationMillis = style.hoverDurationMillis),
        label = "scrollbar_color",
    )

    // 计算显示空间的偏移量（已考虑 reverseLayout）并在正常滚动期间使其平滑；
    // 在拖动期间即时更新，以便滑块准确跟踪手指。
    val targetDisplayOffset = (if (reverseLayout) {
        1f - metrics.thumbOffsetFraction - metrics.thumbSizeFraction
    } else {
        metrics.thumbOffsetFraction
    }).coerceIn(0f, 1f)

    val smoothedDisplayOffset by animateFloatAsState(
        targetValue = targetDisplayOffset,
        animationSpec = if (isDragging) snap() else tween(durationMillis = 50),
        label = "scrollbar_thumb_offset",
    )

    val currentMetrics by rememberUpdatedState(metrics)
    val currentOnScroll by rememberUpdatedState(onScrollToFraction)

    var dragStartPointerY by remember { mutableFloatStateOf(0f) }
    // dragStartThumbOffset 处于滚动比例空间（而非显示空间），以便可以一致地应用 reverseLayout 增量反转。
    var dragStartThumbOffset by remember { mutableFloatStateOf(0f) }
    val scrollJobHolder = remember { object { var job: Job? = null } }

    // 仅在滚动条处于交互状态时启用指针输入：这避免了在滚动条完全透明时在移动设备上创建不可见的点击目标。
    val isPointerEnabled = isHovered || isScrollInProgress || isDragging

    Canvas(
        modifier = modifier
            .width(style.thickness)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .pointerInput(isPointerEnabled) {
                if (!isPointerEnabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val interaction = DragInteraction.Start()
                        scope.launch { interactionSource.emit(interaction) }
                        dragInteraction.value = interaction
                        dragStartPointerY = offset.y
                        dragStartThumbOffset = currentMetrics.thumbOffsetFraction
                    },
                    onDragEnd = {
                        dragInteraction.value?.let { interaction ->
                            scope.launch { interactionSource.emit(DragInteraction.Stop(interaction)) }
                        }
                        dragInteraction.value = null
                    },
                    onDragCancel = {
                        dragInteraction.value?.let { interaction ->
                            scope.launch { interactionSource.emit(DragInteraction.Cancel(interaction)) }
                        }
                        dragInteraction.value = null
                    },
                    onVerticalDrag = { change, _ ->
                        val delta = (change.position.y - dragStartPointerY) / size.height
                        // 在 reverseLayout 中取反 delta：向上拖动应增加滚动比例（移向反向列表的开头）。
                        val adjustedDelta = if (reverseLayout) -delta else delta
                        val thumbSizeFraction = currentMetrics.thumbSizeFraction
                        val newThumbOffset =
                            (dragStartThumbOffset + adjustedDelta).coerceIn(
                                0f,
                                (1f - thumbSizeFraction).coerceAtLeast(0f),
                            )
                        val scrollFraction = if (thumbSizeFraction >= 1f) 0f
                        else (newThumbOffset / (1f - thumbSizeFraction)).coerceIn(0f, 1f)
                        scrollJobHolder.job?.cancel()
                        scrollJobHolder.job = scope.launch { currentOnScroll(scrollFraction) }
                    },
                )
            }
    ) {
        if (color.alpha <= 0.01f) return@Canvas

        val minThumbPx = style.minimalHeight.toPx()
        val thumbHeight = maxOf(size.height * currentMetrics.thumbSizeFraction, minThumbPx)
        val thumbTop = size.height * smoothedDisplayOffset

        val outline = style.shape.createOutline(
            Size(size.width, thumbHeight),
            layoutDirection,
            this,
        )
        translate(top = thumbTop) {
            drawOutline(outline, color)
        }
    }
}

/**
 * 主要的 [VerticalScrollbar] 重载。接受任何 [ScrollbarState]，允许使用超出内置 lazy 变体的自定义状态实现。
 *
 * @param state 驱动滚动条的 [ScrollbarState]
 * @param modifier 应用于此 composable 的 modifier
 * @param reverseLayout 当列表以 `reverseLayout = true` 组合时设置为 `true`
 * @param style 视觉样式；默认为 [LocalScrollbarStyle]
 * @param interactionSource 接收 [DragInteraction] 事件的 [MutableInteractionSource]
 */
@Composable
fun VerticalScrollbar(
    state: ScrollbarState,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val metrics by remember(state) { derivedStateOf { state.computeMetrics() } }

    ScrollbarImpl(
        metrics = metrics,
        isScrollInProgress = state.isScrollInProgress,
        onScrollToFraction = { fraction -> state.scrollToFraction(fraction) },
        modifier = modifier,
        reverseLayout = reverseLayout,
        style = style,
        interactionSource = interactionSource,
    )
}

// ── 便捷重载 ─────────────────────────────────────────────────────────────

@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = VerticalScrollbar(rememberLazyListScrollbarState(state), modifier, reverseLayout, style, interactionSource)

@Composable
fun VerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = VerticalScrollbar(rememberLazyGridScrollbarState(state), modifier, reverseLayout, style, interactionSource)

@Composable
fun VerticalScrollbar(
    state: LazyStaggeredGridState,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    style: ScrollbarStyle = LocalScrollbarStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = VerticalScrollbar(rememberLazyStaggeredGridScrollbarState(state), modifier, reverseLayout, style, interactionSource)
