package com.mrl.pixiv.common.ai

import com.mrl.pixiv.common.data.setting.AiProvider

enum class AiMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
}

data class AiTextMessage(
    val role: AiMessageRole,
    val content: String,
)

data class AiTextRequest(
    val provider: AiProvider,
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val messages: List<AiTextMessage>,
    val maxOutputTokens: Int? = null,
    val responseApi: Boolean = false
)

data class AiTextResponse(
    val text: String,
)

data class AiImageRequest(
    val provider: AiProvider,
    val endpoint: String,
    val apiKey: String,
    val model: String,
    val prompt: String,
)

data class AiImageResponse(
    val imageUrls: List<String> = emptyList(),
    val imageBase64List: List<String> = emptyList(),
)

