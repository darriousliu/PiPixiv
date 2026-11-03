package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.mrl.pixiv.common.compose.LocalNavigationSharedContentEnabled
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.SplitPaneState
import com.mrl.pixiv.common.compose.rememberNavigationSharedContentState
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PredictiveBackCardTest {
    @Test
    fun sameColoredLightPagesStaySeparatedUntilPreviewEnds() = checkSameColorPages(
        pageColor = CardSameLightArgb,
        colorScheme = lightColorScheme(background = Color(CardSameLightArgb)),
    )

    @Test
    fun sameColoredDarkPagesStaySeparatedUntilPreviewEnds() = checkSameColorPages(
        pageColor = CardSameDarkArgb,
        colorScheme = darkColorScheme(background = Color(CardSameDarkArgb)),
    )

    private fun checkSameColorPages(pageColor: Int, colorScheme: ColorScheme) = withFixture(
        rootColor = pageColor,
        detailColor = pageColor,
        colorScheme = colorScheme,
    ) { fixture ->
        assertFullPage(pageColor)
        previewBack(fixture, NavigationEvent.EDGE_LEFT)
        var exposedColor: Int? = null
        for (progress in listOf(0.6f, 0.9f, 1f)) {
            holdProgress(fixture, NavigationEvent.EDGE_LEFT, progress)
            exposedColor = assertSameColorPreview(pageColor, NavigationEvent.EDGE_LEFT, exposedColor)
            runOnIdle {
                assertEquals(0, fixture.backCalls)
                assertEquals(false, fixture.sharedEnabled["detail"])
                assertEquals(false, fixture.sharedEnabled["root"])
            }
        }

        // 从 100% 进度往回拖动时，两层页面仍须清晰区分，前景不能淡出。
        holdProgress(fixture, NavigationEvent.EDGE_LEFT, 0.25f)
        assertSameColorPreview(pageColor, NavigationEvent.EDGE_LEFT, exposedColor)
        runOnIdle { fixture.input.backCancelled() }
        settle()
        assertFullPage(pageColor)
        runOnIdle {
            assertEquals(listOf(fixture.root, fixture.detail), fixture.backStack.toList())
            assertEquals(0, fixture.backCalls)
            assertEquals(true, fixture.sharedEnabled["detail"])
            assertFalse(fixture.sharedScope.isTransitionActive)
        }

        // 正常完成返回时，卡片消失后必须清除目标页面的蒙层。
        previewBack(fixture, NavigationEvent.EDGE_RIGHT)
        holdProgress(fixture, NavigationEvent.EDGE_RIGHT, 1f)
        assertSameColorPreview(pageColor, NavigationEvent.EDGE_RIGHT, exposedColor)
        runOnIdle { fixture.input.backCompleted() }
        settle()
        assertFullPage(pageColor)
        onNodeWithTag("detail").assertDoesNotExist()
        runOnIdle {
            assertEquals(1, fixture.backCalls)
            assertEquals(true, fixture.sharedEnabled["root"])
            assertFalse(fixture.sharedScope.isTransitionActive)
            fixture.backStack.add(fixture.detail)
        }
        settle()
        assertFullPage(pageColor)

        // 快速完成返回时也须立即清除蒙层，并恢复共享元素动画。
        runOnIdle {
            fixture.input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT))
            fixture.input.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.9f))
        }
        mainClock.advanceTimeBy(48)
        waitForIdle()
        assertSameColorPreview(pageColor, NavigationEvent.EDGE_LEFT, exposedColor)
        runOnIdle { fixture.input.backCompleted() }
        advanceCommitFrames()
        assertFullPage(pageColor)
        onNodeWithTag("detail").assertDoesNotExist()
        runOnIdle {
            assertEquals(listOf(fixture.root), fixture.backStack.toList())
            assertEquals(2, fixture.backCalls)
            assertEquals(true, fixture.sharedEnabled["root"])
            assertFalse(fixture.sharedScope.isTransitionActive)
        }
    }

    @Test
    fun previewFollowsProgressBeforeTheQuickBackWindowEnds() = withFixture { fixture ->
        runOnIdle {
            fixture.input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT))
            fixture.input.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.2f))
        }
        mainClock.advanceTimeBy(48)
        waitForIdle()
        val earlyWidth = onNodeWithTag("detail").fetchSemanticsNode().boundsInRoot.width
        assertEquals(CardWidth * 0.98f, earlyWidth, 0.5f, "The first progress must render without waiting 120 ms")
        assertHeldForeground(0.2f, NavigationEvent.EDGE_LEFT)
        runOnIdle {
            assertEquals(0, fixture.backCalls)
            fixture.input.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.4f))
        }
        mainClock.advanceTimeBy(48)
        waitForIdle()
        val nextWidth = onNodeWithTag("detail").fetchSemanticsNode().boundsInRoot.width
        assertEquals(CardWidth * 0.96f, nextWidth, 0.5f, "Later progress must also remain in sync")
        // 手指保持不动时，跨过快速返回的判定时限不能导致画面跳变。
        mainClock.advanceTimeBy(96)
        waitForIdle()
        assertEquals(nextWidth, onNodeWithTag("detail").fetchSemanticsNode().boundsInRoot.width, 0.5f)
        runOnIdle { fixture.input.backCancelled() }
        settle()
        assertFullPage(CardDetailArgb)
        runOnIdle { assertEquals(0, fixture.backCalls) }
    }

    @Test
    fun quickSwipeCommitsWithoutAnAnimationTailFromEitherEdge() = withFixture { fixture ->
        for ((index, edge) in listOf(NavigationEvent.EDGE_LEFT, NavigationEvent.EDGE_RIGHT).withIndex()) {
            if (index > 0) {
                runOnIdle { fixture.backStack.add(fixture.detail) }
                settle()
            }
            runOnIdle {
                fixture.input.backStarted(NavigationEvent(swipeEdge = edge))
                // 即使短促滑动的预览进度很高，完成返回也不能等待剩余动画。
                fixture.input.backProgressed(NavigationEvent(swipeEdge = edge, progress = 0.9f))
            }
            mainClock.advanceTimeBy(48)
            waitForIdle()
            assertHeldForeground(0.9f, edge)
            assertTrue(onNodeWithTag("detail").fetchSemanticsNode().boundsInRoot.width < CardWidth)
            onNodeWithTag("root").assertExists()
            runOnIdle { fixture.input.backCompleted() }
            advanceCommitFrames()
            assertFullPage(CardRootArgb)
            onNodeWithTag("detail").assertDoesNotExist()
            runOnIdle {
                assertEquals(listOf(fixture.root), fixture.backStack.toList())
                assertEquals(index + 1, fixture.backCalls)
                assertFalse(fixture.sharedScope.isTransitionActive)
            }
        }
    }

    @Test
    fun quickCancellationAndRetryInOneFrameDoNotReuseTheCompletionTimer() = withFixture { fixture ->
        runOnIdle { fixture.input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT)) }
        mainClock.advanceTimeBy(96)
        waitForIdle()
        runOnIdle {
            fixture.input.backCancelled()
            fixture.input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_RIGHT))
            fixture.input.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_RIGHT, progress = 0.6f))
        }
        // 此时旧手势的计时已结束，新手势仍应判定为快速返回。
        mainClock.advanceTimeBy(80)
        waitForIdle()
        assertHeldForeground(0.6f, NavigationEvent.EDGE_RIGHT)
        runOnIdle {
            assertEquals(0, fixture.backCalls)
            fixture.input.backCompleted()
        }
        advanceCommitFrames()
        assertFullPage(CardRootArgb)
        onNodeWithTag("detail").assertDoesNotExist()
        mainClock.advanceTimeBy(300)
        waitForIdle()
        assertFullPage(CardRootArgb)
        runOnIdle { assertEquals(1, fixture.backCalls) }
    }

    @Test
    fun quickCommitInterruptsACancelledPreviewWithoutKeepingItsCard() = withFixture { fixture ->
        previewBack(fixture, NavigationEvent.EDGE_LEFT)
        assertPreview(captureToImage().toAwtImage())
        runOnIdle {
            fixture.input.backCancelled()
            fixture.input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_RIGHT))
            fixture.input.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_RIGHT, progress = 0.15f))
            fixture.input.backCompleted()
        }
        advanceCommitFrames()
        assertFullPage(CardRootArgb)
        onNodeWithTag("detail").assertDoesNotExist()
        runOnIdle {
            assertEquals(1, fixture.backCalls)
            assertEquals(true, fixture.sharedEnabled["root"])
            assertFalse(fixture.sharedScope.isTransitionActive)
        }
    }

    @Test
    fun completionWithoutProgressReturnsImmediately() = withFixture { fixture ->
        runOnIdle { fixture.input.backCompleted() }
        advanceCommitFrames()
        assertFullPage(CardRootArgb)
        onNodeWithTag("detail").assertDoesNotExist()
        runOnIdle { assertEquals(1, fixture.backCalls) }
    }

    @Test
    fun holdingNearOrAtFullProgressKeepsTheForegroundOpaqueUntilTheGestureCompletes() = withFixture { fixture ->
        previewBack(fixture, NavigationEvent.EDGE_LEFT)
        for (progress in listOf(0.9f, 1f)) {
            holdProgress(fixture, NavigationEvent.EDGE_LEFT, progress)
            assertHeldForeground(progress, NavigationEvent.EDGE_LEFT)
            runOnIdle {
                assertEquals(listOf(fixture.root, fixture.detail), fixture.backStack.toList())
                assertEquals(0, fixture.backCalls, "Progress $progress must not complete a held gesture")
                assertEquals(false, fixture.sharedEnabled["detail"], "The held foreground must remain composed")
            }
        }

        // 输入明确完成手势之前，即使达到 100% 进度仍可往回拖动。
        holdProgress(fixture, NavigationEvent.EDGE_LEFT, 0.25f)
        assertHeldForeground(0.25f, NavigationEvent.EDGE_LEFT)
        runOnIdle { fixture.input.backCancelled() }
        settle()
        assertFullPage(CardDetailArgb)
        runOnIdle {
            assertEquals(2, fixture.backStack.size)
            assertEquals(0, fixture.backCalls)
            assertEquals(true, fixture.sharedEnabled["detail"])
        }

    }

    @Test
    fun cancellationRestoresThePageAndTheSamePairCanCommitFromTheOtherEdge() = withFixture { fixture ->
        assertFullPage(CardDetailArgb)
        previewBack(fixture, NavigationEvent.EDGE_LEFT)
        val leftPreview = captureToImage().toAwtImage()
        ImageIO.write(leftPreview, "png", File(System.getProperty("java.io.tmpdir"), "pipixiv-predictive-back.png"))
        assertPreview(leftPreview)
        val leftCardStart = firstDetailPixel(leftPreview)
        runOnIdle {
            assertEquals(false, fixture.sharedEnabled["detail"])
            assertEquals(false, fixture.sharedEnabled["root"])
            assertFalse(fixture.sharedScope.isTransitionActive, "Shared images must remain inside the card")
            assertEquals(2, fixture.backStack.size, "Previewing must not pop the stack")
            fixture.input.backCancelled()
        }
        settle()
        assertFullPage(CardDetailArgb)
        runOnIdle {
            assertEquals(2, fixture.backStack.size)
            assertEquals(0, fixture.backCalls)
            assertEquals(true, fixture.sharedEnabled["detail"], "Cancellation must restore shared content")
        }

        // 复用相同的两个页面实例，检查旧手势是否错误保留了左侧滑入方向。
        previewBack(fixture, NavigationEvent.EDGE_RIGHT)
        val rightPreview = captureToImage().toAwtImage()
        assertPreview(rightPreview)
        assertTrue(firstDetailPixel(rightPreview) < leftCardStart, "A new gesture must use its own swipe edge")
        holdProgress(fixture, NavigationEvent.EDGE_RIGHT, 1f)
        assertHeldForeground(1f, NavigationEvent.EDGE_RIGHT)
        runOnIdle { fixture.input.backCompleted() }
        settle()
        assertFullPage(CardRootArgb)
        runOnIdle {
            assertEquals(listOf(fixture.root), fixture.backStack.toList())
            assertEquals(1, fixture.backCalls, "Completing a gesture must pop exactly once")
            assertEquals(true, fixture.sharedEnabled["root"], "Commit must restore shared content")
            assertFalse(fixture.sharedScope.isTransitionActive)
        }
    }

    @Test
    fun ordinaryPopAfterCancellationAndTheNextForwardNavigationDoNotUseTheCard() = withFixture { fixture ->
        previewBack(fixture, NavigationEvent.EDGE_LEFT)
        runOnIdle { fixture.input.backCancelled() }
        settle()

        // 普通返回与已取消的手势使用相同的起始页面和目标页面。
        runOnIdle { fixture.backStack.removeLast() }
        mainClock.advanceTimeBy(64)
        waitForIdle()
        assertUnscaledPage("detail")
        runOnIdle {
            assertEquals(true, fixture.sharedEnabled["detail"], "A completed cancellation must clear its session")
            assertEquals(true, fixture.sharedEnabled["root"])
        }
        settle()
        assertFullPage(CardRootArgb)

        runOnIdle { fixture.backStack.add(fixture.detail) }
        mainClock.advanceTimeBy(64)
        waitForIdle()
        assertUnscaledPage("detail")
        runOnIdle {
            assertEquals(true, fixture.sharedEnabled["detail"], "Forward navigation must keep shared transitions enabled")
        }
        settle()
        assertFullPage(CardDetailArgb)
    }

    private fun withFixture(
        rootColor: Int = CardRootArgb,
        detailColor: Int = CardDetailArgb,
        colorScheme: ColorScheme = lightColorScheme(),
        block: DesktopComposeUiTest.(BackCardFixture) -> Unit,
    ) {
        // 应用安装了 Tao 的 Main 调度器，而桌面 Compose 测试使用 Swing EDT。
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runDesktopComposeUiTest(width = CardWidth, height = CardHeight, testTimeout = 30.seconds) {
                mainClock.autoAdvance = false
                val fixture = BackCardFixture(rootColor, detailColor, colorScheme)
                try {
                    setContent { fixture.Content() }
                    settle()
                    block(fixture)
                } finally {
                    runOnIdle { fixture.navigationEventDispatcher.dispose() }
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun DesktopComposeUiTest.previewBack(fixture: BackCardFixture, edge: Int) {
        runOnIdle { fixture.input.backStarted(NavigationEvent(swipeEdge = edge)) }
        mainClock.advanceTimeByFrame()
        waitForIdle()
        runOnIdle { fixture.input.backProgressed(NavigationEvent(swipeEdge = edge, progress = 0.6f)) }
        mainClock.advanceTimeBy(64)
        waitForIdle()
    }

    private fun DesktopComposeUiTest.advanceCommitFrames() {
        repeat(3) {
            mainClock.advanceTimeByFrame()
            waitForIdle()
        }
    }

    private fun DesktopComposeUiTest.settle() {
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun DesktopComposeUiTest.holdProgress(fixture: BackCardFixture, edge: Int, progress: Float) {
        runOnIdle { fixture.input.backProgressed(NavigationEvent(swipeEdge = edge, progress = progress)) }
        // 等待时间超过卡片动画时长，验证按住手指时不会自行完成动画。
        mainClock.advanceTimeBy(1_200)
        waitForIdle()
    }

    private fun DesktopComposeUiTest.assertHeldForeground(progress: Float, edge: Int) {
        val image = captureToImage().toAwtImage()
        assertEquals(
            CardDetailArgb,
            image.getRGB(CardWidth / 2, CardSampleY),
            "The foreground must remain fully opaque while progress $progress is held",
        )
        // 采样点位于上一页较小的图片标记之外，避免其像素掩盖前景消失的问题。
        val markerX = CardWidth / 2 + if (edge == NavigationEvent.EDGE_LEFT) 30 else -30
        assertEquals(
            CardMarkerArgb,
            image.getRGB(markerX, CardHeight / 2),
            "The foreground image must remain visible while progress $progress is held",
        )
    }

    private fun DesktopComposeUiTest.assertFullPage(expected: Int) {
        val image = captureToImage().toAwtImage()
        assertEquals(expected, image.getRGB(2, 2), "The page must restore its top-left corner")
        assertEquals(expected, image.getRGB(CardWidth - 3, CardHeight - 3), "The page must fill the viewport")
        assertEquals(expected, image.getRGB(CardWidth / 2, CardSampleY))
    }

    private fun DesktopComposeUiTest.assertSameColorPreview(
        pageColor: Int,
        edge: Int,
        previousExposedColor: Int?,
    ): Int {
        val image = captureToImage().toAwtImage()
        val exposedColor = image.getRGB(2, 2)
        assertTrue(exposedColor != pageColor, "Equal page backgrounds must remain distinguishable during preview")
        assertEquals(exposedColor, image.getRGB(CardWidth - 3, CardHeight - 3))
        if (previousExposedColor != null) {
            assertEquals(previousExposedColor, exposedColor, "The exposed destination must not lose contrast at full progress")
        }
        assertEquals(pageColor, image.getRGB(CardWidth / 2, CardSampleY), "The foreground must retain its original color")
        val markerX = CardWidth / 2 + if (edge == NavigationEvent.EDGE_LEFT) 30 else -30
        assertEquals(CardMarkerArgb, image.getRGB(markerX, CardHeight / 2), "The foreground image must stay fully opaque")
        return exposedColor
    }

    private fun assertPreview(image: BufferedImage) {
        assertEquals(CardDetailArgb, image.getRGB(CardWidth / 2, CardSampleY), "The foreground must remain opaque")
        assertEquals(CardMarkerArgb, image.getRGB(CardWidth / 2, CardHeight / 2), "The shared image must stay visible")
        val corner = image.getRGB(2, 2)
        val rootBlue = CardRootArgb and 0xff
        val cornerBlue = corner and 0xff
        assertTrue(cornerBlue in 1 until rootBlue, "The exposed previous page must be dimmed")
        assertTrue((corner and 0xff) > ((corner shr 16) and 0xff), "The previous blue page must be visible")
        val start = firstDetailPixel(image)
        val end = (CardWidth - 1 downTo 0).first { image.getRGB(it, CardSampleY) == CardDetailArgb }
        assertTrue(start > 0 && end < CardWidth - 1, "The foreground must shrink on both sides")
        assertTrue(end - start < CardWidth * 0.98f, "Predictive progress must scale the whole foreground")
        // 进度为 60% 时，缩放后的顶边接近 y=24；顶部中央应填充，圆角处应裁切。
        assertEquals(CardDetailArgb, image.getRGB(CardWidth / 2, 30))
        assertTrue(image.getRGB(start + 2, 26) != CardDetailArgb, "The shrunken page must have rounded corners")
    }

    private fun firstDetailPixel(image: BufferedImage): Int =
        (0 until CardWidth).first { image.getRGB(it, CardSampleY) == CardDetailArgb }

    private fun DesktopComposeUiTest.assertUnscaledPage(tag: String) {
        val bounds = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals(0f, bounds.left, 0.5f, "Ordinary navigation must not translate the page")
        assertEquals(0f, bounds.top, 0.5f)
        assertEquals(CardWidth.toFloat(), bounds.width, 0.5f, "Ordinary navigation must not shrink the page")
        assertEquals(CardHeight.toFloat(), bounds.height, 0.5f)
    }
}

private const val CardWidth = 400
private const val CardHeight = 800
private const val CardSampleY = 520
private const val CardRootArgb = 0xff286bc5.toInt()
private const val CardDetailArgb = 0xffcc3452.toInt()
private const val CardMarkerArgb = 0xff4bc17b.toInt()
private const val CardSameLightArgb = 0xfff4f4f4.toInt()
private const val CardSameDarkArgb = 0xff121212.toInt()

private class BackCardFixture(
    private val rootColor: Int = CardRootArgb,
    private val detailColor: Int = CardDetailArgb,
    private val colorScheme: ColorScheme = lightColorScheme(),
) : NavigationEventDispatcherOwner {
    override val navigationEventDispatcher = NavigationEventDispatcher()
    val input = DirectNavigationEventInput().also(navigationEventDispatcher::addInput)
    val root = NavigationRecord("root", Destination.Main)
    val detail = NavigationRecord("detail", Destination.Setting)
    val backStack = mutableStateListOf(root, detail)
    val sharedEnabled = mutableMapOf<String, Boolean>()
    var backCalls = 0
    lateinit var sharedScope: SharedTransitionScope

    @Composable
    fun Content() {
        MaterialTheme(colorScheme = colorScheme) {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                sharedScope = this
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this,
                    LocalNavigationEventDispatcherOwner provides this@BackCardFixture,
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
                                    metadata = metadata { put(NavigationRecordKey, record) } +
                                        NavDisplay.predictivePopTransitionSpec {
                                            // 类似 Picture 页面已有的转场元数据不能让卡片淡出。
                                            fadeIn() togetherWith (scaleOut(targetScale = 0.5f) + fadeOut())
                                        },
                                ) { Screen(record) }
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Screen(record: NavigationRecord) {
        val enabled = LocalNavigationSharedContentEnabled.current
        SideEffect { sharedEnabled[record.entryId] = enabled }
        DisposableEffect(record.entryId) {
            onDispose { sharedEnabled.remove(record.entryId) }
        }
        val animatedScope = LocalNavAnimatedContentScope.current
        with(LocalSharedTransitionScope.current) {
            Box(
                Modifier.fillMaxSize().testTag(record.entryId)
                    .background(Color(if (record == detail) detailColor else rootColor)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(if (record == detail) 80.dp else 48.dp)
                        .sharedElement(
                            rememberNavigationSharedContentState("back-card-test-image"),
                            animatedVisibilityScope = animatedScope,
                        )
                        .background(Color(CardMarkerArgb)),
                )
            }
        }
    }
}
