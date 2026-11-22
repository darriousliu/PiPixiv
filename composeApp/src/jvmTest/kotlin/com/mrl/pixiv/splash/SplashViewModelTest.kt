package com.mrl.pixiv.splash

import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.strings.network_error
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SplashViewModelTest {
    @Test
    fun expiredSessionOpensHomeOfflineAndRepeatedFailuresOnlyShowOneToast() = withMainDispatcher {
        val attempts = AtomicInteger()
        val messages = mutableListOf<Any>()
        // Receive each toast on the sender's Swing turn before checking the queue.
        val collector = launch(Dispatchers.Swing.immediate, start = CoroutineStart.UNDISPATCHED) {
            ToastUtil.toastFlow.collect { messages.add(it.message) }
        }
        try {
            withSplashAfterRefresh(handler = {
                attempts.incrementAndGet()
                throw IOException("Offline")
            }) { state, accounts ->
                assertEquals(Destination.Main, state.startDestination)
                assertEquals(expiredSession.userId, accounts.current.value?.userId)

                assertEquals("", AuthManager.requireUserAccessToken(expiredSession.userId))
                assertEquals(2, attempts.get())
                assertEquals(expiredSession, AccountStore.current)
                withContext(Dispatchers.Swing) {
                    assertEquals<List<Any>>(listOf(AppUtil.getString(RStrings.network_error)), messages)
                }
            }
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test
    fun expiredSessionOpensLoginWhenRefreshGrantIsInvalid() = withMainDispatcher {
        withSplashAfterRefresh(handler = {
            respond(
                """{"error":"invalid_grant"}""",
                HttpStatusCode.BadRequest,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { state, accounts ->
            assertEquals(Destination.LoginOption, state.startDestination)
            assertTrue(AccountStore.current?.needsRelogin == true)
            assertNull(accounts.current.value)
        }
    }

    private fun withMainDispatcher(block: suspend CoroutineScope.() -> Unit) = runBlocking {
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            withTimeout(20.seconds, block)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private suspend fun withSplashAfterRefresh(
        handler: MockRequestHandler,
        verify: suspend (SplashState, AccountDatabaseProvider) -> Unit,
    ) = withMockAccountRefresh(handler = handler) {
        initializeMigrationTestStorage()
        val directory = createTempDirectory("pipixiv-splash-test").toFile()
        try {
            MigrationTestRun(directory).useRun { run ->
                assertTrue(run.start())
                AccountStore.saveAndActivate(expiredSession)
                run.accounts.publish(expiredSession.userId)
                val viewModel = SplashViewModel(run.migration, run.accounts)
                try {
                    verify(viewModel.uiState.first { !it.isLoading }, run.accounts)
                } finally {
                    viewModel.viewModelScope.cancel()
                }
            }
        } finally {
            AccountStore.storage.remove(
                expiredSession.userId,
                replacementUserId = null,
                lastUsedAt = 0L,
                discardLocalData = false,
            )
            directory.deleteRecursively()
        }
    }

    private val expiredSession = AccountSession(
        userId = 450001L,
        name = "Reader",
        account = "reader",
        refreshToken = "refresh-token",
        accessToken = "expired-access-token",
        accessTokenExpiresAt = 0L,
    )
}
