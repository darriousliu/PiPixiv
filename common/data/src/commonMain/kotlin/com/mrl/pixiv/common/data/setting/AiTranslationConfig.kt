package com.mrl.pixiv.common.data.setting

import kotlinx.serialization.Serializable

@Serializable
enum class AiProvider {
    OPENAI,
    CLAUDE,
    GEMINI,
}

sealed interface Model {
    val modelId: String

    enum class OpenAI(override val modelId: String) : Model {
        GPT_5_5("gpt-5.5"),
        GPT_5_4("gpt-5.4"),
        GPT_5_4_MINI("gpt-5.4-mini"),
        GPT_5_4_NANO("gpt-5.4-nano"),
    }

    enum class Claude(override val modelId: String) : Model {
        CLAUDE_OPUS_4_8("claude-opus-4-8"),
        CLAUDE_4_6_SONNET("claude-sonnet-4-6"),
        CLAUDE_4_5_HAIKU("claude-haiku-4-5"),
    }

    enum class Gemini(override val modelId: String) : Model {
        GEMINI_3_5_FLASH("gemini-3.5-flash"),
        GEMINI_3_1_PRO_PREVIEW("gemini-3.1-pro-preview"),
        GEMINI_3_FLASH_PREVIEW("gemini-3-flash-preview"),
        GEMINI_3_1_FLASH_LITE("gemini-3.1-flash-lite"),
    }
}

@Serializable
data class AiTranslationConfig(
    val provider: AiProvider = AiProvider.OPENAI,
    val endpoint: String = defaultEndpoint(AiProvider.OPENAI),
    val apiKey: String = "",
    val model: String = defaultModel(AiProvider.OPENAI).modelId,
    val responseApi: Boolean = false,
    val extraBody: String = "",
    val generationTimeoutSeconds: Int = GENERATION_TIMEOUT_DEFAULT_SECONDS,
) {
    companion object {
        const val GENERATION_TIMEOUT_MIN_SECONDS = 30
        const val GENERATION_TIMEOUT_DEFAULT_SECONDS = 180
        const val GENERATION_TIMEOUT_MAX_SECONDS = 1800

        fun defaultEndpoint(provider: AiProvider): String = when (provider) {
            AiProvider.OPENAI -> "https://api.openai.com/v1"
            AiProvider.CLAUDE -> "https://api.anthropic.com"
            AiProvider.GEMINI -> "https://generativelanguage.googleapis.com"
        }

        fun defaultModel(provider: AiProvider): Model = when (provider) {
            AiProvider.OPENAI -> Model.OpenAI.GPT_5_4_MINI
            AiProvider.CLAUDE -> Model.Claude.CLAUDE_4_5_HAIKU
            AiProvider.GEMINI -> Model.Gemini.GEMINI_3_5_FLASH
        }

        fun suggestedModels(provider: AiProvider): List<Model> = when (provider) {
            AiProvider.OPENAI -> listOf(
                Model.OpenAI.GPT_5_5,
                Model.OpenAI.GPT_5_4,
                Model.OpenAI.GPT_5_4_MINI,
                Model.OpenAI.GPT_5_4_NANO,
            )
            AiProvider.CLAUDE -> listOf(
                Model.Claude.CLAUDE_OPUS_4_8,
                Model.Claude.CLAUDE_4_6_SONNET,
                Model.Claude.CLAUDE_4_5_HAIKU,
            )

            AiProvider.GEMINI -> listOf(
                Model.Gemini.GEMINI_3_5_FLASH,
                Model.Gemini.GEMINI_3_1_PRO_PREVIEW,
                Model.Gemini.GEMINI_3_FLASH_PREVIEW,
                Model.Gemini.GEMINI_3_1_FLASH_LITE,
            )
        }
    }
}
