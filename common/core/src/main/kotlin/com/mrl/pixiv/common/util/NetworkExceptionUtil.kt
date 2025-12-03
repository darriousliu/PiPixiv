package com.mrl.pixiv.common.util

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object NetworkExceptionUtil : CoroutineScope by MainScope() {
    private const val EVENT_HTTP_EXCEPTION = "http_exception"

    @OptIn(DelicateCoroutinesApi::class)
    fun resolveException(e: Throwable) {
        e.printStackTrace()

        when (e) {
            is ClientRequestException -> {
                val code = e.response.status.value
                GlobalScope.launch(Dispatchers.IO) {
                    logEvent(
                        EVENT_HTTP_EXCEPTION,
                        mapOf(
                            "code" to code,
                            "message" to e.response.bodyAsText(),
                            "url" to e.response.request.url.toString()
                        )
                    )
                }
            }

            else -> {
                logException(e)
            }
        }
    }
}