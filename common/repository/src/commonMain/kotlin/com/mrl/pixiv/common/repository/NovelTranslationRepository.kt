package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity
import com.mrl.pixiv.common.util.currentTimeMillis
import org.koin.core.annotation.Single

data class NovelTranslationCache(
    val targetLanguage: String,
    val provider: AiProvider,
    val model: String,
    val sourceMd5: String,
    val translatedText: String,
)

@Single
class NovelTranslationRepository(
    private val dao: NovelTranslationDao
) {
    suspend fun getTranslation(
        novelId: Long,
        targetLanguage: String
    ): NovelTranslationCache? {
        val userId = requireUserInfoValue.user.id
        return dao.getByNovelIdAndLanguage(
            userId = userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
        )?.toDomain()
    }

    suspend fun saveTranslation(
        novelId: Long,
        targetLanguage: String,
        provider: AiProvider,
        model: String,
        sourceMd5: String,
        translatedText: String,
    ) {
        val userId = requireUserInfoValue.user.id
        dao.upsert(
            NovelTranslationEntity(
                novelId = novelId,
                userId = userId,
                targetLanguage = targetLanguage,
                provider = provider.name,
                model = model,
                sourceMd5 = sourceMd5,
                translatedText = translatedText,
                updatedAtMillis = currentTimeMillis(),
            )
        )
    }

    suspend fun deleteTranslation(
        novelId: Long,
        targetLanguage: String
    ) {
        val userId = requireUserInfoValue.user.id
        dao.deleteByNovelIdAndLanguage(
            userId = userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
        )
    }
}

private fun NovelTranslationEntity.toDomain(): NovelTranslationCache {
    return NovelTranslationCache(
        targetLanguage = targetLanguage,
        provider = runCatching { enumValueOf<AiProvider>(provider) }
            .getOrDefault(AiProvider.OPENAI),
        model = model,
        sourceMd5 = sourceMd5,
        translatedText = translatedText,
    )
}
