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
        GPT_5_4("gpt-5.4"),
        GPT_5_4_MINI("gpt-5.4-mini"),
        GPT_5_4_NANO("gpt-5.4-nano"),
    }

    enum class Claude(override val modelId: String) : Model {
        CLAUDE_4_6_SONNET("claude-sonnet-4-6"),
        CLAUDE_4_5_HAIKU("claude-haiku-4-5"),
    }

    enum class Gemini(override val modelId: String) : Model {
        GEMINI_3_1_FLASH("gemini-3-flash-preview"),
        GEMINI_3_1_FLASH_LITE("gemini-3.1-flash-lite-preview"),
        GEMINI_3_1_PRO("gemini-3.1-pro-preview"),
    }
}

@Serializable
data class AiTranslationConfig(
    val provider: AiProvider = AiProvider.OPENAI,
    val endpoint: String = defaultEndpoint(AiProvider.OPENAI),
    val apiKey: String = "",
    val model: String = defaultModel(AiProvider.OPENAI).modelId,
    val responseApi: Boolean = false,
) {
    companion object {
        fun defaultEndpoint(provider: AiProvider): String = when (provider) {
            AiProvider.OPENAI -> "https://api.openai.com/v1"
            AiProvider.CLAUDE -> "https://api.anthropic.com"
            AiProvider.GEMINI -> "https://generativelanguage.googleapis.com"
        }

        fun defaultModel(provider: AiProvider): Model = when (provider) {
            AiProvider.OPENAI -> Model.OpenAI.GPT_5_4_MINI
            AiProvider.CLAUDE -> Model.Claude.CLAUDE_4_5_HAIKU
            AiProvider.GEMINI -> Model.Gemini.GEMINI_3_1_FLASH
        }

        fun suggestedModels(provider: AiProvider): List<Model> = when (provider) {
            AiProvider.OPENAI -> listOf(
                Model.OpenAI.GPT_5_4,
                Model.OpenAI.GPT_5_4_MINI,
                Model.OpenAI.GPT_5_4_NANO,
            )
            AiProvider.CLAUDE -> listOf(
                Model.Claude.CLAUDE_4_6_SONNET,
                Model.Claude.CLAUDE_4_5_HAIKU,
            )

            AiProvider.GEMINI -> listOf(
                Model.Gemini.GEMINI_3_1_FLASH,
                Model.Gemini.GEMINI_3_1_FLASH_LITE,
                Model.Gemini.GEMINI_3_1_PRO
            )
        }
    }
}

