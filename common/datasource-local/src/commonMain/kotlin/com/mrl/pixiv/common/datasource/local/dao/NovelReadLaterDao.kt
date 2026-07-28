package com.mrl.pixiv.common.datasource.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrl.pixiv.common.datasource.local.entity.NovelReadLaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelReadLaterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NovelReadLaterEntity)

    @Query(
        """
        SELECT * FROM novel_read_later
        WHERE userId = :userId
        ORDER BY addedAtMillis DESC
        """
    )
    fun observeByUserId(userId: Long): Flow<List<NovelReadLaterEntity>>

    @Query(
        """
        SELECT * FROM novel_read_later
        WHERE userId = :userId AND novelId = :novelId AND targetLanguage = :targetLanguage
        LIMIT 1
        """
    )
    fun observeByKey(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
    ): Flow<NovelReadLaterEntity?>

    @Query(
        """
        SELECT * FROM novel_read_later
        WHERE userId = :userId AND novelId = :novelId AND targetLanguage = :targetLanguage
        LIMIT 1
        """
    )
    suspend fun getByKey(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
    ): NovelReadLaterEntity?

    @Query(
        """
        SELECT * FROM novel_read_later
        WHERE userId = :userId AND state = 'PENDING'
        ORDER BY addedAtMillis ASC
        """
    )
    suspend fun getPending(userId: Long): List<NovelReadLaterEntity>

    @Query(
        """
        SELECT COUNT(*) FROM novel_read_later
        WHERE userId = :userId AND state = 'PENDING'
        """
    )
    fun observePendingCount(userId: Long): Flow<Int>

    @Query(
        """
        UPDATE novel_read_later
        SET state = 'RUNNING',
            attemptToken = :attemptToken,
            updatedAtMillis = :updatedAtMillis
        WHERE userId = :userId
          AND novelId = :novelId
          AND targetLanguage = :targetLanguage
          AND state = 'PENDING'
        """
    )
    suspend fun claimPending(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
        attemptToken: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        UPDATE novel_read_later
        SET state = :state,
            retryCount = :retryCount,
            lastError = :lastError,
            sourceMd5 = :sourceMd5,
            attemptToken = '',
            updatedAtMillis = :updatedAtMillis
        WHERE userId = :userId
          AND novelId = :novelId
          AND targetLanguage = :targetLanguage
          AND state = 'RUNNING'
          AND attemptToken = :attemptToken
        """
    )
    suspend fun updateResult(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
        attemptToken: String,
        state: String,
        retryCount: Int,
        lastError: String?,
        sourceMd5: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        UPDATE novel_read_later
        SET state = 'PENDING',
            retryCount = 0,
            lastError = NULL,
            provider = :provider,
            model = :model,
            endpoint = :endpoint,
            responseApi = :responseApi,
            extraBody = :extraBody,
            configFingerprint = :configFingerprint,
            sourceMd5 = '',
            attemptToken = '',
            updatedAtMillis = :updatedAtMillis
        WHERE userId = :userId
          AND novelId = :novelId
          AND targetLanguage = :targetLanguage
          AND state = 'FAILED'
        """
    )
    suspend fun retry(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
        provider: String,
        model: String,
        endpoint: String,
        responseApi: Boolean,
        extraBody: String,
        configFingerprint: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        UPDATE novel_read_later
        SET state = 'PENDING',
            attemptToken = '',
            updatedAtMillis = :updatedAtMillis
        WHERE state = 'RUNNING'
        """
    )
    suspend fun restoreInterrupted(updatedAtMillis: Long)

    @Query(
        """
        UPDATE novel_read_later
        SET state = 'PENDING',
            attemptToken = '',
            updatedAtMillis = :updatedAtMillis
        WHERE userId = :userId
          AND novelId = :novelId
          AND targetLanguage = :targetLanguage
          AND state = 'RUNNING'
          AND attemptToken = :attemptToken
        """
    )
    suspend fun restoreRunningAttempt(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
        attemptToken: String,
        updatedAtMillis: Long,
    )

    @Query(
        """
        UPDATE novel_read_later
        SET state = 'FAILED',
            attemptToken = '',
            lastError = :lastError,
            updatedAtMillis = :updatedAtMillis
        WHERE userId = :userId
          AND novelId = :novelId
          AND targetLanguage = :targetLanguage
          AND state = 'READY'
        """
    )
    suspend fun invalidateReady(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
        lastError: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        DELETE FROM novel_read_later
        WHERE userId = :userId AND novelId = :novelId AND targetLanguage = :targetLanguage
        """
    )
    suspend fun deleteByKey(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
    )
}
