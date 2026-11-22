package com.mrl.pixiv.splash

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.search.SearchHistory
import com.mrl.pixiv.common.data.setting.AccountPreference
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.data.setting.DevicePreference
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.setting.toUserPreference
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.mmkv.AccountMMKVOwner
import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountSessionStorage
import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.mmkv.mmkvBytes
import com.mrl.pixiv.common.mmkv.mmkvLong
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvString
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import com.mrl.pixiv.common.repository.LegacyAccountPreferencesMigration
import com.mrl.pixiv.common.repository.LegacyPreferenceOutcome
import com.mrl.pixiv.common.repository.NovelSeriesReadingProgress
import com.mrl.pixiv.common.repository.readDevicePreference
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegacyAccountPreferencesMigrationTest {
    @Test
    fun migratesLegacySettingsCredentialsAndAccountDataIdempotently() = withStorage { app, legacy, account ->
        val userId = 42L
        val userInfo = UserDetailResp(user = User(id = userId, name = "Fixture reader", account = "reader"))
        val preference = UserPreference(
            theme = SettingTheme.DARK.name,
            bypassSetting = UserPreference.BypassSetting.Proxy(host = "example.invalid"),
            appApiProxyHost = "https://app.example.invalid",
            isR18Enabled = true,
            defaultPrivateBookmark = true,
            aiTranslationConfig = AiTranslationConfig(apiKey = "fake-translation-key"),
        )
        val illustSearch = IllustSearch(listOf(SearchHistory("illustrations", 10L)))
        val novelSearch = NovelSearch(listOf(SearchHistory("novels", 20L)))
        val tags = listOf(Tag(name = "favorite"))
        val series = NovelSeriesReadingProgress(novelId = 7, title = "Chapter", fraction = .25f)
        val seriesFromOtherAccount = series.copy(novelId = 8, fraction = .75f)

        legacy.mmkvSerializable(UserDetailResp()).write(legacy, "userInfo", userInfo)
        legacy.mmkvSerializable(UserPreference()).write(legacy, "userPreference", preference)
        legacy.mmkvString().write(legacy, "userRefreshToken", "test-refresh-token")
        legacy.mmkvString().write(legacy, "userAccessToken", "test-access-token")
        legacy.mmkvLong().write(legacy, "accessTokenExpiresTime", 1_234L)
        legacy.mmkvSerializable(IllustSearch()).write(legacy, "searchHistory", illustSearch)
        legacy.mmkvSerializable(NovelSearch()).write(legacy, "novelSearchHistory", novelSearch)
        legacy.mmkvStringSet().write(legacy, "searchIdHistory", setOf("5"))
        legacy.mmkvStringSet().write(legacy, "novelSearchIdHistory", setOf("6"))
        legacy.mmkvSerializable(emptyList<Tag>()).write(legacy, "bookmarkedTagsDelegate", tags)
        app.mmkvSerializable(emptyMap<String, NovelSeriesReadingProgress>()).write(
            app,
            "novelSeriesProgress",
            mapOf("42:9" to series, "43:9" to seriesFromOtherAccount, "0:9" to series),
        )

        val migration = LegacyAccountPreferencesMigration(app, legacy, account)
        val counts = migration.migrateSession().counts + migration.migrateSettings().counts +
            migration.migrateSearchAndTags().counts + migration.migrateSeriesProgress().counts
        val sessions = migration.sessions
        val migrated = assertNotNull(sessions.current)
        assertEquals(userId, migrated.userId)
        assertEquals("test-refresh-token", migrated.refreshToken)
        assertEquals("test-access-token", migrated.accessToken)
        assertEquals(1_234L, migrated.accessTokenExpiresAt)

        val target = account(userId)
        val device = app.mmkvSerializable(DevicePreference()).readStrict(app, "devicePreference")
        val scoped = target.mmkvSerializable(AccountPreference()).readStrict(target, "accountPreference")
        assertEquals(preference, device.toUserPreference(scoped))
        assertEquals(userInfo, target.mmkvSerializable(UserDetailResp()).readStrict(target, "userInfo"))
        assertEquals(illustSearch, target.mmkvSerializable(IllustSearch()).readStrict(target, "searchHistory"))
        assertEquals(novelSearch, target.mmkvSerializable(NovelSearch()).readStrict(target, "novelSearchHistory"))
        assertEquals(setOf("5"), target.mmkvStringSet().read(target, "searchIdHistory"))
        assertEquals(setOf("6"), target.mmkvStringSet().read(target, "novelSearchIdHistory"))
        assertEquals(tags, target.mmkvSerializable(emptyList<Tag>()).readStrict(target, "bookmarkedTagsDelegate"))
        val progress = target.mmkvSerializable(emptyMap<Long, NovelSeriesReadingProgress>())
        assertEquals(mapOf(9L to series), progress.readStrict(target, "novelSeriesProgress"))
        val otherAccount = account(43L)
        assertEquals(mapOf(9L to seriesFromOtherAccount), progress.readStrict(otherAccount, "novelSeriesProgress"))
        assertFalse(target.contains("userAccessToken"))
        assertEquals(2L, counts["novelSeriesProgress"])
        assertEquals(1L, counts["bookmarkedTags"])

        sessions.removeCurrent()
        val anotherSession = migrated.copy(userId = 44L, name = "Current reader")
        sessions.saveAndActivate(anotherSession)
        val updatedProgress = series.copy(fraction = .5f)
        progress.write(target, "novelSeriesProgress", mapOf(9L to updatedProgress))

        assertEquals(LegacyPreferenceOutcome.Done, migration.migrateSession().outcome)
        assertEquals(anotherSession, sessions.current)
        assertEquals(migrated, sessions.find(userId))
        assertEquals(2L, migration.migrateSeriesProgress().counts["novelSeriesProgress"])
        assertEquals(mapOf(9L to updatedProgress), progress.read(target, "novelSeriesProgress"))
        assertEquals("test-access-token", legacy.mmkvString().read(legacy, "userAccessToken"))
        assertEquals(preference, legacy.mmkvSerializable(UserPreference()).readStrict(legacy, "userPreference"))
    }

    @Test
    fun signedOutLegacyInstallationKeepsDevicePreferences() = withStorage { app, legacy, account ->
        val preference = UserPreference(
            appLanguage = "ja",
            theme = SettingTheme.DARK.name,
            bypassSetting = UserPreference.BypassSetting.Direct,
            isR18Enabled = true,
        )
        legacy.mmkvSerializable(UserPreference()).write(legacy, "userPreference", preference)
        legacy.mmkvSerializable(UserDetailResp()).write(legacy, "userInfo", UserDetailResp())
        legacy.mmkvString().write(legacy, "userRefreshToken", "")
        legacy.mmkvString().write(legacy, "userAccessToken", "")
        legacy.mmkvLong().write(legacy, "accessTokenExpiresTime", 0L)

        val migration = LegacyAccountPreferencesMigration(app, legacy, account)
        val session = migration.migrateSession()
        val settings = migration.migrateSettings()

        val device = app.mmkvSerializable(DevicePreference()).readStrict(app, "devicePreference")
        assertEquals(preference.copy(isR18Enabled = false), device.toUserPreference(AccountPreference()))
        assertNull(AccountSessionStorage(app).current)
        assertFalse(migration.sessions.recoverAfterLogin.value)
        assertEquals(LegacyPreferenceOutcome.Skipped, session.outcome)
        assertEquals(LegacyPreferenceOutcome.Done, settings.outcome)
        assertEquals(1L, settings.counts["deviceTheme"])
    }

    @Test
    fun corruptedFieldsLeaveReadableMigrationItemsAndKeysIntact() = withStorage { app, legacy, account ->
        val profile = UserDetailResp(user = User(id = 42, name = "Reader"))
        legacy.mmkvSerializable(UserDetailResp()).write(legacy, "userInfo", profile)
        legacy.mmkvString().write(legacy, "userRefreshToken", "test-refresh-token")
        legacy.mmkvString().write(legacy, "userAccessToken", "test-access-token")
        val novels = NovelSearch(listOf(SearchHistory("novels", 20)))
        legacy.mmkvSerializable(NovelSearch()).write(legacy, "novelSearchHistory", novels)
        legacy.mmkvBytes().write(legacy, "userPreference", byteArrayOf(0xff.toByte()))
        legacy.mmkvBytes().write(legacy, "searchHistory", "{".encodeToByteArray())
        legacy.mmkvBytes().write(legacy, "bookmarkedTagsDelegate", byteArrayOf(0xff.toByte()))
        app.mmkvBytes().write(app, "novelSeriesProgress", "{".encodeToByteArray())

        val migration = LegacyAccountPreferencesMigration(app, legacy, account)
        assertEquals(LegacyPreferenceOutcome.Done, migration.migrateSession().outcome)
        val settings = migration.migrateSettings()
        val searchAndTags = migration.migrateSearchAndTags()
        val progress = migration.migrateSeriesProgress()

        assertEquals(LegacyPreferenceOutcome.Corrupted, settings.outcome)
        assertEquals(1L, settings.counts["skippedCorrupted"])
        assertEquals(LegacyPreferenceOutcome.Done, searchAndTags.outcome)
        assertEquals(2L, searchAndTags.counts["skippedCorrupted"])
        assertEquals(LegacyPreferenceOutcome.Corrupted, progress.outcome)
        assertEquals(1L, progress.counts["skippedCorrupted"])
        val target = account(42L)
        assertEquals(novels, target.mmkvSerializable(NovelSearch()).read(target, "novelSearchHistory"))
        assertFalse(target.contains("searchHistory"))
        assertFalse(target.contains("bookmarkedTagsDelegate"))
    }

    @Test
    fun corruptedLegacyProfileKeepsDeviceSettingsAndDefersAccountData() = withStorage { app, legacy, account ->
        legacy.mmkvBytes().write(legacy, "userInfo", byteArrayOf(0xff.toByte()))
        legacy.mmkvString().write(legacy, "userRefreshToken", "test-refresh-token")
        legacy.mmkvString().write(legacy, "userAccessToken", "test-access-token")
        val settings = UserPreference(theme = SettingTheme.DARK.name, isR18Enabled = true)
        legacy.mmkvSerializable(UserPreference()).write(legacy, "userPreference", settings)
        val oldSearch = IllustSearch(listOf(SearchHistory("duplicate", 10), SearchHistory("old", 1)))
        legacy.mmkvSerializable(IllustSearch()).write(legacy, "searchHistory", oldSearch)

        val migration = LegacyAccountPreferencesMigration(app, legacy, account)
        assertEquals(LegacyPreferenceOutcome.Corrupted, migration.migrateSession().outcome)
        assertNull(migration.sessions.current)
        assertTrue(migration.sessions.recoverAfterLogin.value)
        assertEquals(LegacyPreferenceOutcome.NeedsUser, migration.migrateSettings(allowLegacyUserId = false).outcome)
        assertEquals(settings.theme, readDevicePreference(app, legacy).theme)
        assertEquals(LegacyPreferenceOutcome.NeedsUser, migration.migrateSearchAndTags(allowLegacyUserId = false).outcome)

        val target = account(42L)
        val current = SearchHistory("duplicate", 100)
        val field = target.mmkvSerializable(IllustSearch())
        field.write(target, "searchHistory", IllustSearch(listOf(current)))
        assertEquals(LegacyPreferenceOutcome.Done, migration.migrateSearchAndTags(targetUserId = 42L).outcome)
        assertEquals(listOf(current, oldSearch.searchHistoryList.last()), field.read(target, "searchHistory").searchHistoryList)
        val newStorage = AccountSessionStorage(app)
        newStorage.reloadRecoverAfterLogin()
        assertTrue(newStorage.recoverAfterLogin.value)
        migration.sessions.setRecoverAfterLogin(false)
        assertEquals(LegacyPreferenceOutcome.NeedsUser, migration.migrateSettings().outcome)
    }

    @Test
    fun corruptAccountStoreKeepsOriginalBytes() = withStorage { app, _, _ ->
        val corrupt = byteArrayOf(0xff.toByte(), 0x2)
        val field = app.mmkvBytes()
        field.write(app, "accountStoreState", corrupt)
        val storage = AccountSessionStorage(app)

        assertNull(storage.current)
        assertTrue(storage.recoverAfterLogin.value)
        assertContentEquals(corrupt, field.read(app, "accountStoreState.corrupt"))
        field.write(app, "accountStoreState", "{".encodeToByteArray())
        assertNull(storage.current)
        assertContentEquals(corrupt, field.read(app, "accountStoreState.corrupt"))
    }

    @Test
    fun refreshedTokensAreSavedAfterLastUsedAtChanges() = withStorage { app, _, _ ->
        val account = AccountSession(
            userId = 42,
            name = "Reader",
            account = "reader",
            refreshToken = "old-refresh-token",
            accessToken = "old-access-token",
            accessTokenExpiresAt = 123,
            lastUsedAt = 100,
        )
        val storage = AccountSessionStorage(app)
        storage.saveAndActivate(account)
        val inFlightSession = assertNotNull(storage.current)
        storage.update(account.userId) { it.copy(lastUsedAt = 200, name = "Updated reader") }
        val refreshed = account.copy(
            refreshToken = "new-refresh-token",
            accessToken = "new-access-token",
            accessTokenExpiresAt = 456,
        )

        storage.saveRefreshedTokens(inFlightSession, refreshed)

        assertEquals(refreshed.copy(lastUsedAt = 200, name = "Updated reader"), storage.current)
    }

    @Test
    fun deviceSettingsFallbackReadsLegacyThemeLanguageAndNetworkWithoutWriting() = withStorage { app, legacy, _ ->
        val preference = UserPreference(
            theme = SettingTheme.DARK.name,
            appLanguage = "ja",
            bypassSetting = UserPreference.BypassSetting.Proxy(host = "example.invalid"),
            appApiProxyHost = "https://app.example.invalid",
            isR18Enabled = true,
        )
        legacy.mmkvSerializable(UserPreference()).write(legacy, "userPreference", preference)

        val startupPreference = readDevicePreference(app, legacy).toUserPreference(AccountPreference())

        assertEquals(preference.copy(isR18Enabled = false), startupPreference)
        assertFalse(app.contains("devicePreference"))
        val saved = DevicePreference(appLanguage = "en")
        app.mmkvSerializable(DevicePreference()).write(app, "devicePreference", saved)
        assertEquals(saved, readDevicePreference(app, legacy))
    }

    private fun withStorage(block: (MMKVOwner, MMKVOwner, (Long) -> MMKVOwner) -> Unit) {
        initializeMigrationTestStorage()
        val directory = Files.createTempDirectory("pipixiv-account-preferences-test").toFile()
        val oldRoot = directory.resolve("mmkv").apply { mkdirs() }.absolutePath
        val app = mmkvOwner("pixiv.app", oldRoot)
        val legacy = mmkvOwner("pixiv.user", oldRoot)
        val accounts = mutableMapOf<Long, AccountMMKVOwner>()
        try {
            block(app, legacy) { userId ->
                accounts.getOrPut(userId) { AccountMMKVOwner(userId, directory.absolutePath) }
            }
        } finally {
            accounts.values.forEach { it.kv.close() }
            app.kv.close()
            legacy.kv.close()
            directory.deleteRecursively()
        }
    }
}
