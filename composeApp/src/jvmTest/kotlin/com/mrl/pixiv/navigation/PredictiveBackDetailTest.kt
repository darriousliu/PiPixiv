package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.SplitPaneState
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PredictiveBackDetailTest {
    @Test
    fun returningToThePreviousDetailKeepsTheOutgoingDetailOpaque() = checkDetailBack(hasPreviousDetail = true)

    @Test
    fun closingTheLastDetailPreservesTheExpandingSourcePreview() = checkDetailBack(hasPreviousDetail = false)

    @Test
    fun quicklyReturningToThePreviousDetailSkipsSettlingAndKeepsSourceState() =
        checkQuickDetailBack(hasPreviousDetail = true, edge = NavigationEvent.EDGE_LEFT)

    @Test
    fun quicklyClosingTheLastDetailSkipsSettlingAndKeepsSourceState() =
        checkQuickDetailBack(hasPreviousDetail = false, edge = NavigationEvent.EDGE_RIGHT)

    @Test
    fun closingTheLastDetailStartsExpandingImmediatelyAndCancellationRestoresIt() {
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runDesktopComposeUiTest(width = DetailTestWidth, height = 800, testTimeout = 30.seconds) {
                mainClock.autoAdvance = false
                val fixture = DetailBackFixture(hasPreviousDetail = false)
                val originalStack = fixture.backStack.toList()
                try {
                    setContent { fixture.Content() }
                    settle()
                    val originalSourceWidth = sourcePaintedWidth()

                    startBack(fixture, NavigationEvent.EDGE_LEFT)
                    runOnIdle {
                        fixture.input.backProgressed(
                            NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.3f),
                        )
                    }
                    repeat(3) {
                        mainClock.advanceTimeByFrame()
                        waitForIdle()
                    }
                    // 到 64 ms 时预览就须跟随手指，不能等待额外的激活延迟。
                    assertTrue(
                        sourcePaintedWidth() > originalSourceWidth,
                        "The source must visibly expand during the first frames of the gesture",
                    )
                    assertOpaquePanes(NetworkDetailArgb, allowsSourceExpansion = true)
                    runOnIdle {
                        assertEquals(originalStack, fixture.backStack.toList())
                        assertEquals(0, fixture.backCalls)
                        fixture.input.backCancelled()
                    }
                    settle()
                    assertPanes(NetworkDetailArgb, "after cancelling the immediate preview")
                    assertEquals(originalSourceWidth, sourcePaintedWidth())
                    runOnIdle {
                        assertEquals(originalStack, fixture.backStack.toList())
                        assertEquals(0, fixture.backCalls)
                    }
                } finally {
                    runOnIdle { fixture.navigationEventDispatcher.dispose() }
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun checkQuickDetailBack(hasPreviousDetail: Boolean, edge: Int) {
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runDesktopComposeUiTest(width = DetailTestWidth, height = 800, testTimeout = 30.seconds) {
                mainClock.autoAdvance = false
                val fixture = DetailBackFixture(hasPreviousDetail)
                val originalStack = fixture.backStack.toList()
                val outgoingColor = if (hasPreviousDetail) BrowsingDetailArgb else NetworkDetailArgb
                try {
                    setContent { fixture.Content() }
                    settle()
                    val rememberedState = requireNotNull(fixture.sourceRememberedState)
                    val saveableState = requireNotNull(fixture.sourceSaveableState)
                    runOnIdle {
                        rememberedState.value = 7
                        saveableState.value = 11
                    }
                    mainClock.advanceTimeByFrame()
                    waitForIdle()
                    startBack(fixture, edge)
                    runOnIdle {
                        fixture.input.backProgressed(NavigationEvent(swipeEdge = edge, progress = 0.15f))
                    }
                    mainClock.advanceTimeByFrame()
                    waitForIdle()
                    assertOpaquePanes(outgoingColor, allowsSourceExpansion = !hasPreviousDetail)
                    runOnIdle {
                        assertEquals(originalStack, fixture.backStack.toList())
                        assertEquals(0, fixture.backCalls)
                        fixture.input.backCompleted()
                    }

                    // 短促手势须及时完成，不能等待预览动画的剩余时间。
                    repeat(3) {
                        mainClock.advanceTimeByFrame()
                        waitForIdle()
                    }
                    assertPanes(if (hasPreviousDetail) NetworkDetailArgb else DetailSourceArgb, "after a quick return")
                    onNodeWithTag(originalStack.last().entryId).assertDoesNotExist()
                    runOnIdle {
                        assertEquals(originalStack.dropLast(1), fixture.backStack.toList())
                        assertEquals(1, fixture.backCalls)
                        assertSame(rememberedState, fixture.sourceRememberedState, "The source's remember state must survive")
                        assertSame(saveableState, fixture.sourceSaveableState, "The source must remain in composition")
                        assertEquals(7, fixture.sourceRememberedState?.value)
                        assertEquals(11, fixture.sourceSaveableState?.value)
                        assertEquals(0, fixture.sourceDisposals, "Returning must not dispose and recreate the source")
                    }
                    if (!hasPreviousDetail) {
                        onNodeWithTag("detail-test-source").assertWidthIsEqualTo(DetailTestWidth.dp)
                    }
                } finally {
                    runOnIdle { fixture.navigationEventDispatcher.dispose() }
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun checkDetailBack(hasPreviousDetail: Boolean) {
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runDesktopComposeUiTest(width = DetailTestWidth, height = 800, testTimeout = 30.seconds) {
                mainClock.autoAdvance = false
                val fixture = DetailBackFixture(hasPreviousDetail)
                val originalStack = fixture.backStack.toList()
                val outgoingColor = if (hasPreviousDetail) BrowsingDetailArgb else NetworkDetailArgb
                try {
                    setContent { fixture.Content() }
                    settle()
                    assertPanes(outgoingColor)
                    startBack(fixture, NavigationEvent.EDGE_LEFT)
                    for (progress in listOf(0.6f, 0.9f, 1f)) {
                        holdProgress(fixture, NavigationEvent.EDGE_LEFT, progress)
                        // 关闭最后一个详情面板时，共享的来源页面应展开并覆盖详情。
                        val expectedRight = when {
                            hasPreviousDetail -> outgoingColor
                            progress == 1f -> DetailSourceArgb
                            else -> null
                        }
                        assertPanes(expectedRight, "while holding progress $progress")
                        runOnIdle {
                            assertEquals(originalStack, fixture.backStack.toList(), "Holding must not mutate the stack")
                            assertEquals(0, fixture.backCalls)
                        }
                    }

                    runOnIdle { fixture.input.backCancelled() }
                    settle()
                    assertPanes(outgoingColor, "after cancellation")
                    runOnIdle {
                        assertEquals(originalStack, fixture.backStack.toList())
                        assertEquals(0, fixture.backCalls)
                    }

                    startBack(fixture, NavigationEvent.EDGE_RIGHT)
                    holdProgress(fixture, NavigationEvent.EDGE_RIGHT, 1f)
                    assertPanes(if (hasPreviousDetail) outgoingColor else DetailSourceArgb, "before completion")
                    runOnIdle { fixture.input.backCompleted() }
                    settle()
                    assertPanes(if (hasPreviousDetail) NetworkDetailArgb else DetailSourceArgb, "after completion")
                    runOnIdle {
                        assertEquals(originalStack.dropLast(1), fixture.backStack.toList())
                        assertEquals(1, fixture.backCalls, "Only explicit completion may pop once")
                    }
                    if (!hasPreviousDetail) {
                        onNodeWithTag("detail-test-source").assertWidthIsEqualTo(DetailTestWidth.dp)
                    }
                } finally {
                    runOnIdle { fixture.navigationEventDispatcher.dispose() }
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun DesktopComposeUiTest.startBack(fixture: DetailBackFixture, edge: Int) {
        runOnIdle { fixture.input.backStarted(NavigationEvent(swipeEdge = edge)) }
        mainClock.advanceTimeByFrame()
        waitForIdle()
    }

    private fun DesktopComposeUiTest.holdProgress(fixture: DetailBackFixture, edge: Int, progress: Float) {
        runOnIdle { fixture.input.backProgressed(NavigationEvent(swipeEdge = edge, progress = progress)) }
        // 即使等待超过转场时长，只要用户仍按住手指，页面就不能淡出。
        mainClock.advanceTimeBy(1_200)
        waitForIdle()
    }

    private fun DesktopComposeUiTest.settle() {
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun DesktopComposeUiTest.assertPanes(expectedRight: Int?, phase: String = "initially") {
        val image = captureToImage().toAwtImage()
        assertEquals(DetailSourceArgb, image.getRGB(100, 400), "The source must stay visible $phase")
        if (expectedRight != null) {
            assertEquals(expectedRight, image.getRGB(1_000, 400), "The right side must show the expected opaque page $phase")
        }
    }

    private fun DesktopComposeUiTest.assertOpaquePanes(outgoingColor: Int, allowsSourceExpansion: Boolean) {
        val image = captureToImage().toAwtImage()
        assertEquals(DetailSourceArgb, image.getRGB(100, 400), "The source must stay opaque during preview")
        val rightColor = image.getRGB(1_000, 400)
        assertTrue(
            rightColor == outgoingColor || (allowsSourceExpansion && rightColor == DetailSourceArgb),
            "The detail must stay opaque until covered by the expanding source",
        )
    }

    private fun DesktopComposeUiTest.sourcePaintedWidth(): Int {
        val image = captureToImage().toAwtImage()
        return (0 until image.width).count { image.getRGB(it, 400) == DetailSourceArgb }
    }
}

private const val DetailTestWidth = 1_200
private const val DetailSourceArgb = 0xff286bc5.toInt()
private const val NetworkDetailArgb = 0xff40aa71.toInt()
private const val BrowsingDetailArgb = 0xffcc3452.toInt()

private class DetailBackFixture(hasPreviousDetail: Boolean) : NavigationEventDispatcherOwner {
    override val navigationEventDispatcher = NavigationEventDispatcher()
    val input = DirectNavigationEventInput().also(navigationEventDispatcher::addInput)
    private val source = NavigationRecord("detail-test-source", Destination.Setting)
    private val network = NavigationRecord("detail-test-network", Destination.NetworkSetting, source.entryId)
    private val browsing = NavigationRecord("detail-test-browsing", Destination.BrowsingSetting, source.entryId)
    val backStack = mutableStateListOf(source, network).apply {
        if (hasPreviousDetail) add(browsing)
    }
    var backCalls = 0
    var sourceRememberedState: MutableState<Int>? = null
        private set
    var sourceSaveableState: MutableState<Int>? = null
        private set
    var sourceDisposals = 0
        private set

    @Composable
    fun Content() {
        MaterialTheme {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this,
                    LocalNavigationEventDispatcherOwner provides this@DetailBackFixture,
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val splitState = remember { SplitPaneState() }
                        val inputState = remember { PaneInputState() }
                        val transitions = remember { AdaptivePaneTransitionState(enablePredictiveBackCard = true) }
                        val strategy = remember(maxWidth, maxHeight) {
                            AdaptiveSceneStrategy(maxWidth, maxHeight, splitState, inputState, transitions)
                        }
                        PredictiveBackNavDisplay(
                            backStack = backStack,
                            enableQuickBack = true,
                            modifier = Modifier.fillMaxSize(),
                            sharedTransitionScope = this@SharedTransitionLayout,
                            sceneStrategies = listOf(strategy),
                            onBack = {
                                backCalls++
                                backStack.removeLast()
                            },
                            entryProvider = { record ->
                                NavEntry(
                                    key = record,
                                    contentKey = record.entryId,
                                    metadata = metadata { put(NavigationRecordKey, record) },
                                ) {
                                    if (record == source) {
                                        val rememberedState = remember { mutableStateOf(0) }
                                        val saveableState = rememberSaveable { mutableStateOf(0) }
                                        SideEffect {
                                            sourceRememberedState = rememberedState
                                            sourceSaveableState = saveableState
                                        }
                                        DisposableEffect(Unit) {
                                            onDispose { sourceDisposals++ }
                                        }
                                    }
                                    val color = when (record) {
                                        source -> DetailSourceArgb
                                        network -> NetworkDetailArgb
                                        else -> BrowsingDetailArgb
                                    }
                                    Box(Modifier.fillMaxSize().background(Color(color)).testTag(record.entryId))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
