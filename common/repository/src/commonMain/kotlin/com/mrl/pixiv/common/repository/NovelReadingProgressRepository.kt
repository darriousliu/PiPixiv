package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.datasource.local.dao.NovelReadingProgressDao
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
    private val dao: NovelReadingProgressDao
) {
    suspend fun getProgress(novelId: Long): NovelReadingProgress? {
        val userId = requireUserInfoValue.user.id
        return dao.getByNovelId(userId = userId, novelId = novelId)?.toDomain()
    }

    suspend fun saveProgress(
        novelId: Long,
        progress: NovelReadingProgress
    ) {
        val userId = requireUserInfoValue.user.id
        dao.upsert(
            NovelReadingProgressEntity(
                novelId = novelId,
                userId = userId,
                paragraphIndex = progress.paragraphIndex,
                charIndex = progress.charIndex,
                paragraphHash = progress.paragraphHash,
                updatedAtMillis = currentTimeMillis()
            )
        )
    }
}

private fun NovelReadingProgressEntity.toDomain(): NovelReadingProgress {
    return NovelReadingProgress(
        paragraphIndex = paragraphIndex,
        charIndex = charIndex,
        paragraphHash = paragraphHash
    )
}
