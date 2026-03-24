package com.mrl.pixiv.common.ai.provider

import com.mrl.pixiv.common.ai.AiTextRequest
import com.mrl.pixiv.common.ai.AiTextResponse
import com.mrl.pixiv.common.data.setting.AiProvider

interface AiTextProviderClient {
    val provider: AiProvider

    suspend fun generateText(request: AiTextRequest): AiTextResponse
}
