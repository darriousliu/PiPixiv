package com.mrl.pixiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.mrl.pixiv.common.data.setting.SettingTheme
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.darkmodedetector.getPlatformDarkModeDetector
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBar
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaoDesktopThemeSmokeTest {
    @Test
    fun nativeThemeNotificationsReachTaoWindowAndManualOverrides() {
        val osName = System.getProperty("os.name")
        assumeTrue(osName.startsWith("Mac") || osName.startsWith("Windows"))
        val logFile = File("build/reports/desktop-theme/native-theme.log")
        logFile.parentFile.mkdirs()
        val classpath = generateSequence(Thread.currentThread().contextClassLoader) { it.parent }
            .filterIsInstance<URLClassLoader>()
            .flatMap { it.urLs.asSequence() }
            .map { File(it.toURI()).absolutePath }
            .plus(System.getProperty("java.class.path").split(File.pathSeparator))
            .distinct()
            .joinToString(File.pathSeparator)
        val command = mutableListOf(
            File(System.getProperty("java.home"), "bin/java").absolutePath,
            "--enable-native-access=ALL-UNNAMED",
            "-Djava.awt.headless=false",
        )
        // macOS 的原生窗口事件循环必须运行在独立进程的首线程。
        if (osName.startsWith("Mac")) command += "-XstartOnFirstThread"
        command += listOf("-cp", classpath, TaoDesktopThemeSmokeHarness::class.java.name)
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(logFile)
            .start()
        try {
            assertTrue(process.waitFor(40, TimeUnit.SECONDS), "原生主题测试超时：\n${logFile.readText()}")
            val log = logFile.readText()
            assertEquals(0, process.exitValue(), "原生主题测试失败：\n$log")
            assertTrue("TAO_DESKTOP_THEME_OK" in log, "主题切换未完成：\n$log")
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(5, TimeUnit.SECONDS)
            }
        }
    }
}

// 使用真实 Tao 窗口和系统主题监听器；仅在子进程内模拟通知，不修改用户的系统设置。
object TaoDesktopThemeSmokeHarness {
    @JvmStatic
    fun main(args: Array<String>) {
        val initialSystemDark = getPlatformDarkModeDetector().isDark()
        val observedTheme = AtomicReference<Pair<Boolean, Boolean>>()
        nucleusApplication(backend = NucleusBackend.Tao, enableSingleInstance = false) {
            var theme by remember { mutableStateOf(SettingTheme.SYSTEM.name) }
            ProvideDesktopTheme(theme = theme) { darkTheme ->
                NucleusDecoratedWindowTheme(isDark = darkTheme) {
                    DecoratedWindow(
                        onCloseRequest = ::exitApplication,
                        title = "PiPixiv 主题回归测试",
                        state = rememberWindowState(size = DpSize(480.dp, 320.dp)),
                    ) {
                        TitleBar { Text("PiPixiv 主题回归测试") }
                        val composeDarkTheme = isSystemInDarkTheme()
                        SideEffect { observedTheme.set(darkTheme to composeDarkTheme) }
                        Text("当前深色主题：$darkTheme")
                    }
                }
            }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.Main) {
                    withTimeout(20_000) {
                        suspend fun awaitTheme(expected: Boolean, scenario: String) {
                            while (observedTheme.get() != (expected to expected)) delay(20)
                            println("$scenario：${observedTheme.get()}")
                        }
                        awaitTheme(initialSystemDark, "读取实际系统主题")
                        notifySystemTheme(true)
                        awaitTheme(true, "系统切换为深色")
                        notifySystemTheme(false)
                        awaitTheme(false, "系统切换为浅色")
                        theme = SettingTheme.DARK.name
                        awaitTheme(true, "手动深色覆盖系统浅色")
                        theme = SettingTheme.SYSTEM.name
                        awaitTheme(false, "切回跟随系统浅色")
                        theme = SettingTheme.LIGHT.name
                        notifySystemTheme(true)
                        awaitTheme(false, "手动浅色覆盖系统深色")
                        theme = SettingTheme.SYSTEM.name
                        awaitTheme(true, "切回跟随系统深色")
                        println("TAO_DESKTOP_THEME_OK")
                        exitApplication()
                    }
                }
            }
        }
    }

    private fun notifySystemTheme(dark: Boolean) {
        val bridge = if (System.getProperty("os.name").startsWith("Mac")) {
            "dev.nucleusframework.darkmodedetector.mac.NativeDarkModeBridge"
        } else {
            "dev.nucleusframework.darkmodedetector.windows.NativeWindowsBridge"
        }
        // 从 JNI 通知入口触发真实监听器，覆盖默认参数中的系统主题订阅。
        Class.forName(bridge)
            .getMethod("onThemeChanged", Boolean::class.javaPrimitiveType)
            .invoke(null, dark)
    }
}
