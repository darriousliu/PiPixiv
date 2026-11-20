package com.mrl.pixiv.common.datasource.local

import com.mrl.pixiv.common.datasource.local.dao.BlockContentDao
import com.mrl.pixiv.common.datasource.local.dao.DownloadDao
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import com.mrl.pixiv.common.datasource.local.entity.BlockCommentEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockNovelEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

// Resolve the Room DAOs only when startup has selected and opened the final database.
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

internal class StartupBlockContentDao(private val provider: GlobalDatabaseProvider) : BlockContentDao {
    private suspend fun dao() = provider.awaitOpen().blockContentDao()

    override suspend fun insertMissingIllusts(entities: List<BlockIllustEntity>) = dao().insertMissingIllusts(entities)

    override suspend fun insertMissingNovels(entities: List<BlockNovelEntity>) = dao().insertMissingNovels(entities)

    override suspend fun insertMissingUsers(entities: List<BlockUserEntity>) = dao().insertMissingUsers(entities)

    override suspend fun insertMissingTags(entities: List<BlockTagEntity>) = dao().insertMissingTags(entities)

    override suspend fun insertMissingComments(entities: List<BlockCommentEntity>) = dao().insertMissingComments(entities)

    override suspend fun insertMissing(
        illusts: List<BlockIllustEntity>,
        novels: List<BlockNovelEntity>,
        users: List<BlockUserEntity>,
        tags: List<BlockTagEntity>,
        comments: List<BlockCommentEntity>,
    ) = dao().insertMissing(illusts, novels, users, tags, comments)

    override suspend fun upsertIllust(entity: BlockIllustEntity) = dao().upsertIllust(entity)

    override suspend fun upsertIllusts(entities: List<BlockIllustEntity>) = dao().upsertIllusts(entities)

    override suspend fun deleteIllust(illustId: Long) = dao().deleteIllust(illustId)

    override suspend fun clearIllusts() = dao().clearIllusts()

    override suspend fun getAllIllusts() = dao().getAllIllusts()

    override fun observeIllusts() = flow { emitAll(dao().observeIllusts()) }

    override suspend fun upsertNovel(entity: BlockNovelEntity) = dao().upsertNovel(entity)

    override suspend fun upsertNovels(entities: List<BlockNovelEntity>) = dao().upsertNovels(entities)

    override suspend fun deleteNovel(novelId: Long) = dao().deleteNovel(novelId)

    override suspend fun clearNovels() = dao().clearNovels()

    override suspend fun getAllNovels() = dao().getAllNovels()

    override fun observeNovels() = flow { emitAll(dao().observeNovels()) }

    override suspend fun upsertUser(entity: BlockUserEntity) = dao().upsertUser(entity)

    override suspend fun upsertUsers(entities: List<BlockUserEntity>) = dao().upsertUsers(entities)

    override suspend fun deleteUser(userId: Long) = dao().deleteUser(userId)

    override suspend fun deleteUsers(userIds: List<Long>) = dao().deleteUsers(userIds)

    override suspend fun clearUsers() = dao().clearUsers()

    override suspend fun getAllUsers() = dao().getAllUsers()

    override fun observeUsers() = flow { emitAll(dao().observeUsers()) }

    override suspend fun upsertTag(entity: BlockTagEntity) = dao().upsertTag(entity)

    override suspend fun upsertTags(entities: List<BlockTagEntity>) = dao().upsertTags(entities)

    override suspend fun deleteTag(tag: String) = dao().deleteTag(tag)

    override suspend fun clearTags() = dao().clearTags()

    override suspend fun getAllTags() = dao().getAllTags()

    override fun observeTags() = flow { emitAll(dao().observeTags()) }

    override suspend fun upsertComment(entity: BlockCommentEntity) = dao().upsertComment(entity)

    override suspend fun upsertComments(entities: List<BlockCommentEntity>) = dao().upsertComments(entities)

    override suspend fun deleteComment(commentId: Long) = dao().deleteComment(commentId)

    override suspend fun clearComments() = dao().clearComments()

    override suspend fun getAllComments() = dao().getAllComments()

    override fun observeComments() = flow { emitAll(dao().observeComments()) }
}
