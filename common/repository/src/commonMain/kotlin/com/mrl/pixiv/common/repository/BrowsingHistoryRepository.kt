package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.setting.HistorySettings
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.AccountHandle
import com.mrl.pixiv.common.datasource.local.entity.IllustHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelHistoryEntity
import com.mrl.pixiv.common.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
@OptIn(ExperimentalCoroutinesApi::class)
class BrowsingHistoryRepository(
    private val databases: AccountDatabaseProvider,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    val userPreferenceFlow = SettingRepository.userPreferenceFlow

    suspend fun recordIllust(illust: Illust) {
        recordIllusts(listOf(illust))
    }

    suspend fun recordIllusts(illusts: List<Illust>) {
        val settings = requireUserPreferenceValue.historySettings.normalized()
        if (!settings.enabled || illusts.isEmpty()) return

        val uniqueIllusts = illusts.distinctBy { it.id }
        val account = databases.requireCurrent()
        val isPremium = requireUserInfoValue.let { it.user.id == account.userId && it.profile.isPremium }
        val dao = account.db.browsingHistoryDao()
        val userId = account.userId
        val now = currentTimeMillis()
        dao.upsertIllusts(
            uniqueIllusts.mapIndexed { index, illust ->
                IllustHistoryEntity(
                    illustId = illust.id,
                    userId = userId,
                    viewedAtMillis = now + index,
                    illustJson = json.encodeToString(illust),
                )
            }
        )
        pruneIfNeeded(account, settings)

        if (settings.cloudEnabled && isPremium && databases.current.value === account) {
            runCatching {
                PixivRepository.addIllustBrowsingHistory(uniqueIllusts.map { it.id })
            }
        }
    }

    suspend fun recordNovel(novel: Novel) {
        val settings = requireUserPreferenceValue.historySettings.normalized()
        if (!settings.enabled) return

        val account = databases.requireCurrent()
        val isPremium = requireUserInfoValue.let { it.user.id == account.userId && it.profile.isPremium }
        val dao = account.db.browsingHistoryDao()
        val userId = account.userId
        dao.upsertNovel(
            NovelHistoryEntity(
                novelId = novel.id,
                userId = userId,
                viewedAtMillis = currentTimeMillis(),
                novelJson = json.encodeToString(novel),
            )
        )
        pruneIfNeeded(account, settings)

        if (settings.cloudEnabled && isPremium && databases.current.value === account) {
            runCatching {
                PixivRepository.addNovelBrowsingHistory(novel.id)
            }
        }
    }

    suspend fun getLocalIllusts(limit: Int, offset: Int): List<Illust> {
        val account = databases.current.value ?: return emptyList()
        return account.db.browsingHistoryDao()
            .getIllusts(userId = account.userId, limit = limit, offset = offset)
            .mapNotNull { runCatching { json.decodeFromString<Illust>(it.illustJson) }.getOrNull() }
    }

    suspend fun getLocalNovels(limit: Int, offset: Int): List<Novel> {
        val account = databases.current.value ?: return emptyList()
        return account.db.browsingHistoryDao()
            .getNovels(userId = account.userId, limit = limit, offset = offset)
            .mapNotNull { runCatching { json.decodeFromString<Novel>(it.novelJson) }.getOrNull() }
    }

    fun observeLocalIllustCount(): Flow<Int> =
        databases.current.flatMapLatest { account ->
            account?.let { it.db.browsingHistoryDao().observeIllustCount(it.userId) } ?: flowOf(0)
        }.distinctUntilChanged()

    fun observeLocalNovelCount(): Flow<Int> =
        databases.current.flatMapLatest { account ->
            account?.let { it.db.browsingHistoryDao().observeNovelCount(it.userId) } ?: flowOf(0)
        }.distinctUntilChanged()

    suspend fun clearAllLocalHistory() {
        val account = databases.requireCurrent()
        val dao = account.db.browsingHistoryDao()
        dao.clearIllusts(account.userId)
        dao.clearNovels(account.userId)
    }

    private suspend fun pruneIfNeeded(account: AccountHandle, settings: HistorySettings) {
        if (!settings.autoClean || settings.unlimited) return
        val dao = account.db.browsingHistoryDao()
        dao.pruneIllusts(userId = account.userId, maxEntries = settings.maxEntries)
        dao.pruneNovels(userId = account.userId, maxEntries = settings.maxEntries)
    }
}
