package com.mrl.pixiv.common.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrl.pixiv.common.datasource.local.entity.IllustHistoryEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowsingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIllust(entity: IllustHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIllusts(entities: List<IllustHistoryEntity>)

    @Query(
        """
        SELECT * FROM browsing_history_illust
        WHERE userId = :userId
        ORDER BY viewedAtMillis DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getIllusts(userId: Long, limit: Int, offset: Int): List<IllustHistoryEntity>

    @Query("SELECT COUNT(*) FROM browsing_history_illust WHERE userId = :userId")
    suspend fun countIllusts(userId: Long): Int

    @Query("SELECT COUNT(*) FROM browsing_history_illust WHERE userId = :userId")
    fun observeIllustCount(userId: Long): Flow<Int>

    @Query("DELETE FROM browsing_history_illust WHERE userId = :userId")
    suspend fun clearIllusts(userId: Long)

    @Query(
        """
        DELETE FROM browsing_history_illust
        WHERE userId = :userId
        AND illustId NOT IN (
            SELECT illustId FROM browsing_history_illust
            WHERE userId = :userId
            ORDER BY viewedAtMillis DESC
            LIMIT :maxEntries
        )
        """
    )
    suspend fun pruneIllusts(userId: Long, maxEntries: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNovel(entity: NovelHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNovels(entities: List<NovelHistoryEntity>)

    @Query(
        """
        SELECT * FROM browsing_history_novel
        WHERE userId = :userId
        ORDER BY viewedAtMillis DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getNovels(userId: Long, limit: Int, offset: Int): List<NovelHistoryEntity>

    @Query("SELECT COUNT(*) FROM browsing_history_novel WHERE userId = :userId")
    suspend fun countNovels(userId: Long): Int

    @Query("SELECT COUNT(*) FROM browsing_history_novel WHERE userId = :userId")
    fun observeNovelCount(userId: Long): Flow<Int>

    @Query("DELETE FROM browsing_history_novel WHERE userId = :userId")
    suspend fun clearNovels(userId: Long)

    @Query(
        """
        DELETE FROM browsing_history_novel
        WHERE userId = :userId
        AND novelId NOT IN (
            SELECT novelId FROM browsing_history_novel
            WHERE userId = :userId
            ORDER BY viewedAtMillis DESC
            LIMIT :maxEntries
        )
        """
    )
    suspend fun pruneNovels(userId: Long, maxEntries: Int)
}
