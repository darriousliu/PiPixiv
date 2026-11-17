package com.mrl.pixiv.setting

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class SignOutConfirmationTest {
    @Test
    fun cancelKeepsSettingsOpenWithoutSigningOut() = runSettingsTest {
        var signOuts = 0
        setContent { Settings(onSignOut = { signOuts++ }) }

        openConfirmation()
        onNodeWithText("取消").performClick()

        onNode(isDialog()).assertDoesNotExist()
        onNodeWithText("登出").assertIsDisplayed()
        runOnIdle { assertEquals(0, signOuts) }
    }

    @Test
    fun escapeDismissesTheDialogWithoutSigningOut() = runSettingsTest {
        var signOuts = 0
        setContent { Settings(onSignOut = { signOuts++ }) }

        openConfirmation()
        onNodeWithText("取消").performKeyInput { pressKey(Key.Escape) }

        onNode(isDialog()).assertDoesNotExist()
        onNodeWithText("登出").assertIsDisplayed()
        runOnIdle { assertEquals(0, signOuts) }
    }

    @Test
    fun restoredDialogSignsOutOnlyOnceAfterExplicitConfirmation() = runSettingsTest {
        var signOuts = 0
        var registry by mutableStateOf(SaveableStateRegistry(null) { true })
        var showSettings by mutableStateOf(true)
        setContent {
            if (showSettings) {
                CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                    Settings(onSignOut = { signOuts++ })
                }
            }
        }

        openConfirmation()
        val savedState = runOnIdle {
            registry.performSave().also { showSettings = false }
        }
        runOnIdle {
            registry = SaveableStateRegistry(savedState) { true }
            showSettings = true
        }

        onNodeWithText("确定要登出吗？").assertIsDisplayed()
        runOnIdle { assertEquals(0, signOuts) }
        onNode(hasText("登出") and hasAnyAncestor(isDialog()))
            .performSemanticsAction(SemanticsActions.OnClick) { click ->
                click()
                click()
            }

        onNode(isDialog()).assertDoesNotExist()
        runOnIdle { assertEquals(1, signOuts) }
    }

    @Test
    fun navigationChangeDismissesTheDialogEvenWhenSettingsStayComposed() = runSettingsTest {
        var signOuts = 0
        var topEntryId by mutableStateOf("settings")
        setContent { Settings(topEntryId, onSignOut = { signOuts++ }) }

        openConfirmation()
        runOnIdle { topEntryId = "detail" }
        onNode(isDialog()).assertDoesNotExist()
        runOnIdle { topEntryId = "settings" }

        onNode(isDialog()).assertDoesNotExist()
        onNodeWithText("登出").assertIsDisplayed()
        runOnIdle { assertEquals(0, signOuts) }
    }

    private fun runSettingsTest(block: ComposeUiTest.() -> Unit) {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runDesktopComposeUiTest(width = 390, height = 700, testTimeout = 30.seconds) { block() }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun ComposeUiTest.openConfirmation() {
        onNode(hasScrollToKeyAction()).performScrollToKey("sign_out")
        onNodeWithText("登出").performClick()
        onNodeWithText("确定要登出吗？").assertIsDisplayed()
    }

    @Composable
    private fun Settings(topEntryId: String = "settings", onSignOut: () -> Unit) {
        MaterialExpressiveTheme {
            SettingContent(
                currentLanguage = "简体中文",
                languages = emptyList(),
                hasNewVersion = false,
                useLiquidGlassNavigation = true,
                onLanguageChange = {},
                onUseLiquidGlassNavigationChange = {},
                onBack = {},
                onNavigate = {},
                onExportToken = {},
                onSignOut = onSignOut,
                topEntryId = topEntryId,
            )
        }
    }
}
