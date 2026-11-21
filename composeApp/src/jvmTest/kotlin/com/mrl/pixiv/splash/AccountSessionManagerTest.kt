package com.mrl.pixiv.splash

import com.mrl.pixiv.common.ai.internal.AiHttpClientHolder
import com.mrl.pixiv.common.ai.provider.ClaudeTextClient
import com.mrl.pixiv.common.ai.provider.GeminiTextClient
import com.mrl.pixiv.common.ai.provider.OpenAiTextClient
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.MetaSinglePage
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.auth.AuthTokenResp
import com.mrl.pixiv.common.data.auth.AuthUser
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.GlobalDatabaseProvider
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.repository.AccountSessionManager
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.IllustCacheRepo
import com.mrl.pixiv.common.repository.NetworkFeatureImpl
import com.mrl.pixiv.common.repository.NovelAiTranslationService
import com.mrl.pixiv.common.repository.NovelReadLaterRepository
import com.mrl.pixiv.common.repository.NovelSeriesProgressRepository
import com.mrl.pixiv.common.repository.NovelTranslationRepository
import com.mrl.pixiv.common.repository.PixivNovelReadLaterSource
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AccountSessionManagerTest {
    @Test
    fun switchingReloadsAccountDataAndReplacesNavigation() = withSessionManager { sessions, databases, navigation ->
        val history = BrowsingHistoryRepository(databases)
        val tag = Tag("only-first")
        val illust = testIllust()
        sessions.addOrUpdate(response(FIRST))
        val firstDatabase = databases.requireCurrent()
        SearchRepository.addSearchHistory("first search")
        BookmarkedTagRepository.addTag(tag)
        history.recordIllust(illust)

        assertFailsWith<CancellationException> { sessions.addOrUpdate(response(SECOND)) { false } }
        assertEquals(listOf(FIRST), AccountStore.sessions.map { it.userId })
        assertSame(firstDatabase, databases.requireCurrent())

        sessions.addOrUpdate(response(SECOND))
        sessions.addOrUpdate(response(SECOND, refreshToken = "updated"))
        assertEquals(2, AccountStore.sessions.size)
        assertEquals("updated", AccountStore.current?.refreshToken)

        sessions.switchTo(FIRST)
        navigation.navigateToSettingScreen()
        val oldEntryId = navigation.topEntryId
        BookmarkState.updateIllustBookmarkDetail(illust.id, true, Restrict.PUBLIC)
        IllustCacheRepo["old-entry"] = listOf(illust)

        sessions.switchTo(SECOND)
        assertEquals(Destination.Main, navigation.backStack.single().destination)
        assertNotEquals(oldEntryId, navigation.topEntryId)
        assertEquals(SECOND, databases.requireCurrent().userId)
        assertTrue(SearchRepository.searchHistoryFlow.value.searchHistoryList.isEmpty())
        assertFalse(BookmarkedTagRepository.isBookmarked(tag))
        assertTrue(BookmarkedTagRepository.bookmarkedTags.value.isEmpty())
        assertTrue(history.getLocalIllusts(10, 0).isEmpty())
        assertFalse(illust.isBookmark)
        assertTrue(IllustCacheRepo["old-entry"].isEmpty())

        sessions.switchTo(FIRST)
        assertSame(firstDatabase, databases.requireCurrent())
        assertEquals("first search", SearchRepository.searchHistoryFlow.value.searchHistoryList.single().keyword)
        assertTrue(BookmarkedTagRepository.isBookmarked(tag))
        assertEquals(listOf(tag), BookmarkedTagRepository.bookmarkedTags.value)
        assertEquals(listOf(illust), history.getLocalIllusts(10, 0))

        // Splash keeps the active ID when it sends the user to login.
        databases.clearCurrent()
        navigation.navigateToLoginOptionScreen()
        navigation.navigate(Destination.AccountManagement)

        sessions.switchTo(SECOND)
        assertEquals(SECOND, databases.requireCurrent().userId)
        assertEquals(Destination.Main, navigation.backStack.single().destination)
    }

    @Test
    fun removingCurrentAccountRestoresPreviousSessionOrOpensLogin() = withSessionManager { sessions, databases, navigation ->
        sessions.addOrUpdate(response(FIRST))
        sessions.addOrUpdate(response(SECOND))
        SearchRepository.addSearchHistory("retained search")
        val secondDirectory = checkNotNull(databases.databasePath(SECOND).parent)

        sessions.remove(SECOND, deleteLocalData = false)
        assertEquals(FIRST, AccountStore.activeUserId)
        assertEquals(FIRST, databases.requireCurrent().userId)
        assertNull(AccountStore.find(SECOND))
        assertTrue(SystemFileSystem.exists(secondDirectory))

        sessions.addOrUpdate(response(SECOND))
        assertEquals("retained search", SearchRepository.searchHistoryFlow.value.searchHistoryList.single().keyword)
        sessions.remove(SECOND, deleteLocalData = true)
        assertFalse(SystemFileSystem.exists(secondDirectory))
        assertTrue(AccountStore.storage.pendingLocalDeletions.isEmpty())

        sessions.remove(FIRST, deleteLocalData = true)
        assertNull(AccountStore.current)
        assertNull(databases.current.value)
        assertEquals(Destination.LoginOption, navigation.backStack.single().destination)
    }

    @Test
    fun removingCurrentAccountPrefersUsableCredentialsOverRecency() = withSessionManager { sessions, databases, navigation ->
        sessions.addOrUpdate(response(FIRST))
        sessions.addOrUpdate(response(SECOND))
        sessions.addOrUpdate(response(THIRD))
        AccountStore.update(FIRST) { it.copy(lastUsedAt = 1L) }
        AccountStore.update(SECOND) { it.copy(needsRelogin = true, lastUsedAt = 2L) }

        sessions.remove(THIRD, deleteLocalData = true)

        assertEquals(FIRST, AccountStore.activeUserId)
        assertEquals(FIRST, databases.requireCurrent().userId)
        assertEquals(Destination.Main, navigation.backStack.single().destination)

        sessions.remove(FIRST, deleteLocalData = true)

        assertEquals(SECOND, AccountStore.activeUserId)
        assertEquals(Destination.LoginOption, navigation.backStack.single().destination)
    }

    @Test
    fun successfulLoginClearsRecoveryFlagWhenOpeningRecovery() = withSessionManager { sessions, _, navigation ->
        AccountStore.storage.setRecoverAfterLogin(true)

        sessions.addOrUpdate(response(FIRST))

        assertEquals(
            listOf(Destination.Main, Destination.DataRecovery(afterLogin = true)),
            navigation.backStack.map { it.destination },
        )
        assertFalse(AccountStore.storage.recoverAfterLogin.value)

        AccountStore.storage.reloadRecoverAfterLogin()
        sessions.addOrUpdate(response(FIRST))
        assertEquals(Destination.Main, navigation.backStack.single().destination)
    }

    private fun withSessionManager(
        test: suspend (AccountSessionManager, AccountDatabaseProvider, NavigationManager) -> Unit,
    ) = runBlocking {
        initializeMigrationTestStorage()
        val directory = createTempDirectory("pipixiv-session-test").toFile()
        val global = GlobalDatabaseProvider { Path(directory.resolve("pixiv_db").path) }
        val databases = AccountDatabaseProvider()
        val navigation = NavigationManager(Destination.LoginOption)
        startKoin { modules(module { single { NovelSeriesProgressRepository() } }) }
        val holder = AiHttpClientHolder(NetworkFeatureImpl())
        val readLater = NovelReadLaterRepository(
            databases,
            NovelTranslationRepository(global.open().novelTranslationDao()),
            NovelAiTranslationService(OpenAiTextClient(holder), ClaudeTextClient(holder), GeminiTextClient(holder)),
            PixivNovelReadLaterSource(),
        )
        val sessions = AccountSessionManager(databases, navigation, readLater)
        try {
            test(sessions, databases, navigation)
        } finally {
            sessions.remove(FIRST, deleteLocalData = true)
            sessions.remove(SECOND, deleteLocalData = true)
            sessions.remove(THIRD, deleteLocalData = true)
            AccountStore.storage.setRecoverAfterLogin(false)
            databases.close()
            global.close()
            stopKoin()
            directory.deleteRecursively()
        }
        Unit
    }

    private fun response(userId: Long, refreshToken: String = "refresh-$userId") = AuthTokenResp(
        accessToken = "access-$userId",
        expiresIn = 3_600L,
        refreshToken = refreshToken,
        user = AuthUser(id = userId.toString(), name = "Reader $userId"),
    )

    private fun testIllust() = Illust(
        id = 1L,
        title = "Saved illustration",
        type = Type.Illust,
        imageUrls = ImageUrls(),
        user = User(),
        width = 800,
        height = 600,
        metaSinglePage = MetaSinglePage(),
        totalView = 0L,
        totalBookmarks = 0L,
        isBookmarked = false,
    )

    private companion object {
        const val FIRST = 440001L
        const val SECOND = 440002L
        const val THIRD = 440003L
    }
}
