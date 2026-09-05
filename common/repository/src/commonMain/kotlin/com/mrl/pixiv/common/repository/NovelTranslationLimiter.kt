package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class NovelTranslationLimiter {
    private val mutex = Mutex()
    private val waiting = mutableListOf<Ticket>()
    private val active = mutableListOf<Ticket>()

    suspend fun <T> withPermit(
        maxConcurrentRequests: Int,
        block: suspend () -> T,
    ): T {
        val ticket = Ticket(
            limit = maxConcurrentRequests.coerceIn(
                AiTranslationConfig.MAX_CONCURRENT_REQUESTS_MIN,
                AiTranslationConfig.MAX_CONCURRENT_REQUESTS_MAX,
            ),
        )
        var registered = false

        try {
            mutex.withLock {
                waiting += ticket
                registered = true
                grantAvailableLocked()
            }
            ticket.ready.await()
            return block()
        } finally {
            if (registered) {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (ticket.granted) {
                            check(active.remove(ticket)) {
                                "Granted translation request was not active."
                            }
                            ticket.granted = false
                        } else {
                            waiting.remove(ticket)
                        }
                        grantAvailableLocked()
                    }
                }
            }
        }
    }

    private fun grantAvailableLocked() {
        while (waiting.isNotEmpty()) {
            val next = waiting.first()
            val activeLimit = active.minOfOrNull { it.limit } ?: next.limit
            val effectiveLimit = minOf(next.limit, activeLimit)
            if (active.size >= effectiveLimit) return

            waiting.removeAt(0)
            next.granted = true
            active += next
            next.ready.complete(Unit)
        }
    }

    private class Ticket(
        val limit: Int,
        val ready: CompletableDeferred<Unit> = CompletableDeferred(),
        var granted: Boolean = false,
    )
}
