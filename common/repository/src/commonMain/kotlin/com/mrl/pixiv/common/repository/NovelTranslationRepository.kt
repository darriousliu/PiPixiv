package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity
import com.mrl.pixiv.common.util.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

data class NovelTranslationCache(
    val targetLanguage: String,
    val provider: AiProvider,
    val model: String,
    val configFingerprint: String,
    val sourceMd5: String,
    val translatedText: String,
    val translatedTitle: String = "",
    val translatedCaption: String = "",
    val metadataSourceMd5: String = "",
)

@Single
class NovelTranslationRepository(
    private val dao: NovelTranslationDao
) {
    private val writeMutex = Mutex()

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
        writeMutex.withLock {
            val existing = dao.getByNovelIdAndLanguage(
                userId = userId,
                novelId = novelId,
                targetLanguage = targetLanguage,
            )
            val preserveMetadata = existing.matchesConfiguration(
                provider = provider.name,
                model = model,
                configFingerprint = configFingerprint,
            )
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
                    translatedTitle = existing?.translatedTitle
                        ?.takeIf { preserveMetadata }
                        .orEmpty(),
                    translatedCaption = existing?.translatedCaption
                        ?.takeIf { preserveMetadata }
                        .orEmpty(),
                    metadataSourceMd5 = existing?.metadataSourceMd5
                        ?.takeIf { preserveMetadata }
                        .orEmpty(),
                )
            )
        }
    }

    suspend fun saveMetadataTranslation(
        novelId: Long,
        targetLanguage: String,
        provider: AiProvider,
        model: String,
        configFingerprint: String,
        metadataSourceMd5: String,
        translatedTitle: String,
        translatedCaption: String,
    ) {
        val userId = requireUserInfoValue.user.id
        saveMetadataTranslationForUser(
            userId = userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
            provider = provider,
            model = model,
            configFingerprint = configFingerprint,
            metadataSourceMd5 = metadataSourceMd5,
            translatedTitle = translatedTitle,
            translatedCaption = translatedCaption,
        )
    }

    suspend fun saveMetadataTranslationForUser(
        userId: Long,
        novelId: Long,
        targetLanguage: String,
        provider: AiProvider,
        model: String,
        configFingerprint: String,
        metadataSourceMd5: String,
        translatedTitle: String,
        translatedCaption: String,
    ) {
        writeMutex.withLock {
            val existing = dao.getByNovelIdAndLanguage(
                userId = userId,
                novelId = novelId,
                targetLanguage = targetLanguage,
            )
            val preserveBody = existing.matchesConfiguration(
                provider = provider.name,
                model = model,
                configFingerprint = configFingerprint,
            )
            dao.upsert(
                NovelTranslationEntity(
                    novelId = novelId,
                    userId = userId,
                    targetLanguage = targetLanguage,
                    provider = provider.name,
                    model = model,
                    configFingerprint = configFingerprint,
                    sourceMd5 = existing?.sourceMd5
                        ?.takeIf { preserveBody }
                        .orEmpty(),
                    translatedText = existing?.translatedText
                        ?.takeIf { preserveBody }
                        .orEmpty(),
                    updatedAtMillis = currentTimeMillis(),
                    translatedTitle = translatedTitle,
                    translatedCaption = translatedCaption,
                    metadataSourceMd5 = metadataSourceMd5,
                )
            )
        }
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
        translatedTitle = translatedTitle,
        translatedCaption = translatedCaption,
        metadataSourceMd5 = metadataSourceMd5,
    )
}

private fun NovelTranslationEntity?.matchesConfiguration(
    provider: String,
    model: String,
    configFingerprint: String,
): Boolean = this != null &&
        this.provider == provider &&
        this.model == model &&
        this.configFingerprint == configFingerprint
