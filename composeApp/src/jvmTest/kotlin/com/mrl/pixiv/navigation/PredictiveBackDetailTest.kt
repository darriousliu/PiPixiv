package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
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
import com.mrl.pixiv.common.compose.navigationSharedImage
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationRecord
import com.mrl.pixiv.common.router.PictureCoverSource
import com.mrl.pixiv.common.router.PictureImageKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun quicklyReturningToTheArtworkGridCompletesTheSharedTransition() =
        checkSharedImageQuickBack(SharedImageRoute.GridToPicture)

    @Test
    fun quicklyClosingTheImagePreviewCompletesTheSharedTransition() =
        checkSharedImageQuickBack(SharedImageRoute.PictureToPreview)

    private fun checkSharedImageQuickBack(route: SharedImageRoute) {
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runDesktopComposeUiTest(width = 420, height = 800, testTimeout = 30.seconds) {
                mainClock.autoAdvance = false
                val fixture = DetailBackFixture(hasPreviousDetail = false, sharedImageRoute = route)
                val originalStack = fixture.backStack.toList()
                val detailTag = originalStack.last().entryId
                try {
                    setContent { fixture.Content() }
                    settle()

                    // 左上角按钮直接修改返回栈，当前页的图片应与上一页的对应图片匹配。
                    runOnIdle { fixture.backStack.removeLast() }
                    mainClock.advanceTimeBy(48)
                    waitForIdle()
                    runOnIdle {
                        assertTrue(fixture.sharedScope.isTransitionActive, "Toolbar pop must animate the shared image")
                    }
                    settle()
                    runOnIdle { fixture.backStack.add(originalStack.last()) }
                    settle()

                    startBack(fixture, NavigationEvent.EDGE_RIGHT)
                    runOnIdle {
                        fixture.input.backProgressed(
                            NavigationEvent(swipeEdge = NavigationEvent.EDGE_RIGHT, progress = 0.2f),
                        )
                    }
                    mainClock.advanceTimeBy(48)
                    waitForIdle()
                    runOnIdle {
                        assertEquals(originalStack, fixture.backStack.toList())
                        assertTrue(fixture.sharedScope.isTransitionActive, "Swipe must seek the shared image transition")
                        fixture.input.backCompleted()
                    }

                    // 在 120 ms 内松手时，也必须保留场景以绘制共享元素的收尾帧。
                    repeat(3) {
                        mainClock.advanceTimeByFrame()
                        waitForIdle()
                        onNodeWithTag(detailTag).assertExists()
                        runOnIdle { assertTrue(fixture.sharedScope.isTransitionActive) }
                    }
                    settle()
                    onNodeWithTag(detailTag).assertDoesNotExist()
                    assertEquals(DetailSourceArgb, captureToImage().toAwtImage().getRGB(2, 2))
                    runOnIdle {
                        assertEquals(originalStack.dropLast(1), fixture.backStack.toList())
                        assertEquals(1, fixture.backCalls)
                        assertFalse(fixture.sharedScope.isTransitionActive)
                    }
                } finally {
                    runOnIdle { fixture.navigationEventDispatcher.dispose() }
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

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

                    // 逐帧检查完成后的画面，不能跳过可能出现空白或旧场景回闪的前三帧。
                    repeat(6) { frame ->
                        mainClock.advanceTimeByFrame()
                        waitForIdle()
                        assertPanes(
                            if (hasPreviousDetail) NetworkDetailArgb else DetailSourceArgb,
                            "on quick return frame ${frame + 1}",
                        )
                        runOnIdle {
                            assertSame(rememberedState, fixture.sourceRememberedState)
                            assertSame(saveableState, fixture.sourceSaveableState)
                            assertEquals(0, fixture.sourceDisposals, "Source disposed on quick return frame ${frame + 1}")
                        }
                    }
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
        assertEquals(DetailSourceArgb, image.getRGB(100, 400), "The source pixel (100, 400) must stay visible $phase")
        if (expectedRight != null) {
            assertEquals(expectedRight, image.getRGB(1_000, 400), "The detail pixel (1000, 400) must stay opaque $phase")
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

/** 当前页面共享图片的打开方式；[None] 为普通的来源页与详情页。 */
private enum class SharedImageRoute { None, GridToPicture, PictureToPreview }

private class DetailBackFixture(
    hasPreviousDetail: Boolean,
    sharedImageRoute: SharedImageRoute = SharedImageRoute.None,
) : NavigationEventDispatcherOwner {
    override val navigationEventDispatcher = NavigationEventDispatcher()
    val input = DirectNavigationEventInput().also(navigationEventDispatcher::addInput)
    private val sharedImage = when (sharedImageRoute) {
        SharedImageRoute.None -> null
        SharedImageRoute.GridToPicture -> PictureImageKey("detail-test-source", "home", 1L)
        // 作品首图沿用瀑布流的封面标识，entryId 指向更早的瀑布流而不是作品页。
        SharedImageRoute.PictureToPreview -> PictureImageKey("detail-test-grid", "home", 1L)
    }
    private val cover = sharedImage?.let { PictureCoverSource(it, "thumbnail") }
    private val source = NavigationRecord(
        "detail-test-source",
        when (sharedImageRoute) {
            SharedImageRoute.None -> Destination.Setting
            SharedImageRoute.GridToPicture -> Destination.Main
            SharedImageRoute.PictureToPreview -> Destination.Picture(0, "test", cover)
        },
    )
    private val network = NavigationRecord(
        "detail-test-network",
        when (sharedImageRoute) {
            SharedImageRoute.None -> Destination.NetworkSetting
            SharedImageRoute.GridToPicture -> Destination.Picture(0, "test", cover)
            SharedImageRoute.PictureToPreview -> Destination.ImagePreview(listOf("image"), 0, sharedImage)
        },
        ownerEntryId = source.entryId.takeIf { sharedImageRoute == SharedImageRoute.None },
    )
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
    lateinit var sharedScope: SharedTransitionScope
        private set

    @Composable
    fun Content() {
        MaterialTheme {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                sharedScope = this
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
                            settlesQuickBack = { scene, previous -> scene.returnsWithSharedImage(previous) },
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
                                    Box(
                                        Modifier.fillMaxSize().background(Color(color)).testTag(record.entryId),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (sharedImage != null) {
                                            Box(
                                                Modifier
                                                    .navigationSharedImage(sharedImage)
                                                    .size(if (record == source) 48.dp else 160.dp)
                                                    .background(Color.White),
                                            )
                                        }
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
