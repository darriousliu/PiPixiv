package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.setting.HistorySettings
import com.mrl.pixiv.common.datasource.local.dao.BrowsingHistoryDao
import com.mrl.pixiv.common.datasource.local.entity.IllustHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelHistoryEntity
import com.mrl.pixiv.common.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single
@OptIn(ExperimentalCoroutinesApi::class)
class BrowsingHistoryRepository(
    private val dao: BrowsingHistoryDao,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    val historyEnabledFlow: Flow<Boolean> =
        requireUserPreferenceFlow.map { it.historySettings.enabled }.distinctUntilChanged()

    suspend fun recordIllust(illust: Illust) {
        recordIllusts(listOf(illust))
    }

    suspend fun recordIllusts(illusts: List<Illust>) {
        val settings = requireUserPreferenceValue.historySettings.normalized()
        if (!settings.enabled || illusts.isEmpty()) return

        val uniqueIllusts = illusts.distinctBy { it.id }
        val userInfo = requireUserInfoValue
        val userId = userInfo.user.id
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
        pruneIfNeeded(userId, settings)

        if (userInfo.profile.isPremium) {
            runCatching {
                PixivRepository.addIllustBrowsingHistory(uniqueIllusts.map { it.id })
            }
        }
    }

    suspend fun recordNovel(novel: Novel) {
        val settings = requireUserPreferenceValue.historySettings.normalized()
        if (!settings.enabled) return

        val userInfo = requireUserInfoValue
        val userId = userInfo.user.id
        dao.upsertNovel(
            NovelHistoryEntity(
                novelId = novel.id,
                userId = userId,
                viewedAtMillis = currentTimeMillis(),
                novelJson = json.encodeToString(novel),
            )
        )
        pruneIfNeeded(userId, settings)

        if (userInfo.profile.isPremium) {
            runCatching {
                PixivRepository.addNovelBrowsingHistory(novel.id)
            }
        }
    }

    suspend fun getLocalIllusts(limit: Int, offset: Int): List<Illust> {
        val userId = requireUserInfoValue.user.id
        return dao.getIllusts(userId = userId, limit = limit, offset = offset)
            .mapNotNull { runCatching { json.decodeFromString<Illust>(it.illustJson) }.getOrNull() }
    }

    suspend fun getLocalNovels(limit: Int, offset: Int): List<Novel> {
        val userId = requireUserInfoValue.user.id
        return dao.getNovels(userId = userId, limit = limit, offset = offset)
            .mapNotNull { runCatching { json.decodeFromString<Novel>(it.novelJson) }.getOrNull() }
    }

    fun observeLocalIllustCount(): Flow<Int> =
        requireUserInfoFlow.flatMapLatest { dao.observeIllustCount(it.user.id) }.distinctUntilChanged()

    fun observeLocalNovelCount(): Flow<Int> =
        requireUserInfoFlow.flatMapLatest { dao.observeNovelCount(it.user.id) }.distinctUntilChanged()

    suspend fun clearAllLocalHistory() {
        val userId = requireUserInfoValue.user.id
        dao.clearIllusts(userId)
        dao.clearNovels(userId)
    }

    private suspend fun pruneIfNeeded(userId: Long, settings: HistorySettings) {
        if (!settings.autoClean || settings.unlimited) return
        dao.pruneIllusts(userId = userId, maxEntries = settings.maxEntries)
        dao.pruneNovels(userId = userId, maxEntries = settings.maxEntries)
    }
}
