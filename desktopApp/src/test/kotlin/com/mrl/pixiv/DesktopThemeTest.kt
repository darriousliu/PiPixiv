@file:Suppress("DEPRECATION")
@file:OptIn(
    androidx.compose.ui.InternalComposeUiApi::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
)

package com.mrl.pixiv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.LocalSystemTheme
import com.mrl.pixiv.common.data.setting.SettingTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skiko.SystemTheme as SkikoSystemTheme

class DesktopThemeTest {
    @Test
    fun systemDarkThemeReachesComposeConsumersDespiteNucleusEnumMismatch() = runTest {
        ThemeComposition(this, systemDark = true).use { it.assertTheme(true) }
    }

    @Test
    fun systemLightThemeReachesComposeConsumers() = runTest {
        ThemeComposition(this, systemDark = false).use { it.assertTheme(false) }
    }

    @Test
    fun systemThemeChangesRecomposeExistingConsumers() = runTest {
        ThemeComposition(this, systemDark = false).use {
            it.assertTheme(false)
            it.update(systemDark = true)
            it.assertTheme(true)
            it.update(systemDark = false)
            it.assertTheme(false)
        }
    }

    @Test
    fun manualThemeOverridesSystemForBothWindowAndComposeConsumers() = runTest {
        ThemeComposition(this, systemDark = false, theme = SettingTheme.DARK.name).use {
            it.assertTheme(true)
            it.update(systemDark = true)
            it.assertTheme(true)
            it.update(theme = SettingTheme.LIGHT.name)
            it.assertTheme(false)
            it.update(systemDark = false)
            it.assertTheme(false)
        }
    }

    @Test
    fun returningToSystemUsesLatestSystemTheme() = runTest {
        ThemeComposition(this, systemDark = false, theme = SettingTheme.LIGHT.name).use {
            it.update(systemDark = true)
            it.assertTheme(false)
            it.update(theme = SettingTheme.SYSTEM.name)
            it.assertTheme(true)
            it.update(theme = SettingTheme.DARK.name)
            it.update(systemDark = false)
            it.assertTheme(true)
            it.update(theme = SettingTheme.SYSTEM.name)
            it.assertTheme(false)
        }
    }

    @Test
    fun unknownPreferenceFallsBackToSystemTheme() = runTest {
        ThemeComposition(this, systemDark = true, theme = "").use { it.assertTheme(true) }
    }

    private class ThemeComposition(
        private val scope: TestScope,
        systemDark: Boolean,
        theme: String = SettingTheme.SYSTEM.name,
    ) : AutoCloseable {
        private val systemDarkState = mutableStateOf(systemDark)
        private val themeState = mutableStateOf(theme)
        private val frameClock = BroadcastFrameClock()
        private val recomposer = Recomposer(scope.coroutineContext + frameClock)
        private val composition = Composition(ThemeApplier(), recomposer)
        private val recompositionJob = scope.backgroundScope.launch(
            UnconfinedTestDispatcher(scope.testScheduler) + frameClock,
        ) { recomposer.runRecomposeAndApplyChanges() }
        private var frameTimeNanos = 0L
        private var observedTheme: Pair<Boolean, Boolean>? = null

        init {
            composition.setContent {
                // 复现 Nucleus 2.5.18 向 Compose 的主题容器写入 Skiko 枚举的行为。
                @Suppress("UNCHECKED_CAST")
                val nucleusTheme = LocalSystemTheme as ProvidableCompositionLocal<Any>
                CompositionLocalProvider(
                    nucleusTheme provides if (systemDarkState.value) {
                        SkikoSystemTheme.DARK
                    } else {
                        SkikoSystemTheme.LIGHT
                    },
                ) {
                    ProvideDesktopTheme(themeState.value, systemDarkState.value) { darkTheme ->
                        val composeDarkTheme = isSystemInDarkTheme()
                        SideEffect { observedTheme = darkTheme to composeDarkTheme }
                    }
                }
            }
        }

        fun update(
            systemDark: Boolean = systemDarkState.value,
            theme: String = themeState.value,
        ) {
            systemDarkState.value = systemDark
            themeState.value = theme
            Snapshot.sendApplyNotifications()
            scope.runCurrent()
            frameTimeNanos += 16_666_667L
            frameClock.sendFrame(frameTimeNanos)
            scope.runCurrent()
        }

        fun assertTheme(expected: Boolean) {
            assertEquals(expected to expected, observedTheme, "窗口与 Compose 组件的主题应保持一致")
        }

        override fun close() {
            composition.dispose()
            recomposer.cancel()
            recompositionJob.cancel()
        }
    }

    private class ThemeApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
