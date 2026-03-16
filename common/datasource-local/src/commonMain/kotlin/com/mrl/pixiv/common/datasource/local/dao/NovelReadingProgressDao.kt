package com.mrl.pixiv.common.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity

@Dao
interface NovelReadingProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NovelReadingProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NovelReadingProgressEntity>)

    @Query("SELECT * FROM novel_reading_progress WHERE userId = :userId AND novelId = :novelId LIMIT 1")
    suspend fun getByNovelId(userId: Long, novelId: Long): NovelReadingProgressEntity?

    @Query("SELECT * FROM novel_reading_progress WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): List<NovelReadingProgressEntity>
}
