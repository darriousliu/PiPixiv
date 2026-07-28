package com.mrl.pixiv.common.ai

class AiHttpStatusException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message)
