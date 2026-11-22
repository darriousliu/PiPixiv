package com.mrl.pixiv.splash

import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.ai.internal.AiHttpClientHolder
import com.mrl.pixiv.common.ai.provider.ClaudeTextClient
import com.mrl.pixiv.common.ai.provider.GeminiTextClient
import com.mrl.pixiv.common.ai.provider.OpenAiTextClient
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.network.ApiClient
import com.mrl.pixiv.common.network.RoutedHttpClient
import com.mrl.pixiv.common.repository.AccountSessionManager
import com.mrl.pixiv.common.repository.NetworkFeatureImpl
import com.mrl.pixiv.common.repository.NovelAiTranslationService
import com.mrl.pixiv.common.repository.NovelReadLaterRepository
import com.mrl.pixiv.common.repository.NovelSeriesProgressRepository
import com.mrl.pixiv.common.repository.NovelTranslationRepository
import com.mrl.pixiv.common.repository.PixivNovelReadLaterSource
import com.mrl.pixiv.common.repository.UserManager
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.login.oauth.OAuthLoginAction
import com.mrl.pixiv.login.oauth.OAuthLoginViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.SystemFileSystem
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class FreshTokenLoginRegressionTest {
    private fun assertAccountLoginIsIndependentOfWebPkce() {
        val facade = Class.forName(
            "com.mrl.pixiv.login.LoginUtilKt",
            false,
            OAuthLoginViewModel::class.java.classLoader,
        )
        val methodNames = facade.declaredMethods.map { it.name }.toSet()
        assertTrue(methodNames.containsAll(setOf("generateWebViewUrl", "checkUri")))
        val sharedHelpers = methodNames.intersect(setOf("canFinishLogin", "recordLoginAttempt"))
        assertTrue(sharedHelpers.isEmpty(), "Web PKCE facade also declares Token login helpers: $sharedHelpers")
    }

    @Test
    fun firstTokenLoginPublishesAccountWithoutUsingWebPkce() = runBlocking {
        Dispatchers.setMain(Dispatchers.Swing)
        initializeMigrationTestStorage()
        val directory = createTempDirectory("pipixiv-fresh-token-regression").toFile()
        val requestReceived = CompletableDeferred<Unit>()
        val allowResponse = CompletableDeferred<Unit>()
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
        val profileClient = HttpClient(MockEngine) {
            engine { addHandler { respond("""{"user":{"id":$ACCOUNT_ID}}""", headers = jsonHeaders) } }
            install(ContentNegotiation) { json(JSON) }
        }
        try {
            withMockAccountRefresh(handler = { request ->
                val form = (request.body as? FormDataContent)?.formData ?: error("Expected an OAuth form request")
                check(request.method == HttpMethod.Post && request.url.encodedPath == "/auth/token")
                check(form["grant_type"] == "refresh_token" && form["refresh_token"] == "fixture-sign-in")
                requestReceived.complete(Unit)
                allowResponse.await()
                respond(
                    """{"access_token":"fixture-access","refresh_token":"fixture-refresh","expires_in":3600,"user":{"id":"$ACCOUNT_ID"}}""",
                    headers = jsonHeaders,
                )
            }) {
                loadKoinModules(module {
                    single { NovelSeriesProgressRepository() }
                    single(named<ApiClient>()) {
                        RoutedHttpClient({ BypassSetting.Direct }, { profileClient }, HttpClient::close)
                    }
                })
                MigrationTestRun(directory).useRun { run ->
                    assertTrue(run.start())
                    assertTrue(AccountStore.sessions.isEmpty())
                    val accountDatabase = run.accounts.databasePath(ACCOUNT_ID)
                    assertFalse(SystemFileSystem.exists(accountDatabase))

                    val navigation = NavigationManager(Destination.LoginOption)
                    navigation.navigate(Destination.OAuthLogin)
                    val loginEntry = navigation.forEntry(checkNotNull(navigation.topEntryId))
                    val holder = AiHttpClientHolder(NetworkFeatureImpl())
                    val readLater = NovelReadLaterRepository(
                        run.accounts,
                        NovelTranslationRepository(run.database.open().novelTranslationDao()),
                        NovelAiTranslationService(OpenAiTextClient(holder), ClaudeTextClient(holder), GeminiTextClient(holder)),
                        PixivNovelReadLaterSource(),
                    )
                    val sessions = AccountSessionManager(run.accounts, navigation, readLater)
                    val viewModel = OAuthLoginViewModel(sessions)
                    try {
                        withTimeout(20.seconds) {
                            viewModel.dispatch(OAuthLoginAction.Login("fixture-sign-in") {
                                loginEntry.entryId == loginEntry.topEntryId
                            })
                            requestReceived.await()
                            assertTrue(viewModel.uiState.value.loading)
                            allowResponse.complete(Unit)
                            viewModel.uiState.first { !it.loading }
                        }

                        assertEquals(ACCOUNT_ID, AccountStore.activeUserId)
                        assertEquals(ACCOUNT_ID, UserManager.profileForAccount(ACCOUNT_ID)?.user?.id)
                        assertEquals(ACCOUNT_ID, run.accounts.requireCurrent().userId)
                        assertTrue(SystemFileSystem.exists(accountDatabase))
                        assertEquals(Destination.Main, navigation.backStack.single().destination)
                        assertAccountLoginIsIndependentOfWebPkce()
                    } finally {
                        viewModel.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
                        sessions.remove(ACCOUNT_ID, deleteLocalData = true)
                    }
                }
            }
        } finally {
            profileClient.close()
            directory.deleteRecursively()
            Dispatchers.resetMain()
        }
        Unit
    }

    private companion object {
        const val ACCOUNT_ID = 460001L
    }
}
