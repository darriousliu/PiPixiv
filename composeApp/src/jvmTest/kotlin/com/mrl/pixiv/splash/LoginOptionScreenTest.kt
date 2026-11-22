package com.mrl.pixiv.splash

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.login.LoginOptionScreen
import com.mrl.pixiv.theme.PiPixivTheme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class LoginOptionScreenTest {
    @Test
    fun savedAlternativeProvidesNavigationOnlyFromInitialLogin() {
        initializeMigrationTestStorage()
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        AccountStore.saveAndActivate(account(ACTIVE_ID).copy(needsRelogin = true))
        try {
            runDesktopComposeUiTest(width = 390, height = 700, testTimeout = 30.seconds) {
                val navigation = NavigationManager(Destination.LoginOption)
                var addAccount by mutableStateOf(false)
                setContent {
                    val owner = remember {
                        object : LifecycleOwner {
                            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply {
                                currentState = Lifecycle.State.RESUMED
                            }
                        }
                    }
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        PiPixivTheme {
                            LoginOptionScreen(navigationManager = navigation, addAccount = addAccount)
                        }
                    }
                }

                val switchAccount = onNodeWithText("切换到其他账号")
                switchAccount.assertDoesNotExist()

                runOnIdle { AccountStore.storage.addMigratedAccount(account(OTHER_ID)) }
                switchAccount.assertIsDisplayed()

                runOnIdle { addAccount = true }
                switchAccount.assertDoesNotExist()

                runOnIdle { addAccount = false }
                switchAccount.performClick()

                runOnIdle {
                    assertEquals(
                        listOf(Destination.LoginOption, Destination.AccountManagement),
                        navigation.backStack.map { it.destination },
                    )
                }
            }
        } finally {
            AccountStore.storage.remove(OTHER_ID, null, 0L, discardLocalData = false)
            AccountStore.storage.remove(ACTIVE_ID, null, 0L, discardLocalData = false)
            Locale.setDefault(previousLocale)
        }
    }

    private fun account(userId: Long) = AccountSession(
        userId = userId,
        name = "Reader",
        account = "reader-$userId",
        refreshToken = "test-refresh-token",
        accessToken = "test-access-token",
        accessTokenExpiresAt = Long.MAX_VALUE,
    )

    private companion object {
        const val ACTIVE_ID = 440004L
        const val OTHER_ID = 440005L
    }
}
