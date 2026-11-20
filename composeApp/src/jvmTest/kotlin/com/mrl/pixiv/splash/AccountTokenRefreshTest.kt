package com.mrl.pixiv.splash

import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.network.AuthClient
import com.mrl.pixiv.common.network.RoutedHttpClient
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.serialize.JSON
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AccountTokenRefreshTest {
    @Test
    fun refreshResponsesAffectOnlyTheAccountThatStartedTheRequest() = runBlocking {
        initializeMigrationTestStorage()
        val requests = Channel<Pair<String, CompletableDeferred<RefreshReply>>>()
        val engine = MockEngine { request ->
            val token = assertNotNull(assertIs<FormDataContent>(request.body).formData["refresh_token"])
            val pending = CompletableDeferred<RefreshReply>()
            requests.send(token to pending)
            val reply = pending.await()
            respond(reply.body, reply.status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(JSON) }
        }
        val route = RoutedHttpClient({ BypassSetting.Direct }, { client }, HttpClient::close)
        val authModule = module { single(named<AuthClient>()) { route } }
        startKoin { modules(authModule) }
        try {
            withTimeout(10.seconds) {
                val first = AccountSession(
                    userId = 430001L,
                    name = "Reader One",
                    account = "reader-one",
                    refreshToken = "old-refresh-one",
                    accessToken = "old-access-one",
                    accessTokenExpiresAt = 0L,
                )
                val second = first.copy(
                    userId = 430002L,
                    name = "Reader Two",
                    account = "reader-two",
                    refreshToken = "refresh-two",
                    accessToken = "access-two",
                    accessTokenExpiresAt = Long.MAX_VALUE,
                )
                AccountStore.saveAndActivate(second)
                AccountStore.saveAndActivate(first)

                val refresh = async { AuthManager.requireUserAccessToken() }
                val (oldToken, success) = requests.receive()
                assertEquals(first.refreshToken, oldToken)
                AccountStore.saveAndActivate(second)
                success.complete(RefreshReply(HttpStatusCode.OK, """
                    {"access_token":"new-access-one","refresh_token":"new-refresh-one",
                     "expires_in":3600,"user":{"id":"${first.userId}"}}
                """.trimIndent()))

                assertEquals("new-access-one", refresh.await())
                val refreshed = assertNotNull(AccountStore.find(first.userId))
                assertEquals("new-refresh-one", refreshed.refreshToken)
                assertEquals(second, AccountStore.current)

                AccountStore.saveAndActivate(refreshed)
                val invalidation = async { AuthManager.requireUserAccessToken(force = true) }
                val (rotatedToken, invalidGrant) = requests.receive()
                assertEquals(refreshed.refreshToken, rotatedToken)
                AccountStore.saveAndActivate(second)
                invalidGrant.complete(RefreshReply(HttpStatusCode.BadRequest, """{"error":"invalid_grant"}"""))

                assertEquals("", invalidation.await())
                assertTrue(assertNotNull(AccountStore.find(first.userId)).needsRelogin)
                assertEquals(second, AccountStore.current)
            }
        } finally {
            for (userId in listOf(430001L, 430002L)) {
                AccountStore.storage.remove(userId, replacementUserId = null, lastUsedAt = 0L, discardLocalData = false)
            }
            stopKoin()
            requests.cancel()
            client.close()
            engine.close()
        }
    }

    private data class RefreshReply(val status: HttpStatusCode, val body: String)
}
