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
    val configFingerprint: String,
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
        return getTranslationForUser(
            userId = userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
        )
    }

    suspend fun getTranslationForUser(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
    ): NovelTranslationCache? {
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
        configFingerprint: String,
        sourceMd5: String,
        translatedText: String,
    ) {
        val userId = requireUserInfoValue.user.id
        saveTranslationForUser(
            userId = userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
            provider = provider,
            model = model,
            configFingerprint = configFingerprint,
            sourceMd5 = sourceMd5,
            translatedText = translatedText,
        )
    }

    suspend fun saveTranslationForUser(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
        provider: AiProvider,
        model: String,
        configFingerprint: String,
        sourceMd5: String,
        translatedText: String,
    ) {
        dao.upsert(
            NovelTranslationEntity(
                novelId = novelId,
                userId = userId,
                targetLanguage = targetLanguage,
                provider = provider.name,
                model = model,
                configFingerprint = configFingerprint,
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
        deleteTranslationForUser(
            userId = userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
        )
    }

    suspend fun deleteTranslationForUser(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
    ) {
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
        configFingerprint = configFingerprint,
        sourceMd5 = sourceMd5,
        translatedText = translatedText,
    )
}
