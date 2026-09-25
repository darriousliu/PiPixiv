package com.mrl.pixiv.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.mrl.pixiv.common.animation.DefaultAnimationDuration
import com.mrl.pixiv.common.animation.DefaultFloatAnimationSpec
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.layout.PaneInputState
import com.mrl.pixiv.common.compose.layout.SplitPaneState
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationRecord
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.image.preview.ImagePreviewScreen
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
class PicturePreviewInteractionTest {
    @Test
    fun desktopPreviewPaintsAndReturnsControlToBothColumns() = checkPreviewRoundTrip(
        width = 1440,
        desktopPointerHandler = true,
    )

    @Test
    fun narrowedDesktopPreviewPaintsAndReturnsControlToBothColumns() = checkPreviewRoundTrip(
        width = 1024,
        desktopPointerHandler = true,
    )

    @Test
    fun withoutDesktopPointerHandlerPreviewStillReturnsControlToBothColumns() = checkPreviewRoundTrip(
        width = 1024,
        desktopPointerHandler = false,
    )

    private fun checkPreviewRoundTrip(width: Int, desktopPointerHandler: Boolean) {
        // 使用本地图片测试 CoilZoomAsyncImage 和真实预览页面，
        // 避免依赖网络、账号状态、数据仓库和依赖注入。
        val bitmap = BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().let { graphics ->
                graphics.color = java.awt.Color(ImageArgb, true)
                graphics.fillRect(0, 0, this.width, height)
                graphics.dispose()
            }
        }
        val imageFile = File.createTempFile("picture-preview-", ".png")
        ImageIO.write(bitmap, "png", imageFile)
        // common:core 会引入原生 Tao Main 调度器；Compose 无窗口桌面测试
        // 则在 Swing EDT 上渲染，并处理 NavDisplay 的生命周期变化。
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runDesktopComposeUiTest(width = width, height = 800, testTimeout = 40.seconds) {
                // 共享转场卡住时应触发断言失败，
                // 避免测试自动等待动画结束而无限挂起。
                mainClock.autoAdvance = false
                val fixture = PictureFixture(bitmap, imageFile, desktopPointerHandler)
                val screenshotPrefix = "$width-desktop-handler-$desktopPointerHandler"
                setContent { fixture.Content() }
                advanceFrames()
                saveScreenshot("$screenshotPrefix-loading")
                waitUntil(timeoutMillis = 10_000) {
                    mainClock.advanceTimeByFrame()
                    captureToImage().toAwtImage().getRGB(width / 4, 200) == ImageArgb
                }
                saveScreenshot("$screenshotPrefix-before")
                runOnIdle {
                    assertFalse(
                        fixture.imageSharedState.isMatchFound,
                        "A picture image must not match a duplicate modifier on itself before preview opens",
                    )
                }

                onNodeWithTag("picture-image").performMouseInput { click() }
                advanceFrames()
                saveScreenshot("$screenshotPrefix-preview")
                runOnIdle {
                    assertEquals(1, fixture.previewOpens, "One physical image click opens one preview")
                    assertFalse(fixture.sharedScope.isTransitionActive, "Preview transition must finish")
                    assertTrue(fixture.backStack.last().destination is Destination.ImagePreview)
                }

                // 节点存在无法排除页面不可见但仍拦截输入的情况。
                // 检查图片外侧为黑色，以及原详情区域已被图片像素覆盖。
                assertPixel(width - 8, 8, 0xff000000.toInt(), "Preview must paint its black background")
                waitUntil(timeoutMillis = 10_000) {
                    mainClock.advanceTimeByFrame()
                    captureToImage().toAwtImage().getRGB(width * 3 / 4, 400) == ImageArgb
                }
                assertPixel(width * 3 / 4, 400, ImageArgb, "Preview image must cover the right column")

                // 对实际返回按钮发送指针事件（内缩 8 dp，尺寸 48 dp）。
                // 直接调用返回回调或语义操作会绕过输入拦截器。
                onNodeWithTag("preview").performMouseInput { click(Offset(32f, 32f)) }
                advanceFrames()
                runOnIdle {
                    assertEquals(1, fixture.backStack.size, "The preview back button must receive input")
                    assertFalse(fixture.sharedScope.isTransitionActive, "Return transition must finish")
                }
                assertPixel(width - 8, 8, DetailsArgb, "Picture details must be painted after returning")

                onNodeWithTag("details-action-0").performMouseInput { click() }
                onNodeWithTag("left-action-0").performMouseInput { click() }
                onNodeWithTag("picture-left").performTouchInput {
                    swipe(Offset(center.x, height * .85f), Offset(center.x, height * .3f))
                }
                advanceFrames()
                onNodeWithTag("picture-right").performTouchInput {
                    swipe(Offset(center.x, height * .85f), Offset(center.x, height * .3f))
                }
                advanceFrames()
                runOnIdle {
                    assertEquals(1, fixture.leftClicks, "Left column must receive clicks after preview")
                    assertEquals(1, fixture.detailClicks, "Right column must receive clicks after preview")
                    assertTrue(fixture.leftState.hasScrolled(), "Left image list must actually scroll")
                    assertTrue(fixture.rightState.hasScrolled(), "Right details list must actually scroll")
                }
                saveScreenshot("$screenshotPrefix-returned-and-scrolled")
            }
        } finally {
            Dispatchers.resetMain()
            imageFile.delete()
        }
    }

    private fun DesktopComposeUiTest.advanceFrames() {
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
    }

    private fun DesktopComposeUiTest.assertPixel(x: Int, y: Int, expected: Int, message: String) {
        assertEquals(expected, captureToImage().toAwtImage().getRGB(x, y), message)
    }

    private fun DesktopComposeUiTest.saveScreenshot(name: String) {
        val directory = File("build/reports/picture-preview").apply { mkdirs() }
        ImageIO.write(captureToImage().toAwtImage(), "png", File(directory, "$name.png"))
    }
}

private const val ImageArgb = 0xffbc2649.toInt()
private const val DetailsArgb = 0xffe6d37b.toInt()
private const val ImageKey = "preview-test-image-5-0"

private fun LazyListState.hasScrolled() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0

/**
 * Picture 测试保留生产代码的修饰符顺序和宽屏布局：
 * 卡片 sharedBounds -> 等权重分栏 -> LazyColumn -> fillMaxWidth -> 按条件启用的
 * 图片 sharedElement(AnimatedSize) -> 点击处理 -> 按条件启用的桌面指针处理。
 * 目标页面、导航场景、预览页面和条件修饰符辅助函数均使用生产代码。
 */
private class PictureFixture(
    bitmap: BufferedImage,
    imageFile: File,
    private val desktopPointerHandler: Boolean,
) {
    private val picture = NavigationRecord("picture", Destination.PictureDeeplink(5))
    private val preview = NavigationRecord(
        "preview",
        Destination.ImagePreview(listOf(imageFile.absolutePath), 0, ImageKey),
    )
    val backStack = mutableStateListOf(picture)
    val leftState = LazyListState()
    val rightState = LazyListState()
    private val imageUrl = imageFile.absolutePath
    private val placeholder = BitmapPainter(bitmap.toComposeImageBitmap())
    lateinit var sharedScope: SharedTransitionScope
    lateinit var imageSharedState: SharedTransitionScope.SharedContentState
    var previewOpens = 0
    var leftClicks = 0
    var detailClicks = 0

    @Composable
    fun Content() {
        MaterialTheme {
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                sharedScope = this
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val splitState = remember { SplitPaneState() }
                        val inputState = remember { PaneInputState() }
                        val transitions = remember { AdaptivePaneTransitionState() }
                        val strategy = remember(maxWidth, maxHeight) {
                            AdaptiveSceneStrategy(maxWidth, maxHeight, splitState, inputState, transitions)
                        }
                        PredictiveBackNavDisplay(
                            backStack = backStack,
                            enableQuickBack = true,
                            onBack = { backStack.removeLast() },
                            modifier = Modifier.fillMaxSize(),
                            sharedTransitionScope = this@SharedTransitionLayout,
                            sceneStrategies = listOf(strategy),
                            entryProvider = { record ->
                                val transitionMetadata = NavDisplay.transitionSpec {
                                    fadeIn(DefaultFloatAnimationSpec) togetherWith fadeOut(DefaultFloatAnimationSpec)
                                } + NavDisplay.predictivePopTransitionSpec {
                                    fadeIn(DefaultFloatAnimationSpec) togetherWith fadeOut(DefaultFloatAnimationSpec)
                                }
                                NavEntry(
                                    key = record,
                                    contentKey = record.entryId,
                                    metadata = transitionMetadata + metadata { put(NavigationRecordKey, record) },
                                ) {
                                    when (val destination = record.destination) {
                                        is Destination.ImagePreview -> ImagePreviewScreen(
                                            imageUrls = destination.imageUrls,
                                            initialIndex = destination.initialIndex,
                                            sharedElementKey = destination.sharedElementKey,
                                            onBack = { backStack.removeLast() },
                                            modifier = Modifier.testTag("preview"),
                                        )
                                        else -> Picture()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Picture() = with(LocalSharedTransitionScope.current) {
        val animatedScope = LocalNavAnimatedContentScope.current
        imageSharedState = rememberSharedContentState(ImageKey)
        Scaffold(
            modifier = Modifier.fillMaxSize().conditionally(true) {
                sharedBounds(
                    rememberSharedContentState("preview-test-card-5"),
                    animatedScope,
                    enter = fadeIn(DefaultFloatAnimationSpec),
                    exit = fadeOut(DefaultFloatAnimationSpec),
                    boundsTransform = { _, _ -> tween(DefaultAnimationDuration) },
                )
            },
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    LazyColumn(state = leftState, modifier = Modifier.fillMaxSize().testTag("picture-left")) {
                        item {
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                        .data(imageUrl)
                                        .size(640, 480)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    placeholder = placeholder,
                                    modifier = Modifier.fillMaxWidth()
                                        .conditionally(true) {
                                            sharedElement(
                                                imageSharedState,
                                                animatedVisibilityScope = animatedScope,
                                                placeholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                                            )
                                        }
                                        .throttleClick(
                                            onClick = {
                                                previewOpens++
                                                backStack.add(preview)
                                            },
                                            onLongClick = {},
                                        )
                                        .conditionally(desktopPointerHandler) {
                                            pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) awaitPointerEvent()
                                                }
                                            }
                                        }
                                        .testTag("picture-image"),
                                )
                            }
                        }
                        items(40) { index ->
                            Text(
                                "Image action $index",
                                modifier = Modifier.fillMaxWidth().height(96.dp)
                                    .background(Color(0xffd7e4f0)).testTag("left-action-$index")
                                    .clickable { leftClicks++ }.padding(24.dp),
                            )
                        }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    LazyColumn(state = rightState, modifier = Modifier.fillMaxSize().testTag("picture-right")) {
                        items(40) { index ->
                            Text(
                                "Details action $index",
                                modifier = Modifier.fillMaxWidth().height(96.dp)
                                    .background(Color(DetailsArgb)).testTag("details-action-$index")
                                    .clickable { detailClicks++ }.padding(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
