package com.mrl.pixiv.common.coroutine

import kotlinx.io.IOException

class CancelException(cause: Exception) : IOException(cause.message.orEmpty(), cause)