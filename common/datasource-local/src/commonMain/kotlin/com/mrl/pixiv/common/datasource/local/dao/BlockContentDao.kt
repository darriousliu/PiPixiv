package com.mrl.pixiv.common.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrl.pixiv.common.datasource.local.entity.BlockCommentEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockNovelEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIllust(entity: BlockIllustEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIllusts(entities: List<BlockIllustEntity>)

    @Query("DELETE FROM block_illust WHERE illustId = :illustId")
    suspend fun deleteIllust(illustId: Long)

    @Query("DELETE FROM block_illust")
    suspend fun clearIllusts()

    @Query("SELECT * FROM block_illust")
    suspend fun getAllIllusts(): List<BlockIllustEntity>

    @Query("SELECT * FROM block_illust ORDER BY illustId DESC")
    fun observeIllusts(): Flow<List<BlockIllustEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNovel(entity: BlockNovelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNovels(entities: List<BlockNovelEntity>)

    @Query("DELETE FROM block_novel WHERE novelId = :novelId")
    suspend fun deleteNovel(novelId: Long)

    @Query("DELETE FROM block_novel")
    suspend fun clearNovels()

    @Query("SELECT * FROM block_novel")
    suspend fun getAllNovels(): List<BlockNovelEntity>

    @Query("SELECT * FROM block_novel ORDER BY novelId DESC")
    fun observeNovels(): Flow<List<BlockNovelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(entity: BlockUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(entities: List<BlockUserEntity>)

    @Query("DELETE FROM block_user WHERE userId = :userId")
    suspend fun deleteUser(userId: Long)

    @Query("DELETE FROM block_user WHERE userId IN (:userIds)")
    suspend fun deleteUsers(userIds: List<Long>)

    @Query("DELETE FROM block_user")
    suspend fun clearUsers()

    @Query("SELECT * FROM block_user")
    suspend fun getAllUsers(): List<BlockUserEntity>

    @Query("SELECT * FROM block_user ORDER BY userId DESC")
    fun observeUsers(): Flow<List<BlockUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(entity: BlockTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTags(entities: List<BlockTagEntity>)

    @Query("DELETE FROM block_tag WHERE tag = :tag")
    suspend fun deleteTag(tag: String)

    @Query("DELETE FROM block_tag")
    suspend fun clearTags()

    @Query("SELECT * FROM block_tag")
    suspend fun getAllTags(): List<BlockTagEntity>

    @Query("SELECT * FROM block_tag ORDER BY tag COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<BlockTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComment(entity: BlockCommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(entities: List<BlockCommentEntity>)

    @Query("DELETE FROM block_comment WHERE commentId = :commentId")
    suspend fun deleteComment(commentId: Long)

    @Query("DELETE FROM block_comment")
    suspend fun clearComments()

    @Query("SELECT * FROM block_comment")
    suspend fun getAllComments(): List<BlockCommentEntity>

    @Query("SELECT * FROM block_comment ORDER BY commentId DESC")
    fun observeComments(): Flow<List<BlockCommentEntity>>
}
