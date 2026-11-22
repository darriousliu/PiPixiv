package com.mrl.pixiv.splash

import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.util.isDebug
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.filesDir
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountDebugTest {
    @Test
    fun forcedInvalidGrantSkipsNetworkAndIsConsumedOnlyOnce() = runBlocking {
        assumeTrue(isDebug)
        withAccountDebugConfig("""{"forceInvalidGrantOnce":true}""") { file ->
            var requests = 0
            withSuccessfulRefresh(onRequest = { requests++ }) {
                assertEquals("", AuthManager.requireUserAccessToken(force = true))
                assertTrue(AccountStore.current?.needsRelogin == true)
                assertEquals(0, requests)
                assertFalse(file.exists())

                AccountStore.saveAndActivate(session)
                assertEquals("new-access-token", AuthManager.requireUserAccessToken(force = true))
                assertEquals(1, requests)
                assertFalse(AccountStore.current?.needsRelogin == true)
            }
        }
    }

    @Test
    fun startupExpirationIsConsumedOnlyOnceAndPreservesPendingInvalidGrant() = runBlocking {
        assumeTrue(isDebug)
        withAccountDebugConfig("""{"forceInvalidGrantOnce":true,"expireAccessTokenOnce":true}""") { file ->
            AuthManager.applyStartupDebugOverrides()
            assertTrue(AccountStore.current?.hasExpired() == true)

            AccountStore.saveAndActivate(session)
            AuthManager.applyStartupDebugOverrides()
            assertEquals(session, AccountStore.current)

            withSuccessfulRefresh(onRequest = { error("Pending invalid grant must skip the request") }) {
                assertEquals("", AuthManager.requireUserAccessToken(force = true))
                assertTrue(AccountStore.current?.needsRelogin == true)
                assertFalse(file.exists())
            }
        }
    }

    @Test
    fun releaseBuildIgnoresBothConfiguredFlags() = runBlocking {
        assumeFalse(isDebug)
        val config = """{"forceInvalidGrantOnce":true,"expireAccessTokenOnce":true}"""
        withAccountDebugConfig(config) { file ->
            AuthManager.applyStartupDebugOverrides()
            assertEquals(session, AccountStore.current)
            withSuccessfulRefresh {
                assertEquals("new-access-token", AuthManager.requireUserAccessToken(force = true))
                assertEquals(config, file.readText())
            }
        }
    }

    @Test
    fun malformedConfigurationDoesNotAffectStartupOrRefresh() = runBlocking {
        assumeTrue(isDebug)
        val malformed = """{"forceInvalidGrantOnce":true,"expireAccessTokenOnce":"""
        withAccountDebugConfig(malformed) { file ->
            AuthManager.applyStartupDebugOverrides()
            assertEquals(session, AccountStore.current)
            withSuccessfulRefresh {
                assertEquals("new-access-token", AuthManager.requireUserAccessToken(force = true))
                assertEquals(malformed, file.readText())
            }
        }
    }

    private suspend fun withAccountDebugConfig(config: String, verify: suspend (File) -> Unit) {
        initializeMigrationTestStorage()
        val file = File(FileKit.filesDir.absolutePath(), "account-debug.json")
        check(!file.exists())
        file.writeText(config)
        try {
            AccountStore.saveAndActivate(session)
            verify(file)
        } finally {
            file.delete()
            AccountStore.storage.remove(
                session.userId,
                replacementUserId = null,
                lastUsedAt = 0L,
                discardLocalData = false,
            )
        }
    }

    private suspend fun withSuccessfulRefresh(onRequest: () -> Unit = {}, verify: suspend () -> Unit) =
        withMockAccountRefresh(handler = {
            onRequest()
            respond(
                """{"access_token":"new-access-token","refresh_token":"new-refresh-token",
                   "expires_in":3600,"user":{"id":"${session.userId}"}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }, block = verify)

    private val session = AccountSession(
        userId = 490001L,
        name = "Reader",
        account = "reader",
        refreshToken = "refresh-token",
        accessToken = "access-token",
        accessTokenExpiresAt = Long.MAX_VALUE,
    )
}
