package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.mrl.pixiv.common.animation.DefaultAnimationDuration
import com.mrl.pixiv.common.animation.DefaultFloatAnimationSpec
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.PaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.PaneRole
import com.mrl.pixiv.common.compose.layout.SplitPaneState
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastExpanded
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PicturePaneResizeTest {
    @Test
    fun existingPicturePagerReceivesNarrowedAndRestoredPaneWidth() = checkPictureResize(
        initialWidth = 1200.dp,
        visitCommentPane = false,
    )

    @Test
    fun picturePagerStillReceivesResizesAfterMovingToAndFromACommentSplit() = checkPictureResize(
        initialWidth = 1500.dp,
        visitCommentPane = true,
    )

    private fun checkPictureResize(initialWidth: Dp, visitCommentPane: Boolean) {
        // The application's Tao dispatcher differs from the Swing EDT used by Compose UI tests.
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runDesktopComposeUiTest(width = 1580, height = 800, testTimeout = 30.seconds) {
                var containerWidth by mutableStateOf(initialWidth)
                var observedPane: PaneLayoutInfo? = null
                val record = NavigationRecord("picture", Destination.Picture(0, "resize-fixture", true))
                val backStack = mutableStateListOf(record)
                setContent {
                    MaterialTheme {
                        SharedTransitionLayout(Modifier.fillMaxSize()) {
                            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                                // Leave the host window at 1580 dp while changing only the page's
                                // container, so falling back to LocalWindowInfo cannot pass this test.
                                Box(Modifier.fillMaxSize()) {
                                    BoxWithConstraints(
                                        Modifier.width(containerWidth).fillMaxHeight()
                                            .testTag("resizing-container"),
                                    ) {
                                        val splitState = remember { SplitPaneState() }
                                        val inputState = remember { PaneInputState() }
                                        val paneTransitions = remember { AdaptivePaneTransitionState() }
                                        val strategy = remember(maxWidth, maxHeight) {
                                            AdaptiveSceneStrategy(
                                                maxWidth, maxHeight, splitState, inputState, paneTransitions,
                                            )
                                        }
                                        PredictiveBackNavDisplay(
                                            backStack = backStack,
                                            enableQuickBack = true,
                                            modifier = Modifier.fillMaxSize(),
                                            onBack = { backStack.removeLast() },
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            sceneStrategies = listOf(strategy),
                                            entryProvider = { entryRecord ->
                                                NavEntry(
                                                    key = entryRecord,
                                                    contentKey = entryRecord.entryId,
                                                    metadata = metadata { put(NavigationRecordKey, entryRecord) },
                                                ) {
                                                    // AdaptiveScene creates the production PaneHost
                                                    // around this shared navigation entry.
                                                    if (entryRecord.destination is Destination.Comment) {
                                                        Text("Comments", Modifier.testTag("comment-content"))
                                                    } else {
                                                        PicturePager { observedPane = it }
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                assertPicturePane(initialWidth, twoPane = true) { observedPane }
                if (visitCommentPane) {
                    runOnIdle {
                        backStack.add(
                            NavigationRecord(
                                "comments",
                                Destination.Comment(5, CommentType.ILLUST),
                                ownerEntryId = record.entryId,
                            ),
                        )
                    }
                    onNodeWithTag("comment-content").assertTextEquals("Comments")
                    assertPicturePane(
                        840.dp,
                        twoPane = true,
                        role = PaneRole.Source,
                        containerWidth = initialWidth,
                    ) { observedPane }
                    runOnIdle { backStack.removeLast() }
                    assertPicturePane(initialWidth, twoPane = true) { observedPane }
                }
                runOnIdle { containerWidth = 840.dp }
                assertPicturePane(840.dp, twoPane = true) { observedPane }
                runOnIdle { containerWidth = 839.dp }
                assertPicturePane(839.dp, twoPane = false) { observedPane }
                runOnIdle { containerWidth = 800.dp }
                assertPicturePane(800.dp, twoPane = false) { observedPane }
                runOnIdle { containerWidth = 500.dp }
                assertPicturePane(500.dp, twoPane = false) { observedPane }
                runOnIdle { containerWidth = initialWidth }
                assertPicturePane(initialWidth, twoPane = true) { observedPane }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun DesktopComposeUiTest.assertPicturePane(
        width: Dp,
        twoPane: Boolean,
        role: PaneRole = PaneRole.Single,
        containerWidth: Dp = width,
        observedPane: () -> PaneLayoutInfo?,
    ) {
        waitForIdle()
        onNodeWithTag("resizing-container").assertWidthIsEqualTo(containerWidth)
        onNodeWithTag("picture-content").assertWidthIsEqualTo(width)
        runOnIdle {
            val pane = assertNotNull(observedPane(), "Picture must receive pane information")
            assertEquals(width, pane.size.width, "Entry-local pane width must follow the resized container")
            assertEquals(twoPane, pane.sizeClass.isWidthAtLeastExpanded, "Picture's layout breakpoint must update")
            assertEquals(role, pane.role, "The same picture entry must receive its current pane role")
        }
        onNodeWithTag("picture-layout-mode").assertTextEquals(if (twoPane) "wide" else "compact")
    }
}

@Composable
private fun PicturePager(onPaneChanged: (PaneLayoutInfo) -> Unit) {
    // Match HorizontalSwipePictureScreen: its pager has no explicit sizing modifier.
    HorizontalPager(state = rememberPagerState { 1 }) {
        val pane = currentPaneLayoutInfo()
        SideEffect { onPaneChanged(pane) }
        val animatedScope = LocalNavAnimatedContentScope.current
        with(LocalSharedTransitionScope.current) {
            Scaffold(
                modifier = Modifier.fillMaxSize().sharedBounds(
                    rememberSharedContentState("resize-picture-card"),
                    animatedScope,
                    enter = fadeIn(DefaultFloatAnimationSpec),
                    exit = fadeOut(DefaultFloatAnimationSpec),
                    boundsTransform = { _, _ -> tween(DefaultAnimationDuration) },
                ),
            ) {
                Box(Modifier.fillMaxSize().testTag("picture-content")) {
                    Text(
                        if (pane.sizeClass.isWidthAtLeastExpanded) "wide" else "compact",
                        modifier = Modifier.testTag("picture-layout-mode"),
                    )
                }
            }
        }
    }
}
