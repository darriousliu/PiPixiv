package com.mrl.pixiv.common.datasource.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Update
    suspend fun update(entity: DownloadEntity)

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
