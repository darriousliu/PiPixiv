package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.LocalSearchFilter
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.search.SearchHistory
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
import com.mrl.pixiv.common.mmkv.clearAll
import com.mrl.pixiv.common.mmkv.mmkvBool
import com.mrl.pixiv.common.mmkv.mmkvBytes
import com.mrl.pixiv.common.mmkv.mmkvLong
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import kotlinx.serialization.Serializable
import kotlin.text.CharacterCodingException

enum class LegacyPreferenceOutcome { Done, Skipped, NeedsUser, Corrupted }

data class LegacyPreferenceResult(val outcome: LegacyPreferenceOutcome, val counts: Map<String, Long>)

@Serializable
data class LegacyPreferencesPackage(
    val userInfo: UserDetailResp? = null,
    val userPreference: UserPreference? = null,
    val searchAndTags: LegacySearchAndTags = LegacySearchAndTags(),
    val novelSeriesProgress: Map<String, NovelSeriesReadingProgress> = emptyMap(),
) {
    val sourceUserId get() = userInfo?.user?.id?.takeIf { it > 0 }

    fun counts(): Map<String, Long> = searchAndTags.counts() + mapOf(
        "userInfo" to (userInfo != null).toCount(),
        "userPreference" to (userPreference != null).toCount(),
        "novelSeriesProgress" to novelSeriesProgress.size.toLong(),
    )
}

@Serializable
data class LegacySearchAndTags(
    val searchHistory: IllustSearch? = null,
    val novelSearchHistory: NovelSearch? = null,
    val searchIdHistory: Set<String>? = null,
    val novelSearchIdHistory: Set<String>? = null,
    val bookmarkedTags: List<Tag>? = null,
    @Suppress("DEPRECATION")
    val savedSearchFilter: LocalSearchFilter? = null,
    val rememberSearchFilter: Boolean? = null,
) {
    internal fun hasData() = searchHistory != null || novelSearchHistory != null || searchIdHistory != null ||
        novelSearchIdHistory != null || bookmarkedTags != null || savedSearchFilter != null || rememberSearchFilter != null

    internal fun counts() = mapOf(
        "searchHistory" to searchHistory?.searchHistoryList.orEmpty().size.toLong(),
        "novelSearchHistory" to novelSearchHistory?.novelSearchHistory.orEmpty().size.toLong(),
        "searchIdHistory" to searchIdHistory.orEmpty().size.toLong(),
        "novelSearchIdHistory" to novelSearchIdHistory.orEmpty().size.toLong(),
        "bookmarkedTags" to bookmarkedTags.orEmpty().size.toLong(),
    )
}

class LegacyAccountPreferencesMigration(
    private val app: MMKVOwner = AccountStore,
    private val legacy: MMKVOwner = mmkvOwner("pixiv.user"),
    private val account: (Long) -> MMKVOwner = ::accountMMKVOwner,
) {
    val sessions = if (app === AccountStore) AccountStore.storage else AccountSessionStorage(app)

    fun legacyUserId(): Long? = LegacyFields(legacy).userInfo()?.user?.id

    fun migrateSession(): LegacyPreferenceResult = withAccountStorageLock {
        val fields = LegacyFields(legacy)
        if (sessions.hasCorruptState()) {
            return@withAccountStorageLock fields.result(LegacyPreferenceOutcome.Corrupted, "accounts" to 0L)
        }
        val profile = fields.userInfo()
        val refreshToken = fields.token("userRefreshToken")
        val accessToken = fields.token("userAccessToken")
        val expiresAt = fields.expirationTime()
        val outcome = when {
            profile != null && sessions.find(profile.user.id) != null -> LegacyPreferenceOutcome.Done
            profile != null && !refreshToken.isNullOrEmpty() && !accessToken.isNullOrEmpty() -> {
                val user = profile.user
                val target = account(user.id)
                target.mmkvSerializable(UserDetailResp()).writeIfAbsent(target, "userInfo", profile)
                target.sync()
                sessions.addMigratedAccount(
                    AccountSession(
                        userId = user.id,
                        name = user.name,
                        account = user.account,
                        avatarUrl = user.profileImageUrls.medium,
                        isPremium = profile.profile.isPremium,
                        refreshToken = refreshToken,
                        accessToken = accessToken,
                        accessTokenExpiresAt = expiresAt,
                    )
                )
                app.sync()
                LegacyPreferenceOutcome.Done
            }
            profile != null || !refreshToken.isNullOrEmpty() || !accessToken.isNullOrEmpty() ||
                expiresAt != 0L || fields.hasCorruption -> {
                sessions.setRecoverAfterLogin(true)
                LegacyPreferenceOutcome.Corrupted
            }
            else -> LegacyPreferenceOutcome.Skipped
        }
        fields.result(outcome, "accounts" to sessions.sessions.size.toLong())
    }

    fun migrateSettings(targetUserId: Long? = null, allowLegacyUserId: Boolean = true): LegacyPreferenceResult =
        withAccountStorageLock {
            val fields = LegacyFields(legacy)
            val preference = fields.read<UserPreference>("userPreference")
                ?: return@withAccountStorageLock fields.result(fields.emptyOutcome())
            val userId = resolveTarget(targetUserId, allowLegacyUserId)
            val counts = copySettings(preference, userId)
            val needsUser = userId == null &&
                (sessions.recoverAfterLogin.value || LegacyFields(legacy).hasSessionFootprint())
            fields.result(if (needsUser) LegacyPreferenceOutcome.NeedsUser else LegacyPreferenceOutcome.Done, counts)
        }

    fun migrateSearchAndTags(targetUserId: Long? = null, allowLegacyUserId: Boolean = true): LegacyPreferenceResult =
        withAccountStorageLock {
            val fields = LegacyFields(legacy)
            val data = fields.searchAndTags()
            if (!data.hasData()) return@withAccountStorageLock fields.result(fields.emptyOutcome(), data.counts())
            val userId = resolveTarget(targetUserId, allowLegacyUserId)
                ?: return@withAccountStorageLock fields.result(LegacyPreferenceOutcome.NeedsUser, data.counts())
            fields.result(LegacyPreferenceOutcome.Done, mergeSearchAndTags(account(userId), data).counts())
        }

    fun migrateSeriesProgress(): LegacyPreferenceResult = withAccountStorageLock {
        val fields = LegacyFields(app)
        val byAccount = fields.seriesProgress().groupSeriesProgress()
        val rows = byAccount.entries.sumOf { (userId, progress) ->
            mergeSeriesProgress(account(userId), progress)
        }
        fields.result(
            if (byAccount.isEmpty()) fields.emptyOutcome() else LegacyPreferenceOutcome.Done,
            "novelSeriesProgress" to rows,
        )
    }

    fun readPackage(): LegacyPreferencesPackage = withAccountStorageLock {
        val fields = LegacyFields(legacy)
        LegacyPreferencesPackage(
            userInfo = fields.userInfo(),
            userPreference = fields.read<UserPreference>("userPreference"),
            searchAndTags = fields.searchAndTags(),
            novelSeriesProgress = LegacyFields(app).seriesProgress(),
        )
    }

    fun importSettings(data: LegacyPreferencesPackage, targetUserId: Long): LegacyPreferenceResult =
        withAccountStorageLock {
            require(targetUserId > 0)
            val profile = data.userInfo?.takeIf { it.user.id == targetUserId }
            if (profile != null) {
                val target = account(targetUserId)
                target.mmkvSerializable(UserDetailResp()).writeIfAbsent(target, "userInfo", profile)
                target.sync()
            }
            val preference = data.userPreference
            val counts = preference?.let { copySettings(it, targetUserId) }.orEmpty()
            val outcome = if (preference != null || profile != null) LegacyPreferenceOutcome.Done
            else LegacyPreferenceOutcome.Skipped
            LegacyPreferenceResult(outcome, counts)
        }

    fun importSearchAndTags(data: LegacyPreferencesPackage, targetUserId: Long): LegacyPreferenceResult =
        withAccountStorageLock {
            require(targetUserId > 0)
            val search = data.searchAndTags
            if (search.hasData()) {
                LegacyPreferenceResult(LegacyPreferenceOutcome.Done, mergeSearchAndTags(account(targetUserId), search).counts())
            } else {
                LegacyPreferenceResult(LegacyPreferenceOutcome.Skipped, search.counts())
            }
        }

    fun importSeriesProgress(
        data: LegacyPreferencesPackage,
        targetUserId: Long,
        sourceUserId: Long? = data.sourceUserId,
    ): LegacyPreferenceResult = withAccountStorageLock {
        require(targetUserId > 0)
        val byAccount = data.novelSeriesProgress.groupSeriesProgress()
        val originalUserId = sourceUserId ?: byAccount.keys.singleOrNull()
        val progress = byAccount[originalUserId]
        when {
            progress != null -> LegacyPreferenceResult(
                LegacyPreferenceOutcome.Done,
                mapOf("novelSeriesProgress" to mergeSeriesProgress(account(targetUserId), progress)),
            )
            originalUserId == null && byAccount.isNotEmpty() ->
                LegacyPreferenceResult(LegacyPreferenceOutcome.NeedsUser, emptyMap())
            else -> LegacyPreferenceResult(LegacyPreferenceOutcome.Skipped, emptyMap())
        }
    }

    fun clearLegacyUser() = withAccountStorageLock {
        legacy.clearAll()
        legacy.sync()
    }

    private fun resolveTarget(targetUserId: Long?, allowLegacyUserId: Boolean): Long? {
        require(targetUserId == null || targetUserId > 0)
        return targetUserId ?: if (allowLegacyUserId) legacyUserId() else null
    }

    private fun copySettings(preference: UserPreference, userId: Long?): Map<String, Long> {
        val expectedDevice = preference.toDevicePreference()
        val actualDevice = app.mmkvSerializable(DevicePreference())
            .writeIfAbsent(app, DEVICE_PREFERENCE_KEY, expectedDevice)
        app.sync()
        val expectedAccount = preference.toAccountPreference()
        val actualAccount = userId?.let {
            val target = account(it)
            target.mmkvSerializable(AccountPreference())
                .writeIfAbsent(target, ACCOUNT_PREFERENCE_KEY, expectedAccount)
                .also { target.sync() }
        }
        return mapOf(
            "deviceTheme" to (actualDevice.theme == expectedDevice.theme).toCount(),
            "deviceProxy" to (actualDevice.bypassSetting == expectedDevice.bypassSetting &&
                actualDevice.appApiProxyHost == expectedDevice.appApiProxyHost).toCount(),
            "deviceAiTranslation" to (actualDevice.aiTranslationConfig == expectedDevice.aiTranslationConfig).toCount(),
            "accountR18" to (actualAccount?.isR18Enabled == expectedAccount.isR18Enabled).toCount(),
            "accountPrivateBookmark" to
                (actualAccount?.defaultPrivateBookmark == expectedAccount.defaultPrivateBookmark).toCount(),
        )
    }
}

private const val SERIES_PROGRESS_KEY = "novelSeriesProgress"

private class LegacyFields(private val owner: MMKVOwner) {
    private var skippedCorrupted = 0L
    val hasCorruption get() = skippedCorrupted > 0

    inline fun <reified T : Any> read(key: String): T? {
        if (!owner.contains(key)) return null
        return owner.mmkvSerializable<T?>(null).readRecoverable(owner, key) { null } ?: corrupted()
    }

    fun userInfo(): UserDetailResp? {
        val profile = read<UserDetailResp>("userInfo") ?: return null
        return when {
            profile.user.id > 0 -> profile
            profile == UserDetailResp() -> null // The previous logout path wrote this default payload.
            else -> corrupted()
        }
    }

    fun token(key: String): String? {
        if (!owner.contains(key)) return null
        return try {
            owner.mmkvBytes().read(owner, key)?.decodeToString(throwOnInvalidSequence = true)
                ?.takeIf { it.isEmpty() || it.isNotBlank() } ?: corrupted()
        } catch (_: CharacterCodingException) {
            corrupted()
        }
    }

    fun expirationTime(): Long {
        val key = "accessTokenExpiresTime"
        if (!owner.contains(key)) return 0L
        val value = owner.mmkvLong(Long.MIN_VALUE).read(owner, key)
        if (value >= 0L) return value
        corrupted()
        return 0L
    }

    fun hasSessionFootprint(): Boolean = userInfo() != null ||
        !token("userRefreshToken").isNullOrEmpty() || !token("userAccessToken").isNullOrEmpty() ||
        expirationTime() != 0L || hasCorruption

    fun seriesProgress(): Map<String, NovelSeriesReadingProgress> =
        read<Map<String, NovelSeriesReadingProgress>>(SERIES_PROGRESS_KEY).orEmpty()

    @Suppress("DEPRECATION")
    fun searchAndTags() = LegacySearchAndTags(
        searchHistory = read<IllustSearch>("searchHistory"),
        novelSearchHistory = read<NovelSearch>("novelSearchHistory"),
        searchIdHistory = readStringSet("searchIdHistory"),
        novelSearchIdHistory = readStringSet("novelSearchIdHistory"),
        bookmarkedTags = read<List<Tag>>("bookmarkedTagsDelegate"),
        savedSearchFilter = read<LocalSearchFilter>("_savedSearchFilter"),
        rememberSearchFilter = readRememberSearchFilter(),
    )

    private fun readStringSet(key: String): Set<String>? {
        if (!owner.contains(key)) return null
        return owner.mmkvStringSet().read(owner, key) ?: corrupted()
    }

    private fun readRememberSearchFilter(): Boolean? {
        val key = "_rememberSearchFilter"
        if (!owner.contains(key)) return null
        val value = owner.mmkvBool(false).read(owner, key)
        return value.takeIf { it == owner.mmkvBool(true).read(owner, key) } ?: corrupted()
    }

    private fun corrupted(): Nothing? {
        skippedCorrupted++
        return null
    }

    fun emptyOutcome() =
        if (skippedCorrupted == 0L) LegacyPreferenceOutcome.Skipped else LegacyPreferenceOutcome.Corrupted

    fun result(outcome: LegacyPreferenceOutcome, vararg counts: Pair<String, Long>) =
        result(outcome, mapOf(*counts))

    fun result(outcome: LegacyPreferenceOutcome, counts: Map<String, Long>) =
        LegacyPreferenceResult(outcome, counts + ("skippedCorrupted" to skippedCorrupted))
}

private fun Map<String, NovelSeriesReadingProgress>.groupSeriesProgress():
    Map<Long, Map<Long, NovelSeriesReadingProgress>> {
    val byAccount = mutableMapOf<Long, MutableMap<Long, NovelSeriesReadingProgress>>()
    for ((key, progress) in this) {
        val userId = key.substringBefore(':').toLongOrNull()?.takeIf { it > 0 } ?: continue
        val seriesId = key.substringAfter(':', "").toLongOrNull()?.takeIf { it > 0 } ?: continue
        byAccount.getOrPut(userId) { mutableMapOf() }[seriesId] = progress
    }
    return byAccount
}

private fun mergeSeriesProgress(owner: MMKVOwner, progress: Map<Long, NovelSeriesReadingProgress>): Long {
    val field = owner.mmkvSerializable<Map<Long, NovelSeriesReadingProgress>>(emptyMap())
    val merged = field.merge(owner, SERIES_PROGRESS_KEY, progress) { current, incoming ->
        current + incoming.filterKeys { it !in current }
    }
    owner.sync()
    return merged.size.toLong()
}

private fun mergeSearchAndTags(owner: MMKVOwner, data: LegacySearchAndTags): LegacySearchAndTags {
    val illust = data.searchHistory?.let {
        owner.mmkvSerializable(IllustSearch()).merge(owner, "searchHistory", it) { current, incoming ->
            IllustSearch(mergeSearches(current.searchHistoryList, incoming.searchHistoryList))
        }
    }
    val novel = data.novelSearchHistory?.let {
        owner.mmkvSerializable(NovelSearch()).merge(owner, "novelSearchHistory", it) { current, incoming ->
            NovelSearch(mergeSearches(current.novelSearchHistory, incoming.novelSearchHistory))
        }
    }
    val ids = owner.mmkvStringSet()
    val illustIds = data.searchIdHistory?.let {
        ids.merge(owner, "searchIdHistory", it) { current, incoming -> current.orEmpty() + incoming.orEmpty() }
    }
    val novelIds = data.novelSearchIdHistory?.let {
        ids.merge(owner, "novelSearchIdHistory", it) { current, incoming -> current.orEmpty() + incoming.orEmpty() }
    }
    val tags = data.bookmarkedTags?.let {
        owner.mmkvSerializable<List<Tag>>(emptyList()).merge(owner, "bookmarkedTagsDelegate", it) { current, incoming ->
            (current + incoming).distinctBy { tag -> tag.name }
        }
    }
    @Suppress("DEPRECATION")
    data.savedSearchFilter?.let {
        owner.mmkvSerializable(LocalSearchFilter()).writeIfAbsent(owner, "_savedSearchFilter", it)
    }
    data.rememberSearchFilter?.let {
        owner.mmkvBool().writeIfAbsent(owner, "_rememberSearchFilter", it)
    }
    owner.sync()
    return data.copy(
        searchHistory = illust,
        novelSearchHistory = novel,
        searchIdHistory = illustIds,
        novelSearchIdHistory = novelIds,
        bookmarkedTags = tags,
    )
}

private fun mergeSearches(current: List<SearchHistory>, incoming: List<SearchHistory>) =
    (current + incoming).distinctBy { it.keyword }

private fun <T> MMKVField<T>.writeIfAbsent(owner: MMKVOwner, key: String, value: T): T {
    if (!owner.contains(key)) {
        write(owner, key, value)
        return value
    }
    return read(owner, key)
}

private fun <T> MMKVField<T>.merge(owner: MMKVOwner, key: String, incoming: T, combine: (T, T) -> T): T {
    val current = read(owner, key)
    val merged = combine(current, incoming)
    if (merged != current) {
        write(owner, key, merged)
    }
    return merged
}

private fun Boolean.toCount() = if (this) 1L else 0L
