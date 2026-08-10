package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.ai.provider.AiTextStreamEvent
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.datasource.local.dao.NovelTranslationDao
import com.mrl.pixiv.common.datasource.local.entity.NovelTranslationEntity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NovelTranslationStreamingTest {
    @Test
    fun appendsConcurrentChunksOnlyInSourceOrder() = runTest {
        val second = async {
            delay(50)
            "B"
        }
        val third = async {
            delay(1)
            "C"
        }

        val progress = combineTranslatedChunks(
            firstChunk = flowOf(
                AiTextStreamEvent.Delta("A"),
                AiTextStreamEvent.Completed("A"),
            ),
            remainingChunks = listOf(second, third),
            totalChunks = 3,
        ).toList()

        assertEquals("A\nB\nC", progress.last().text)
        assertTrue(progress.last().isComplete)
        assertEquals(
            listOf("A", "A\nB", "A\nB\nC"),
            progress.map { it.text }.distinct(),
        )
    }

    @Test
    fun cancellationCancelsPendingChunkRequests() = runTest {
        var firstStreamCancelled = false
        val second = backgroundScope.async<String> {
            awaitCancellation()
        }
        val third = backgroundScope.async<String> {
            awaitCancellation()
        }

        combineTranslatedChunks(
            firstChunk = flow {
                try {
                    emit(AiTextStreamEvent.Delta("A"))
                    awaitCancellation()
                } finally {
                    firstStreamCancelled = true
                }
            },
            remainingChunks = listOf(second, third),
            totalChunks = 3,
        ).take(1).toList()

        assertTrue(firstStreamCancelled)
        assertTrue(second.isCancelled)
        assertTrue(third.isCancelled)
    }

    @Test
    fun incompleteFirstStreamFailsAndCancelsPendingChunks() = runTest {
        val pending = backgroundScope.async<String>(start = CoroutineStart.LAZY) {
            awaitCancellation()
        }

        assertFailsWith<IllegalStateException> {
            combineTranslatedChunks(
                firstChunk = flowOf(AiTextStreamEvent.Delta("partial")),
                remainingChunks = listOf(pending),
                totalChunks = 2,
            ).toList()
        }
        assertTrue(pending.isCancelled)
    }

    @Test
    fun metadataPromptCombinesTitleAndCaptionInOneJsonInput() {
        val title = "Source \"title\""
        val caption = "<a href=\"https://example.com/work?id=1\">Source caption</a>"

        val prompt = buildNovelMetadataTranslationPrompt(
            title = title,
            caption = caption,
            targetLanguage = "简体中文",
        )
        val input = Json.parseToJsonElement(
            prompt.substringAfter("输入 JSON：\n").trim(),
        ).jsonObject

        assertEquals(setOf("title", "caption"), input.keys)
        assertEquals(title, input.getValue("title").jsonPrimitive.content)
        assertEquals(caption, input.getValue("caption").jsonPrimitive.content)
        assertTrue(prompt.contains("完整保留简介中的 HTML 标签、属性和结构"))
        assertTrue(prompt.contains("原样保留所有 URL"))
    }

    @Test
    fun metadataParserAcceptsOnlyTitleAndCaptionStrings() {
        val translated = parseNovelMetadataTranslation(
            """{"title":"译文标题","caption":"<a href=\"https://example.com\">译文简介</a>"}""",
        )

        assertEquals("译文标题", translated.title)
        assertEquals(
            "<a href=\"https://example.com\">译文简介</a>",
            translated.caption,
        )
        assertEquals(
            "",
            parseNovelMetadataTranslation("""{"title":"译文标题","caption":""}""").caption,
        )
    }

    @Test
    fun metadataParserRejectsBlankTitleAndNonContractJson() {
        listOf(
            """{"title":"","caption":"简介"}""",
            """{"title":"标题"}""",
            """{"title":"标题","caption":"简介","tags":[]}""",
            """{"title":"标题","caption":null}""",
            """["标题","简介"]""",
            """```json
                {"title":"标题","caption":"简介"}
            ```""".trimIndent(),
        ).forEach { response ->
            assertFails {
                parseNovelMetadataTranslation(response)
            }
        }
    }

    @Test
    fun bodyAndMetadataCacheWritesPreserveEachOtherForTheSameConfiguration() = runTest {
        val repository = NovelTranslationRepository(FakeNovelTranslationDao())

        awaitAll(
            async {
                repository.saveTranslationForUser(
                    userId = 1L,
                    novelId = 2L,
                    targetLanguage = "en",
                    provider = AiProvider.OPENAI,
                    model = "model",
                    configFingerprint = "config",
                    sourceMd5 = "body-hash",
                    translatedText = "translated body",
                )
            },
            async {
                repository.saveMetadataTranslationForUser(
                    userId = 1L,
                    novelId = 2L,
                    targetLanguage = "en",
                    provider = AiProvider.OPENAI,
                    model = "model",
                    configFingerprint = "config",
                    metadataSourceMd5 = "metadata-hash",
                    translatedTitle = "translated title",
                    translatedCaption = "translated caption",
                )
            },
        )

        val cached = repository.getTranslationForUser(
            userId = 1L,
            novelId = 2L,
            targetLanguage = "en",
        )
        assertEquals("body-hash", cached?.sourceMd5)
        assertEquals("translated body", cached?.translatedText)
        assertEquals("metadata-hash", cached?.metadataSourceMd5)
        assertEquals("translated title", cached?.translatedTitle)
        assertEquals("translated caption", cached?.translatedCaption)
    }

    @Test
    fun configurationChangeClearsTheIncompatibleCachedFragment() = runTest {
        val repository = NovelTranslationRepository(FakeNovelTranslationDao())
        repository.saveMetadataTranslationForUser(
            userId = 1L,
            novelId = 2L,
            targetLanguage = "en",
            provider = AiProvider.OPENAI,
            model = "old-model",
            configFingerprint = "old-config",
            metadataSourceMd5 = "metadata-hash",
            translatedTitle = "old title",
            translatedCaption = "old caption",
        )

        repository.saveTranslationForUser(
            userId = 1L,
            novelId = 2L,
            targetLanguage = "en",
            provider = AiProvider.CLAUDE,
            model = "new-model",
            configFingerprint = "new-config",
            sourceMd5 = "body-hash",
            translatedText = "new body",
        )

        val cached = repository.getTranslationForUser(
            userId = 1L,
            novelId = 2L,
            targetLanguage = "en",
        )
        assertEquals("new body", cached?.translatedText)
        assertEquals("", cached?.metadataSourceMd5)
        assertEquals("", cached?.translatedTitle)
        assertEquals("", cached?.translatedCaption)

        repository.saveMetadataTranslationForUser(
            userId = 1L,
            novelId = 2L,
            targetLanguage = "en",
            provider = AiProvider.GEMINI,
            model = "latest-model",
            configFingerprint = "latest-config",
            metadataSourceMd5 = "latest-metadata-hash",
            translatedTitle = "latest title",
            translatedCaption = "latest caption",
        )

        val metadataOnly = repository.getTranslationForUser(
            userId = 1L,
            novelId = 2L,
            targetLanguage = "en",
        )
        assertEquals("", metadataOnly?.sourceMd5)
        assertEquals("", metadataOnly?.translatedText)
        assertEquals("latest-metadata-hash", metadataOnly?.metadataSourceMd5)
        assertEquals("latest title", metadataOnly?.translatedTitle)
        assertEquals("latest caption", metadataOnly?.translatedCaption)
    }

    private class FakeNovelTranslationDao : NovelTranslationDao {
        private var entity: NovelTranslationEntity? = null

        override suspend fun upsert(entity: NovelTranslationEntity) {
            this.entity = entity
        }

        override suspend fun getByNovelIdAndLanguage(
            userId: Long,
            novelId: Long,
            targetLanguage: String,
        ): NovelTranslationEntity? = entity?.takeIf {
            it.userId == userId &&
                    it.novelId == novelId &&
                    it.targetLanguage == targetLanguage
        }

        override suspend fun deleteByNovelIdAndLanguage(
            userId: Long,
            novelId: Long,
            targetLanguage: String,
        ) {
            if (getByNovelIdAndLanguage(userId, novelId, targetLanguage) != null) {
                entity = null
            }
        }
    }
}
