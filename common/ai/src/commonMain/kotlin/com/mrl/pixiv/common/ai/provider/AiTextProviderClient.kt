package com.mrl.pixiv.common.ai.provider

import com.mrl.pixiv.common.ai.AiTextRequest
import com.mrl.pixiv.common.ai.AiTextResponse
import com.mrl.pixiv.common.data.setting.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface AiTextStreamEvent {
    data class Delta(val text: String) : AiTextStreamEvent

    data class Completed(val text: String) : AiTextStreamEvent
}

interface AiTextProviderClient {
    val provider: AiProvider

    suspend fun generateText(request: AiTextRequest): AiTextResponse

    fun generateTextStream(request: AiTextRequest): Flow<AiTextStreamEvent> = flow {
        val text = generateText(request).text
        emit(AiTextStreamEvent.Delta(text))
        emit(AiTextStreamEvent.Completed(text))
    }
}
