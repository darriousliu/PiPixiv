package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.ai.AiMessageRole
import com.mrl.pixiv.common.ai.AiTextMessage
import com.mrl.pixiv.common.ai.AiTextRequest
import com.mrl.pixiv.common.ai.provider.ClaudeTextClient
import com.mrl.pixiv.common.ai.provider.GeminiTextClient
import com.mrl.pixiv.common.ai.provider.OpenAiTextClient
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single
class NovelAiTranslationService(
    private val openAiTextClient: OpenAiTextClient,
    private val claudeTextClient: ClaudeTextClient,
    private val geminiTextClient: GeminiTextClient,
) {
    suspend fun translate(
        text: String,
        targetLanguageTag: String,
        config: AiTranslationConfig,
    ): String {
        val sourceText = text.trim()
        if (sourceText.isEmpty()) return sourceText

        val modelName = config.model.trim()

        val chunkPlan = splitChunks(sourceText)
        val chunks = chunkPlan.chunks
        return coroutineScope {
            chunks.mapIndexed { index, chunk ->
                async {
                    index to translateChunk(
                        config = config,
                        model = modelName,
                        targetLanguageTag = targetLanguageTag,
                        chunk = chunk,
                        chunkIndex = index + 1,
                        totalChunks = chunks.size,
                        maxParagraphCount = chunkPlan.maxParagraphCount,
                    )
                }
            }
                .awaitAll()
                .sortedBy { it.first }
                .joinToString(separator = "\n") { it.second }
        }
    }

    private suspend fun translateChunk(
        config: AiTranslationConfig,
        model: String,
        targetLanguageTag: String,
        chunk: String,
        chunkIndex: Int,
        totalChunks: Int,
        maxParagraphCount: Int,
    ): String {
        val prompt = buildPrompt(
            chunk = chunk,
            targetLanguageTag = targetLanguageTag,
            chunkIndex = chunkIndex,
            totalChunks = totalChunks,
            maxParagraphCount = maxParagraphCount,
        )

        val translated = when (config.provider) {
            AiProvider.OPENAI -> openAiTextClient
            AiProvider.CLAUDE -> claudeTextClient
            AiProvider.GEMINI -> geminiTextClient
        }.generateText(
            AiTextRequest(
                provider = config.provider,
                endpoint = config.endpoint.trim(),
                apiKey = config.apiKey.trim(),
                model = model.trim(),
                messages = listOf(
                    AiTextMessage(
                        role = AiMessageRole.USER,
                        content = prompt,
                    )
                ),
                responseApi = config.responseApi,
            )
        ).text

        require(translated.isNotBlank()) {
            "AI returned empty translation."
        }

        return translated
    }

    private fun buildPrompt(
        chunk: String,
        targetLanguageTag: String,
        chunkIndex: Int,
        totalChunks: Int,
        maxParagraphCount: Int,
    ): String {
        return """
            你是一名专业的文学翻译。请将以下小说正文翻译为 ${toDisplayLanguage(targetLanguageTag)}。
            这是第 ${chunkIndex}/${totalChunks} 段内容。
            当前分片策略：每批最多 ${maxParagraphCount} 个段落。

            约束：
            1. 只返回翻译后的正文，不要添加任何解释、标题、注释或额外内容。
            2. 必须尽量保持原有段落与换行结构。
            3. 必须保留原文中的特殊标记、URL、数字和符号格式（例如 [newpage]、[chapter]、#、@、链接）。
            4. 人名和专有名词保持前后一致。
            5. 对对话、拟声词和语气词进行自然本地化，但不要改写剧情。

            原文：
            $chunk
        """.trimIndent()
    }

    private fun splitChunks(text: String): ChunkPlan {
        val paragraphs = text.split("\n")
        if (text.length <= MAX_CHARS_PER_CHUNK) {
            return ChunkPlan(
                chunks = listOf(text),
                maxParagraphCount = paragraphs.size.coerceAtLeast(1),
            )
        }

        val chunks = mutableListOf<String>()
        val currentParagraphs = mutableListOf<String>()
        var currentLength = 0
        var maxParagraphCount = 1

        fun flushCurrentChunk() {
            if (currentParagraphs.isEmpty()) return
            maxParagraphCount = maxParagraphCount.coerceAtLeast(currentParagraphs.size)
            chunks += currentParagraphs.joinToString(separator = "\n")
            currentParagraphs.clear()
            currentLength = 0
        }

        paragraphs.forEach { paragraph ->
            if (paragraph.length > MAX_CHARS_PER_CHUNK) {
                flushCurrentChunk()
                paragraph.chunked(MAX_CHARS_PER_CHUNK).forEach { piece ->
                    chunks += piece
                }
                return@forEach
            }

            val appendedLength = if (currentParagraphs.isEmpty()) {
                paragraph.length
            } else {
                paragraph.length + 1
            }

            if (currentParagraphs.isNotEmpty() && currentLength + appendedLength > MAX_CHARS_PER_CHUNK) {
                flushCurrentChunk()
            }

            currentParagraphs += paragraph
            currentLength += if (currentParagraphs.size == 1) {
                paragraph.length
            } else {
                paragraph.length + 1
            }
        }

        flushCurrentChunk()

        return ChunkPlan(
            chunks = chunks.filter { it.isNotEmpty() },
            maxParagraphCount = maxParagraphCount,
        )
    }

    private fun toDisplayLanguage(languageTag: String): String = when (languageTag) {
        "zh-CN" -> "简体中文"
        "zh-TW" -> "繁体中文"
        "en" -> "English"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "pt" -> "Português"
        "ru" -> "Русский"
        "ar" -> "العربية"
        "hi" -> "हिन्दी"
        else -> languageTag
    }

    companion object {
        private const val MAX_CHARS_PER_CHUNK = 3000
    }
}

private data class ChunkPlan(
    val chunks: List<String>,
    val maxParagraphCount: Int,
)
