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
import com.mrl.pixiv.common.mmkv.AccountSessionStorage
import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.mmkv.mmkvLong
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvString
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import com.mrl.pixiv.common.repository.LegacyAccountPreferencesMigration
import com.mrl.pixiv.common.repository.NovelSeriesReadingProgress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LegacyAccountPreferencesMigrationTest {
    @Test
    fun migratesLegacySettingsCredentialsAndAccountDataOnce() = withStorage { app, legacy, account ->
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

        val counts = LegacyAccountPreferencesMigration.migrate(app, legacy, account)
        val sessions = AccountSessionStorage(app)
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
        assertEquals(counts, LegacyAccountPreferencesMigration.migrate(app, legacy, account))
        assertNull(sessions.current)
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

        val counts = LegacyAccountPreferencesMigration.migrate(app, legacy, account)

        val device = app.mmkvSerializable(DevicePreference()).readStrict(app, "devicePreference")
        assertEquals(preference.copy(isR18Enabled = false), device.toUserPreference(AccountPreference()))
        assertNull(AccountSessionStorage(app).current)
        assertEquals(0L, counts["accounts"])
        assertEquals(1L, counts["deviceTheme"])
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
