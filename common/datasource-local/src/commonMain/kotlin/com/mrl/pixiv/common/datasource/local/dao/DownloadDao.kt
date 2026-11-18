package com.mrl.pixiv.common.datasource.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DownloadEntity>)

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("UPDATE download SET progress = :progress WHERE illustId = :illustId AND `index` = :index")
    suspend fun updateProgress(illustId: Long, index: Int, progress: Float)

    @Query(
        """
        UPDATE download SET status = :status, progress = COALESCE(:progress, progress),
        filePath = COALESCE(:filePath, filePath), fileUri = COALESCE(:fileUri, fileUri)
        WHERE illustId = :illustId AND `index` = :index AND createTime = :createTime
        AND status IN (0, 1)
        """
    )
    suspend fun updateActiveAttempt(
        illustId: Long,
        index: Int,
        createTime: Long,
        status: Int,
        progress: Float? = null,
        filePath: String? = null,
        fileUri: String? = null,
    ): Int

    @Delete
    suspend fun delete(entity: DownloadEntity)

    @Query("DELETE FROM download")
    suspend fun deleteAll()

    @Query("SELECT * FROM download ORDER BY createTime DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download WHERE status = :status ORDER BY createTime DESC")
    fun getDownloadsByStatus(status: Int): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download WHERE illustId = :illustId AND `index` = :index")
    suspend fun getDownload(illustId: Long, index: Int): DownloadEntity?
}
