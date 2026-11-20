package com.mrl.pixiv.common.datasource.local

import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

// The download worker and read-later queue can be constructed before the global database opens.
// Resolve their Room DAOs only when startup has selected and opened the final database.
internal class StartupDownloadDao(private val provider: GlobalDatabaseProvider) : DownloadDao {
    private suspend fun dao() = provider.awaitOpen().downloadDao()

    override suspend fun insert(entity: DownloadEntity) = dao().insert(entity)

    override suspend fun insertAll(entities: List<DownloadEntity>) = dao().insertAll(entities)

    override suspend fun update(entity: DownloadEntity) = dao().update(entity)

    override suspend fun updateProgress(illustId: Long, index: Int, progress: Float) =
        dao().updateProgress(illustId, index, progress)

    override suspend fun updateActiveAttempt(
        illustId: Long,
        index: Int,
        createTime: Long,
        status: Int,
        progress: Float?,
        filePath: String?,
        fileUri: String?,
    ): Int = dao().updateActiveAttempt(illustId, index, createTime, status, progress, filePath, fileUri)

    override suspend fun delete(entity: DownloadEntity) = dao().delete(entity)

    override suspend fun deleteAll() = dao().deleteAll()

    override fun getAllDownloads() = flow { emitAll(dao().getAllDownloads()) }

    override fun getDownloadsByStatus(status: Int) = flow { emitAll(dao().getDownloadsByStatus(status)) }

    override suspend fun getDownload(illustId: Long, index: Int): DownloadEntity? =
        dao().getDownload(illustId, index)
}

internal class StartupNovelTranslationDao(private val provider: GlobalDatabaseProvider) : NovelTranslationDao {
    private suspend fun dao() = provider.awaitOpen().novelTranslationDao()

    override suspend fun upsert(entity: NovelTranslationEntity) = dao().upsert(entity)

    override suspend fun getByNovelIdAndLanguage(novelId: Long, targetLanguage: String): NovelTranslationEntity? =
        dao().getByNovelIdAndLanguage(novelId, targetLanguage)

    override suspend fun deleteByNovelIdAndLanguage(novelId: Long, targetLanguage: String) =
        dao().deleteByNovelIdAndLanguage(novelId, targetLanguage)
}
