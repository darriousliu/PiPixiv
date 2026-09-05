package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.ai.AiMessageRole
import com.mrl.pixiv.common.ai.AiTextMessage
import com.mrl.pixiv.common.ai.AiTextRequest
import com.mrl.pixiv.common.ai.provider.AiTextProviderClient
import com.mrl.pixiv.common.ai.provider.AiTextStreamEvent
import com.mrl.pixiv.common.ai.provider.ClaudeTextClient
import com.mrl.pixiv.common.ai.provider.GeminiTextClient
import com.mrl.pixiv.common.ai.provider.OpenAiTextClient
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.annotation.Single

data class NovelMetadataTranslation(
    val title: String,
    val caption: String,
)

data class NovelTranslationStreamProgress(
    val text: String,
    val completedChunks: Int,
    val totalChunks: Int,
    val isComplete: Boolean,
)

class NovelAiTranslationService(
    private val openAiTextClient: OpenAiTextClient,
    private val claudeTextClient: ClaudeTextClient,
    private val geminiTextClient: GeminiTextClient,
    private val translationLimiter: NovelTranslationLimiter = NovelTranslationLimiter(),
) {
    suspend fun translate(
        text: String,
        targetLanguageTag: String,
        config: AiTranslationConfig,
    ): String {
        return translateStreaming(
            text = text,
            targetLanguageTag = targetLanguageTag,
            config = config,
        ).last().text
    }

    suspend fun translateMetadata(
        title: String,
        caption: String,
        targetLanguageTag: String,
        config: AiTranslationConfig,
    ): NovelMetadataTranslation {
        val request = buildMetadataRequest(
            title = title,
            caption = caption,
            targetLanguageTag = targetLanguageTag,
            config = config,
        )
        val translated = translationLimiter.withPermit(config.maxConcurrentRequests) {
            clientFor(config.provider).generateText(request).text
        }
        return parseNovelMetadataTranslation(translated)
    }

    fun translateStreaming(
        text: String,
        targetLanguageTag: String,
        config: AiTranslationConfig,
    ): Flow<NovelTranslationStreamProgress> = flow {
        val sourceText = text.trim()
        if (sourceText.isEmpty()) {
            emit(
                NovelTranslationStreamProgress(
                    text = sourceText,
                    completedChunks = 0,
                    totalChunks = 0,
                    isComplete = true,
                )
            )
            return@flow
        }

        val modelName = config.model.trim()
        val chunkPlan = splitChunks(sourceText)
        val chunks = chunkPlan.chunks
        val client = clientFor(config.provider)

        coroutineScope {
            val requests = chunks.mapIndexed { index, chunk ->
                buildRequest(
                    config = config,
                    model = modelName,
                    targetLanguageTag = targetLanguageTag,
                    chunk = chunk,
                    chunkIndex = index + 1,
                    totalChunks = chunks.size,
                    maxParagraphCount = chunkPlan.maxParagraphCount,
                )
            }
            val remainingTranslations = requests.drop(1).map { request ->
                async(start = CoroutineStart.LAZY) {
                    generateCompleteText(
                        client = client,
                        request = request,
                        maxConcurrentRequests = config.maxConcurrentRequests,
                    )
                }
            }
            combineTranslatedChunks(
                firstChunk = generateTextStream(
                    client = client,
                    request = requests.first(),
                    maxConcurrentRequests = config.maxConcurrentRequests,
                    onPermitAcquired = {
                        remainingTranslations.forEach { it.start() }
                    },
                ),
                remainingChunks = remainingTranslations,
                totalChunks = chunks.size,
            ).collect { emit(it) }
        }
    }

    private fun clientFor(provider: AiProvider): AiTextProviderClient = when (provider) {
        AiProvider.OPENAI -> openAiTextClient
        AiProvider.CLAUDE -> claudeTextClient
        AiProvider.GEMINI -> geminiTextClient
    }

    private fun buildMetadataRequest(
        title: String,
        caption: String,
        targetLanguageTag: String,
        config: AiTranslationConfig,
    ): AiTextRequest {
        return AiTextRequest(
            provider = config.provider,
            endpoint = config.endpoint.trim(),
            apiKey = config.apiKey.trim(),
            model = config.model.trim(),
            messages = listOf(
                AiTextMessage(
                    role = AiMessageRole.USER,
                    content = buildNovelMetadataTranslationPrompt(
                        title = title,
                        caption = caption,
                        targetLanguage = toDisplayLanguage(targetLanguageTag),
                    ),
                )
            ),
            responseApi = config.responseApi,
            extraBody = config.extraBody,
            generationTimeoutMillis =
                config.generationTimeoutSeconds.toLong() * MILLIS_PER_SECOND,
        )
    }

    private fun buildRequest(
        config: AiTranslationConfig,
        model: String,
        targetLanguageTag: String,
        chunk: String,
        chunkIndex: Int,
        totalChunks: Int,
        maxParagraphCount: Int,
    ): AiTextRequest {
        val prompt = buildPrompt(
            chunk = chunk,
            targetLanguageTag = targetLanguageTag,
            chunkIndex = chunkIndex,
            totalChunks = totalChunks,
            maxParagraphCount = maxParagraphCount,
        )

        return AiTextRequest(
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
            extraBody = config.extraBody,
            generationTimeoutMillis = config.generationTimeoutSeconds.toLong() * MILLIS_PER_SECOND,
        )
    }

    private suspend fun generateCompleteText(
        client: AiTextProviderClient,
        request: AiTextRequest,
        maxConcurrentRequests: Int,
    ): String {
        return translationLimiter.withPermit(maxConcurrentRequests) {
            val translated = client.generateText(request).text

            require(translated.isNotBlank()) {
                "AI returned empty translation."
            }

            translated
        }
    }

    private fun generateTextStream(
        client: AiTextProviderClient,
        request: AiTextRequest,
        maxConcurrentRequests: Int,
        onPermitAcquired: () -> Unit,
    ): Flow<AiTextStreamEvent> = flow {
        translationLimiter.withPermit(maxConcurrentRequests) {
            onPermitAcquired()
            client.generateTextStream(request).collect { event ->
                emit(event)
            }
        }
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
            当前分片策略：每批最多 $maxParagraphCount 个段落。

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
        private const val MILLIS_PER_SECOND = 1_000L
    }
}

@Single
fun provideNovelAiTranslationService(
    openAiTextClient: OpenAiTextClient,
    claudeTextClient: ClaudeTextClient,
    geminiTextClient: GeminiTextClient,
    translationLimiter: NovelTranslationLimiter,
): NovelAiTranslationService {
    return NovelAiTranslationService(
        openAiTextClient = openAiTextClient,
        claudeTextClient = claudeTextClient,
        geminiTextClient = geminiTextClient,
        translationLimiter = translationLimiter,
    )
}

internal fun buildNovelMetadataTranslationPrompt(
    title: String,
    caption: String,
    targetLanguage: String,
): String {
    val input = buildJsonObject {
        put("title", title)
        put("caption", caption)
    }
    return """
        你是一名专业的文学翻译。请将下面同一部小说的标题和简介翻译为 $targetLanguage。

        约束：
        1. 只返回一个 JSON 对象，不要使用 Markdown 代码块或添加任何解释。
        2. JSON 必须且只能包含 "title" 和 "caption" 两个字符串字段。
        3. "title" 必须为非空译文；原简介为空时 "caption" 可以为空字符串。
        4. 必须完整保留简介中的 HTML 标签、属性和结构。
        5. 必须原样保留所有 URL、数字和特殊符号。

        输入 JSON：
        $input
    """.trimIndent()
}

internal fun parseNovelMetadataTranslation(text: String): NovelMetadataTranslation {
    val payload = Json.parseToJsonElement(text.trim()) as? JsonObject
        ?: throw IllegalArgumentException("AI metadata translation must be a JSON object.")
    require(payload.keys == METADATA_TRANSLATION_KEYS) {
        "AI metadata translation must contain only title and caption."
    }

    val title = payload.requiredString("title")
    val caption = payload.requiredString("caption")
    require(title.isNotBlank()) {
        "AI metadata translation title must not be blank."
    }
    return NovelMetadataTranslation(
        title = title.trim(),
        caption = caption,
    )
}

private fun JsonObject.requiredString(key: String): String {
    val value = this[key] as? JsonPrimitive
    require(value?.isString == true) {
        "AI metadata translation field $key must be a string."
    }
    return value.content
}

private val METADATA_TRANSLATION_KEYS = setOf("title", "caption")

private data class ChunkPlan(
    val chunks: List<String>,
    val maxParagraphCount: Int,
)

internal fun combineTranslatedChunks(
    firstChunk: Flow<AiTextStreamEvent>,
    remainingChunks: List<Deferred<String>>,
    totalChunks: Int,
): Flow<NovelTranslationStreamProgress> = flow {
    val translated = StringBuilder()
    var firstCompleted = false
    var completedChunks = 0

    try {
        firstChunk.collect { event ->
            when (event) {
                is AiTextStreamEvent.Delta -> {
                    if (!firstCompleted && event.text.isNotEmpty()) {
                        translated.append(event.text)
                        emit(
                            NovelTranslationStreamProgress(
                                text = translated.toString(),
                                completedChunks = completedChunks,
                                totalChunks = totalChunks,
                                isComplete = false,
                            )
                        )
                    }
                }

                is AiTextStreamEvent.Completed -> {
                    check(!firstCompleted) {
                        "AI emitted more than one completion event."
                    }
                    require(event.text.isNotBlank()) {
                        "AI returned empty translation."
                    }
                    translated.clear()
                    translated.append(event.text)
                    firstCompleted = true
                    completedChunks = 1
                    emit(
                        NovelTranslationStreamProgress(
                            text = translated.toString(),
                            completedChunks = completedChunks,
                            totalChunks = totalChunks,
                            isComplete = totalChunks == 1,
                        )
                    )
                }
            }
        }

        check(firstCompleted) {
            "AI stream ended before a completion event."
        }

        remainingChunks.forEach { deferred ->
            val next = deferred.await()
            require(next.isNotBlank()) {
                "AI returned empty translation."
            }
            if (translated.isNotEmpty()) translated.append('\n')
            translated.append(next)
            completedChunks += 1
            emit(
                NovelTranslationStreamProgress(
                    text = translated.toString(),
                    completedChunks = completedChunks,
                    totalChunks = totalChunks,
                    isComplete = completedChunks == totalChunks,
                )
            )
        }
    } finally {
        remainingChunks.forEach { deferred ->
            if (!deferred.isCompleted) deferred.cancel()
        }
    }
}
