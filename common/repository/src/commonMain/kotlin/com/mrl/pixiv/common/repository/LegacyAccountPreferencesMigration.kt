package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.LocalSearchFilter
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.setting.AccountPreference
import com.mrl.pixiv.common.data.setting.DevicePreference
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.setting.toAccountPreference
import com.mrl.pixiv.common.data.setting.toDevicePreference
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountSessionStorage
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.MMKVField
import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.mmkv.accountMMKVOwner
import com.mrl.pixiv.common.mmkv.mmkvBool
import com.mrl.pixiv.common.mmkv.mmkvLong
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvString
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import com.mrl.pixiv.common.mmkv.withAccountStorageLock

object LegacyAccountPreferencesMigration {
    private const val completionKey = "legacyAccountPreferenceMigration"

    fun migrate(): Map<String, Long> =
        migrate(AccountStore, mmkvOwner("pixiv.user"), ::accountMMKVOwner)

    fun migrate(
        app: MMKVOwner,
        legacy: MMKVOwner,
        account: (Long) -> MMKVOwner,
    ): Map<String, Long> = withAccountStorageLock {
        val marker = app.mmkvSerializable<Map<String, Long>?>(null)
        marker.readStrict(app, completionKey)?.let { return@withAccountStorageLock it }

        val accounts = AccountSessionStorage(app)
        val legacyPreference = legacy.mmkvSerializable(UserPreference()).readStrict(legacy, "userPreference")
        val devicePreferenceField = app.mmkvSerializable(DevicePreference())
        val expectedDevicePreference = legacyPreference.toDevicePreference()
        val devicePreference = devicePreferenceField.writeIfAbsent(app, DEVICE_PREFERENCE_KEY, expectedDevicePreference)
        val hasPreference = legacy.contains("userPreference")

        val userInfo = legacy.mmkvSerializable(UserDetailResp()).readStrict(legacy, "userInfo")
        val userId = userInfo.user.id
        val refreshToken = legacy.mmkvString().read(legacy, "userRefreshToken")
        val accessToken = legacy.mmkvString().read(legacy, "userAccessToken")
        val hasAccount = userId > 0L && refreshToken.isNotEmpty() && accessToken.isNotEmpty()
        val counts = linkedMapOf(
            "accounts" to 0L,
            "searchHistory" to 0L,
            "searchIdHistory" to 0L,
            "novelSearchHistory" to 0L,
            "novelSearchIdHistory" to 0L,
            "bookmarkedTags" to 0L,
            "novelSeriesProgress" to 0L,
            "deviceTheme" to (hasPreference && devicePreference.theme == expectedDevicePreference.theme).toCount(),
            "deviceProxy" to (hasPreference &&
                devicePreference.bypassSetting == expectedDevicePreference.bypassSetting &&
                devicePreference.appApiProxyHost == expectedDevicePreference.appApiProxyHost).toCount(),
            "deviceAiTranslation" to (hasPreference &&
                devicePreference.aiTranslationConfig == expectedDevicePreference.aiTranslationConfig).toCount(),
            "accountR18" to 0L,
            "accountPrivateBookmark" to 0L,
        )

        if (hasAccount) {
            val destination = account(userId)
            val expectedAccountPreference = legacyPreference.toAccountPreference()
            val actualAccountPreference = legacy.mmkvSerializable(AccountPreference())
                .writeIfAbsent(destination, ACCOUNT_PREFERENCE_KEY, expectedAccountPreference)
            legacy.mmkvSerializable(UserDetailResp()).writeIfAbsent(destination, "userInfo", userInfo)
            counts["accountR18"] = (hasPreference &&
                actualAccountPreference.isR18Enabled == expectedAccountPreference.isR18Enabled).toCount()
            counts["accountPrivateBookmark"] = (hasPreference &&
                actualAccountPreference.defaultPrivateBookmark == expectedAccountPreference.defaultPrivateBookmark).toCount()

            val illusts = legacy.mmkvSerializable(IllustSearch())
            val novels = legacy.mmkvSerializable(NovelSearch())
            val searchIds = legacy.mmkvStringSet(emptySet())
            val bookmarks = legacy.mmkvSerializable<List<Tag>>(emptyList())

            counts["searchHistory"] = illusts.writeIfAbsent(
                destination, "searchHistory", illusts.readStrict(legacy, "searchHistory"),
            ).searchHistoryList.size.toLong()
            counts["novelSearchHistory"] = novels.writeIfAbsent(
                destination, "novelSearchHistory", novels.readStrict(legacy, "novelSearchHistory"),
            ).novelSearchHistory.size.toLong()
            counts["searchIdHistory"] = searchIds.writeIfAbsent(
                destination, "searchIdHistory", searchIds.read(legacy, "searchIdHistory"),
            ).orEmpty().size.toLong()
            counts["novelSearchIdHistory"] = searchIds.writeIfAbsent(
                destination, "novelSearchIdHistory", searchIds.read(legacy, "novelSearchIdHistory"),
            ).orEmpty().size.toLong()
            counts["bookmarkedTags"] = bookmarks.writeIfAbsent(
                destination, "bookmarkedTagsDelegate", bookmarks.readStrict(legacy, "bookmarkedTagsDelegate"),
            ).size.toLong()

            @Suppress("DEPRECATION")
            val savedSearchFilter = legacy.mmkvSerializable(LocalSearchFilter())
            savedSearchFilter.writeIfAbsent(
                destination, "_savedSearchFilter", savedSearchFilter.readStrict(legacy, "_savedSearchFilter"),
            )
            val rememberSearchFilter = legacy.mmkvBool()
            rememberSearchFilter.writeIfAbsent(
                destination, "_rememberSearchFilter", rememberSearchFilter.read(legacy, "_rememberSearchFilter"),
            )
            destination.sync()

            if (accounts.find(userId) == null) {
                accounts.saveAndActivate(
                    AccountSession(
                        userId = userId,
                        name = userInfo.user.name,
                        account = userInfo.user.account,
                        avatarUrl = userInfo.user.profileImageUrls.medium,
                        isPremium = userInfo.profile.isPremium,
                        refreshToken = refreshToken,
                        accessToken = accessToken,
                        accessTokenExpiresAt = legacy.mmkvLong().read(legacy, "accessTokenExpiresTime"),
                    )
                )
            }
            counts["accounts"] = accounts.sessions.size.toLong()
        }

        counts["novelSeriesProgress"] = migrateSeriesProgress(app, account)
        app.sync()
        marker.write(app, completionKey, counts)
        app.sync()
        counts
    }

    private fun migrateSeriesProgress(app: MMKVOwner, account: (Long) -> MMKVOwner): Long {
        val key = "novelSeriesProgress"
        val oldProgress = app.mmkvSerializable<Map<String, NovelSeriesReadingProgress>>(emptyMap())
            .readStrict(app, key)
        val byAccount = mutableMapOf<Long, MutableMap<Long, NovelSeriesReadingProgress>>()
        for ((legacyKey, progress) in oldProgress) {
            val userId = legacyKey.substringBefore(':').toLongOrNull()?.takeIf { it > 0 } ?: continue
            val seriesId = legacyKey.substringAfter(':', "").toLongOrNull()?.takeIf { it > 0 } ?: continue
            byAccount.getOrPut(userId) { mutableMapOf() }[seriesId] = progress
        }
        val field = app.mmkvSerializable<Map<Long, NovelSeriesReadingProgress>>(emptyMap())
        return byAccount.entries.sumOf { (userId, entries) ->
            val target = account(userId)
            val current = field.readStrict(target, key)
            val merged = entries + current
            if (merged != current) {
                field.write(target, key, merged)
                target.sync()
            }
            merged.size.toLong()
        }
    }

    private fun <T> MMKVField<T>.writeIfAbsent(owner: MMKVOwner, key: String, value: T): T {
        if (!owner.contains(key)) write(owner, key, value)
        return read(owner, key)
    }

    private fun Boolean.toCount() = if (this) 1L else 0L
}
