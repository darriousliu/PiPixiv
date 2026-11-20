package com.mrl.pixiv.common.coroutine

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CoroutineExtTest {
    @Test
    fun cancelledLaunchCallsErrorHandlerWithoutLoggingAnError() = withErrorLogs { loggedErrors ->
        val loading = MutableStateFlow(false)
        var handledError: Throwable? = null
        val job = launchCatch(
            start = CoroutineStart.UNDISPATCHED,
            loading = loading,
            error = { handledError = it },
        ) {
            awaitCancellation()
        }

        assertTrue(loading.value)
        job.cancelAndJoin()

        assertIs<CancelException>(handledError)
        assertFalse(loading.value)
        assertTrue(loggedErrors.isEmpty())
    }

    @Test
    fun failedLaunchLogsExactlyOneError() = withErrorLogs { loggedErrors ->
        val failure = IllegalStateException("Test failure")
        var handledError: Throwable? = null

        launchCatch(error = { handledError = it }) { throw failure }.join()

        assertSame(failure, handledError)
        assertSame(failure, loggedErrors.single())
    }

    private fun withErrorLogs(block: suspend CoroutineScope.(List<Throwable?>) -> Unit) = runBlocking {
        val originalWriters = Logger.config.logWriterList
        val originalSeverity = Logger.config.minSeverity
        val loggedErrors = mutableListOf<Throwable?>()
        val writer = object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                if (severity == Severity.Error && tag == "Coroutine") {
                    loggedErrors += throwable
                }
            }
        }
        val supervisor = SupervisorJob()
        try {
            Logger.setLogWriters(writer)
            Logger.setMinSeverity(Severity.Error)
            CoroutineScope(coroutineContext + supervisor).block(loggedErrors)
        } finally {
            supervisor.cancelAndJoin()
            Logger.setLogWriters(originalWriters)
            Logger.setMinSeverity(originalSeverity)
        }
    }
}
