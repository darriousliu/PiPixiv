package com.mrl.pixiv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.github.panpf.zoomimage.util.requiredMainThread
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.image.preview.ImagePreviewScreen
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.window.tao.TaoEventCode
import dev.nucleusframework.window.tao.TaoMouseButton
import dev.nucleusframework.window.tao.TaoWindow
import java.awt.image.BufferedImage
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The native event loop needs its own first JVM thread; a Swing UI test cannot cover this bug. */
class TaoImagePreviewSmokeTest {
    @Test
    fun previewRendersAndBackButtonRespondsOnTao() {
        val reportDirectory = File("build/reports/tao-image-preview").apply { mkdirs() }
        val logFile = File(reportDirectory, "native-smoke.log")
        val command = mutableListOf(
            File(System.getProperty("java.home"), "bin/java").absolutePath,
            "--enable-native-access=ALL-UNNAMED",
            "-Djava.awt.headless=false",
        )
        if (System.getProperty("os.name").startsWith("Mac")) command += "-XstartOnFirstThread"
        command += listOf(
            "-cp", runtimeClasspath(),
            TaoImagePreviewSmokeHarness::class.java.name,
            reportDirectory.absolutePath,
        )
        // Diagnostic control: the same test must fail if the production thread registration is skipped.
        if (System.getenv("PIPIXIV_SMOKE_SKIP_ZOOM_THREAD_CHECKER") == "1") {
            command += "--skip-zoom-thread-checker"
        }
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(logFile)
            .start()
        try {
            val finished = process.waitFor(40, TimeUnit.SECONDS)
            assertTrue(finished, "Tao preview subprocess timed out.\n${logFile.readText()}")
            val log = logFile.readText()
            assertEquals(0, process.exitValue(), "Tao preview subprocess failed.\n$log")
            assertTrue("TAO_PREVIEW_SMOKE_OK" in log, "Preview never completed rendering and back.\n$log")
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(5, TimeUnit.SECONDS)
            }
        }
    }

    private fun runtimeClasspath(): String {
        // Gradle puts test dependencies in its context URLClassLoader instead of java.class.path.
        val loaderEntries = generateSequence(Thread.currentThread().contextClassLoader) { it.parent }
            .filterIsInstance<URLClassLoader>()
            .flatMap { it.urLs.asSequence() }
            .map { File(it.toURI()).absolutePath }
        return (loaderEntries + System.getProperty("java.class.path").split(File.pathSeparator).asSequence())
            .distinct()
            .joinToString(File.pathSeparator)
    }
}

/**
 * A separate native process using the real preview composable, local PNG, production Tao backend,
 * and production thread registration. No application initialization, account, network, or user files.
 */
object TaoImagePreviewSmokeHarness {
    @JvmStatic
    fun main(args: Array<String>) {
        val reportDirectory = File(args.first()).apply { mkdirs() }
        val imageFile = Files.createTempFile("tao-preview-smoke-", ".png").toFile()
        val bitmap = BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().let { graphics ->
                graphics.color = java.awt.Color(PreviewImageArgb, true)
                graphics.fillRect(0, 0, width, height)
                graphics.dispose()
            }
        }
        ImageIO.write(bitmap, "png", imageFile)
        imageFile.deleteOnExit()
        if ("--skip-zoom-thread-checker" !in args) configureZoomImageMainThreadChecker()

        nucleusApplication(backend = NucleusBackend.Tao, enableSingleInstance = false) {
            val windowState = rememberWindowState(size = DpSize(1024.dp, 760.dp))
            DecoratedWindow(
                onCloseRequest = ::exitApplication,
                state = windowState,
                title = "PiPixiv Image Preview regression test",
                undecorated = true,
            ) {
                val taoWindow = checkNotNull(nucleusWindow.unsafe.taoWindow)
                val density = LocalDensity.current.density
                val captureLayer = rememberGraphicsLayer()
                val drawnFrames = remember { AtomicInteger() }
                val closedFrames = remember { AtomicInteger() }
                val backClicks = remember { AtomicInteger() }
                val closedItemClicks = remember { AtomicInteger() }
                val closedListState = rememberLazyListState()
                var previewVisible by remember { mutableStateOf(true) }
                MaterialTheme {
                    SharedTransitionLayout(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                            AnimatedContent(targetState = previewVisible) { visible ->
                                CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
                                    if (visible) {
                                        ImagePreviewScreen(
                                            imageUrls = listOf(imageFile.absolutePath),
                                            initialIndex = 0,
                                            onBack = {
                                                backClicks.incrementAndGet()
                                                previewVisible = false
                                            },
                                            modifier = Modifier.drawWithContent {
                                                captureLayer.record { this@drawWithContent.drawContent() }
                                                drawLayer(captureLayer)
                                                drawnFrames.incrementAndGet()
                                            },
                                        )
                                    } else {
                                        LazyColumn(
                                            state = closedListState,
                                            modifier = Modifier.fillMaxSize().drawWithContent {
                                                drawContent()
                                                closedFrames.incrementAndGet()
                                            },
                                        ) {
                                            items(40) { index ->
                                                Text(
                                                    "Preview closed — item $index",
                                                    modifier = Modifier.fillMaxWidth().height(80.dp)
                                                        .clickable { closedItemClicks.incrementAndGet() }
                                                        .padding(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    // Native input flushes Compose work. Keep the driver off Compose's own
                    // FlushCoroutineDispatcher so an injected event cannot re-enter this effect.
                    withContext(Dispatchers.Main) {
                        withTimeout(20.seconds) {
                            while (true) {
                                delay(100.milliseconds)
                                if (drawnFrames.get() == 0) continue
                                val image = captureLayer.toImageBitmap().toAwtImage()
                                if (image.getRGB(image.width / 2, image.height / 2) != PreviewImageArgb) continue
                                check(image.getRGB(image.width - 2, image.height / 2) == 0xff000000.toInt()) {
                                    "Preview background has not been painted"
                                }
                                ImageIO.write(image, "png", File(reportDirectory, "native-preview.png"))
                                break
                            }
                            // Check the actual render dispatcher and preserve rejection on a worker.
                            check(!Dispatchers.Main.immediate.isDispatchNeeded(EmptyCoroutineContext))
                            requiredMainThread()
                            val workerFailure = withContext(Dispatchers.Default) {
                                runCatching { requiredMainThread() }.exceptionOrNull()
                            }
                            check(workerFailure is IllegalStateException) { "Worker thread was incorrectly accepted" }

                            // Route the same native events into the host that feeds Compose; do not invoke onBack.
                            val coordinate = (32f * density * 1024f).roundToInt()
                            taoWindow.dispatchSmokePointer(TaoEventCode.CURSOR_MOVED, coordinate, coordinate)
                            taoWindow.dispatchSmokePointer(TaoEventCode.MOUSE_DOWN, TaoMouseButton.LEFT, 0)
                            delay(80.milliseconds)
                            taoWindow.dispatchSmokePointer(TaoEventCode.MOUSE_UP, TaoMouseButton.LEFT, 0)
                            while (closedFrames.get() == 0) delay(50.milliseconds)
                            check(backClicks.get() == 1) { "The real preview back button did not receive one click" }
                            // Wait for AnimatedContent's outgoing preview to finish before probing the list.
                            delay(400.milliseconds)
                            taoWindow.dispatchSmokePointer(
                                TaoEventCode.CURSOR_MOVED,
                                (256f * density * 1024f).roundToInt(),
                                (40f * density * 1024f).roundToInt(),
                            )
                            taoWindow.dispatchSmokePointer(TaoEventCode.MOUSE_DOWN, TaoMouseButton.LEFT, 0)
                            delay(80.milliseconds)
                            taoWindow.dispatchSmokePointer(TaoEventCode.MOUSE_UP, TaoMouseButton.LEFT, 0)
                            while (closedItemClicks.get() == 0) delay(50.milliseconds)
                            check(closedItemClicks.get() == 1) { "Returned list did not receive one click" }

                            // Native wheel deltas use hundredths of a line; negative native Y scrolls down.
                            taoWindow.dispatchSmokePointer(TaoEventCode.SCROLL_LINE, 0, -1200)
                            while (closedListState.firstVisibleItemIndex == 0 &&
                                closedListState.firstVisibleItemScrollOffset == 0
                            ) delay(50.milliseconds)
                            println(
                                "TAO_PREVIEW_SMOKE_OK frames=${drawnFrames.get()} back=${backClicks.get()} " +
                                    "listClicks=${closedItemClicks.get()} " +
                                    "scroll=${closedListState.firstVisibleItemIndex}:${closedListState.firstVisibleItemScrollOffset}",
                            )
                            exitApplication()
                        }
                    }
                }
            }
        }
        imageFile.delete()
    }
}

private const val PreviewImageArgb = 0xffbc2649.toInt()

private fun TaoWindow.dispatchSmokePointer(code: Int, first: Int, second: Int) {
    // 通过反射走原生窗口的事件分发入口，避免依赖系统级输入权限。
    // 只去掉 Kotlin 的内部名称后缀，防止误匹配同参数的滚动手势入口。
    val dispatch = javaClass.declaredMethods.single {
        it.name.substringBefore('$') == "dispatch" && it.parameterTypes.contentEquals(
            arrayOf(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType),
        )
    }
    dispatch.isAccessible = true
    dispatch.invoke(this, code, first, second)
}
