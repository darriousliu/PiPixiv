package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity
import com.mrl.pixiv.common.util.currentTimeMillis
import org.koin.core.annotation.Single

data class NovelReadingProgress(
    val paragraphIndex: Int,
    val charIndex: Int,
    val paragraphHash: Int,
)

@Single
class NovelReadingProgressRepository(
    private val databases: AccountDatabaseProvider,
) {
    suspend fun getProgress(novelId: Long): NovelReadingProgress? {
        val account = databases.current.value ?: return null
        return account.db.novelReadingProgressDao()
            .getByNovelId(userId = account.userId, novelId = novelId)?.toDomain()
    }

    suspend fun saveProgress(
        novelId: Long,
        progress: NovelReadingProgress
    ) {
        val account = databases.requireCurrent()
        account.db.novelReadingProgressDao().upsert(
            NovelReadingProgressEntity(
                novelId = novelId,
                userId = account.userId,
                paragraphIndex = progress.paragraphIndex,
                charIndex = progress.charIndex,
                paragraphHash = progress.paragraphHash,
                updatedAtMillis = currentTimeMillis()
            )
        )
    }

    suspend fun clearProgress(novelId: Long) {
        val account = databases.requireCurrent()
        account.db.novelReadingProgressDao().deleteByNovelId(userId = account.userId, novelId = novelId)
    }
}

private fun NovelReadingProgressEntity.toDomain(): NovelReadingProgress {
    return NovelReadingProgress(
        paragraphIndex = paragraphIndex,
        charIndex = charIndex,
        paragraphHash = paragraphHash
    )
}
