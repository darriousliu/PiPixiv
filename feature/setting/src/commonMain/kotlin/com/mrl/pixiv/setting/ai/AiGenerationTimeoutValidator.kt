package com.mrl.pixiv.setting.ai

import com.mrl.pixiv.common.data.setting.AiTranslationConfig

internal fun parseGenerationTimeoutSeconds(input: String): Int? {
    if (input.isEmpty() || input.any { it !in '0'..'9' }) {
        return null
    }
    return input.toIntOrNull()?.takeIf {
        it in AiTranslationConfig.GENERATION_TIMEOUT_MIN_SECONDS..
            AiTranslationConfig.GENERATION_TIMEOUT_MAX_SECONDS
    }
}
