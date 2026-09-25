package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mrl.pixiv.common.util.isAndroid
import com.mrl.pixiv.common.util.platform

// Classify completion only. Never delay rendering progress to classify a gesture.
private const val QuickBackDurationMillis = 120L

/** Keeps quick back completion separate from Nav3's seekable preview timeline. */
@Composable
internal fun <T : Any> PredictiveBackNavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    entryDecorators: List<NavEntryDecorator<T>> = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    sceneStrategies: List<SceneStrategy<T>>,
    sharedTransitionScope: SharedTransitionScope,
    enableQuickBack: Boolean = platform.isAndroid(),
    entryProvider: (T) -> NavEntry<T>,
) {
    require(backStack.isNotEmpty()) { "Navigation back stack cannot be empty" }
    // Keep all entry ownership outside the display's animation identity. In particular,
    // SceneState owns movable content, so a visible tablet source retains its remember state.
    val entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = sceneStrategies,
        sharedTransitionScope = sharedTransitionScope,
        onBack = onBack,
    )
    val scene = sceneState.currentScene
    val currentInfo = SceneInfo(scene)
    val backInfo = sceneState.previousScenes.map { SceneInfo(it) }
    val gestureState = rememberNavigationEventState(currentInfo, backInfo)
    var isLongGesture by remember { mutableStateOf(false) }
    var gestureSequence by remember { mutableIntStateOf(0) }
    var displaySequence by remember { mutableIntStateOf(0) }
    // Only Idle/InProgress changes matter to the timer. Let NavDisplay itself observe
    // individual progress events without recomposing this entry/history owner for each one.
    val inProgress by remember(gestureState) {
        derivedStateOf { gestureState.transitionState is InProgress }
    }
    val observedSequence = gestureSequence

    LaunchedEffect(inProgress, observedSequence, enableQuickBack) {
        if (inProgress && enableQuickBack) {
            val startedAt = withFrameMillis { it }
            do {
                val elapsed = withFrameMillis { it } - startedAt
            } while (elapsed < QuickBackDurationMillis)
            if (gestureSequence == observedSequence && gestureState.transitionState is InProgress) {
                isLongGesture = true
            }
        }
    }

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCancelled = {
            isLongGesture = false
            // Also cancel the classification timer if cancel/retry happen within one frame.
            gestureSequence++
        },
        onBackCompleted = {
            val popCount = (entries.size - scene.previousEntries.size).coerceAtLeast(0)
            if (enableQuickBack && !isLongGesture && popCount > 0) {
                // Initialize only the renderer at the committed scene. Changing a pop spec
                // alone would still wait for child/shared-entry animations to finish.
                displaySequence++
            }
            isLongGesture = false
            gestureSequence++
            repeat(popCount) { onBack() }
        },
    )

    key(displaySequence) {
        NavDisplay(
            sceneState = sceneState,
            // Seek from the first progress event. Waiting here produces a visible pause
            // followed by a jump to the finger's already advanced position.
            navigationEventState = gestureState,
            modifier = modifier,
        )
    }
}
