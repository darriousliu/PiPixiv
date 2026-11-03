package com.mrl.pixiv

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalPointerSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign

/** Positions and velocities are expressed in logical tab slots, independently of layout direction. */
@Stable
internal class LiquidNavigationMotion(
    initialIndex: Int,
    private val itemCount: Int,
    private val scope: CoroutineScope,
) {
    private var confirmedIndex = initialIndex.coerceIn(0, itemCount - 1)
    private var positionJob: Job? = null
    private var velocityDecayJob: Job? = null
    private var pressJob: Job? = null
    private var releaseJob: Job? = null
    private var panelOffsetJob: Job? = null
    private var pressGeneration = 0
    private var panelDragFraction by mutableFloatStateOf(0f)

    /** Physical drag direction, eased like Miuix's 4dp whole-dock rubber band. */
    val panelOffsetFraction: Float
        get() {
            val fraction = panelDragFraction.coerceIn(-1f, 1f)
            return fraction.sign * EaseOut.transform(abs(fraction))
        }

    var position by mutableFloatStateOf(confirmedIndex.toFloat())
        private set
    var velocity by mutableFloatStateOf(0f)
        private set
    var pressProgress by mutableFloatStateOf(0f)
        private set
    var isPressed by mutableStateOf(false)
        private set
    var isDragging by mutableStateOf(false)
        private set

    fun select(index: Int) {
        confirmedIndex = index.coerceIn(0, itemCount - 1)
        isDragging = false
        beginPress()
        settleAt(confirmedIndex)
        releaseJob = scope.launch {
            delay(110)
            releaseJob = null
            releasePress()
        }
    }

    fun synchronize(index: Int) {
        val nextIndex = index.coerceIn(0, itemCount - 1)
        if (nextIndex == confirmedIndex) return
        confirmedIndex = nextIndex
        if (!isDragging) settleAt(nextIndex)
    }

    fun cancel() {
        isDragging = false
        releasePress()
        settleAt(confirmedIndex)
    }

    internal fun beginPress(): Int {
        pressGeneration++
        releaseJob?.cancel()
        positionJob?.cancel()
        velocityDecayJob?.cancel()
        velocity = 0f
        isPressed = true
        animatePress(1f)
        return pressGeneration
    }

    internal fun cancelPress(generation: Int) {
        if (generation == pressGeneration) cancel()
    }

    internal fun beginDrag() {
        positionJob?.cancel()
        isDragging = true
    }

    internal fun dragTo(
        value: Float,
        slotsPerSecond: Float,
        physicalDeltaFraction: Float = 0f,
    ) {
        velocityDecayJob?.cancel()
        panelOffsetJob?.cancel()
        panelDragFraction += physicalDeltaFraction
        position = value.coerceIn(0f, (itemCount - 1).toFloat())
        velocity = if (position == value) slotsPerSecond else 0f
        val dragVelocity = velocity
        velocityDecayJob = scope.launch {
            delay(40)
            animate(
                initialValue = dragVelocity,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 100),
            ) { value, _ -> velocity = value }
            velocity = 0f
        }
    }

    internal fun finishDrag(): Int {
        val index = position.roundToInt().coerceIn(0, itemCount - 1)
        confirmedIndex = index
        isDragging = false
        releasePress()
        settleAt(index)
        return index
    }

    private fun releasePress() {
        releaseJob?.cancel()
        releaseJob = null
        isPressed = false
        animatePress(0f)
        returnPanelToRest()
    }

    private fun returnPanelToRest() {
        panelOffsetJob?.cancel()
        val start = panelDragFraction
        if (start == 0f) return
        panelOffsetJob = scope.launch {
            animate(
                initialValue = start,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = 300f,
                    visibilityThreshold = 0.001f,
                ),
            ) { value, _ -> panelDragFraction = value }
            panelDragFraction = 0f
        }
    }

    private fun animatePress(target: Float) {
        pressJob?.cancel()
        val start = pressProgress
        pressJob = scope.launch {
            animate(
                initialValue = start,
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
            ) { value, _ -> pressProgress = value.coerceIn(0f, 1f) }
            pressProgress = target
        }
    }

    private fun settleAt(index: Int) {
        positionJob?.cancel()
        velocityDecayJob?.cancel()
        val start = position
        val startVelocity = velocity.coerceIn(-12f, 12f)
        positionJob = scope.launch {
            animate(
                initialValue = start,
                targetValue = index.toFloat(),
                initialVelocity = startVelocity,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            ) { value, currentVelocity ->
                position = value.coerceIn(0f, (itemCount - 1).toFloat())
                velocity = if (position == value) currentVelocity else 0f
            }
            position = index.toFloat()
            velocity = 0f
        }
    }
}

/** Attach to the parent of equal-width tab slots; normal tab clicks remain with the children. */
internal fun Modifier.liquidNavigationGestures(
    motion: LiquidNavigationMotion,
    selectedIndex: Int,
    itemCount: Int,
    isRtl: Boolean,
    onSelect: (Int) -> Unit,
    onStep: () -> Unit,
): Modifier = composed {
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnStep by rememberUpdatedState(onStep)

    pointerInput(motion, itemCount, isRtl) {
        var activePress: Int? = null
        try {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val slotWidth = size.width.toFloat() / itemCount
                if (slotWidth <= 0f || down.position.x < 0f || down.position.x >= size.width) {
                    return@awaitEachGesture
                }
                val physicalIndex = floor(down.position.x / slotWidth).toInt()
                val downIndex = if (isRtl) itemCount - 1 - physicalIndex else physicalIndex
                if (downIndex != currentSelectedIndex) {
                    // Preserve taps, but cancel child clicks once an unselected tab is dragged.
                    val drag = awaitHorizontalPointerSlopOrCancellation(
                        down.id,
                        down.type,
                    ) { change, _ -> change.consume() }
                    if (drag != null) horizontalDrag(drag.id) { it.consume() }
                    return@awaitEachGesture
                }

                val direction = if (isRtl) -1f else 1f
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                // A tap's spring may still be travelling from a previous page when the next drag starts.
                var dragPosition = downIndex.toFloat()
                var feedbackIndex = downIndex
                val press = motion.beginPress()
                activePress = press

                fun updateDrag(physicalDelta: Float) {
                    val requestedPosition = dragPosition
                    dragPosition = requestedPosition.coerceIn(0f, (itemCount - 1).toFloat())
                    motion.dragTo(
                        value = requestedPosition,
                        slotsPerSecond = tracker.calculateVelocity().x / slotWidth * direction,
                        physicalDeltaFraction = physicalDelta / size.width,
                    )
                    // Separate forward and backward thresholds prevent repeated feedback at a boundary.
                    if (abs(motion.position - feedbackIndex) >= 0.58f) {
                        val nextIndex = motion.position.roundToInt().coerceIn(0, itemCount - 1)
                        if (nextIndex != feedbackIndex) {
                            feedbackIndex = nextIndex
                            currentOnStep()
                        }
                    }
                }

                val drag =
                    awaitHorizontalPointerSlopOrCancellation(down.id, down.type) { change, _ ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val physicalDelta = change.position.x - down.position.x
                        dragPosition += physicalDelta / slotWidth * direction
                        motion.beginDrag()
                        updateDrag(physicalDelta)
                    }
                if (drag == null) {
                    // A child click can start a new pulse on this same UP event.
                    motion.cancelPress(press)
                    activePress = null
                    return@awaitEachGesture
                }

                val completed = horizontalDrag(drag.id) { change ->
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val physicalDelta = change.positionChange().x
                    dragPosition += physicalDelta / slotWidth * direction
                    updateDrag(physicalDelta)
                    change.consume()
                }
                if (completed) {
                    val index = motion.finishDrag()
                    activePress = null
                    if (index != feedbackIndex) currentOnStep()
                    if (index != currentSelectedIndex) currentOnSelect(index)
                } else {
                    motion.cancelPress(press)
                    activePress = null
                }
            }
        } finally {
            activePress?.let(motion::cancelPress)
        }
    }
}
