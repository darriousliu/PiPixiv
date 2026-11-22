package com.mrl.pixiv.setting.account

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class AccountManagementTest {
    @Test
    fun selectingAnotherAccountDispatchesItsUserId() = runAccountTest {
        val switched = mutableListOf<Long>()
        setContent {
            AccountList(onSwitch = { switched.add(it) }, onAdd = {})
        }

        onNodeWithText("用户 B").assertIsDisplayed().performClick()
        runOnIdle { assertEquals(listOf(2L), switched) }
    }

    @Test
    fun addAccountOpensTheAddAccountDestination() = runAccountTest {
        val navigationManager = NavigationManager(Destination.AccountManagement)
        setContent {
            AccountList(onSwitch = {}, onAdd = navigationManager::navigateToAddAccountScreen)
        }

        onNodeWithText("添加账户").assertIsDisplayed().performClick()
        runOnIdle {
            assertEquals(Destination.AddAccount, navigationManager.currentDestination)
            navigationManager.popBackStack()
            assertEquals(Destination.AccountManagement, navigationManager.currentDestination)
        }
    }

    @Composable
    private fun AccountList(onSwitch: (Long) -> Unit, onAdd: () -> Unit) {
        MaterialExpressiveTheme {
            AccountManagementContent(
                state = AccountManagementState(
                    listOf(
                        ManagedAccount(1L, "用户 A", "user_a", "", isCurrent = true),
                        ManagedAccount(2L, "用户 B", "user_b", ""),
                    ),
                ),
                onSwitch = onSwitch,
                onRemove = { _, _ -> },
                onAdd = onAdd,
                onBack = {},
                topEntryId = "accounts",
            )
        }
    }

    private fun runAccountTest(block: ComposeUiTest.() -> Unit) {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runDesktopComposeUiTest(width = 390, height = 700, testTimeout = 30.seconds) { block() }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
