package com.mrl.pixiv

import com.mrl.pixiv.common.analytics.captureDesktopFatalException
import com.mrl.pixiv.common.analytics.flushDesktopAnalytics
import dev.nucleusframework.window.tao.TaoApplication
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

private val fatalReportingInstalled = AtomicBoolean()
// JUL otherwise holds named loggers weakly before TaoApplication initializes.
private val taoFatalLogger = Logger.getLogger(TaoApplication::class.java.name)

internal fun installTaoFatalErrorReporting() {
    if (!fatalReportingInstalled.compareAndSet(false, true)) return
    // Tao catches these failures before exiting, so the SDK's JVM uncaught handler
    // never sees them. Keep its existing logging and native error dialog intact.
    taoFatalLogger.addHandler(
        TaoFatalErrorHandler(taoFatalLogger.name) { error ->
            captureDesktopFatalException(error)
            flushDesktopAnalytics(2_000)
        },
    )
}

internal class TaoFatalErrorHandler(
    private val loggerName: String,
    private val report: (Throwable) -> Unit,
) : Handler() {
    private val reported = AtomicBoolean()

    init {
        level = Level.SEVERE
    }

    override fun publish(record: LogRecord?) {
        if (record == null || !isLoggable(record)) return
        if (record.loggerName != loggerName) return
        val error = record.thrown ?: return
        // These are the two process-fatal exits in Nucleus 2.5.14. In particular,
        // do not turn recoverable SEVERE logs or shutdown follow-up errors into crashes.
        if (record.message != "Unhandled exception on the Tao main thread — closing" &&
            record.message != "taoApplication failed"
        ) return
        if (!reported.compareAndSet(false, true)) return
        try {
            report(error)
        } catch (_: Throwable) {
            // Reporting must never prevent Tao from finishing its fatal exit.
            System.err.println("Failed to report Tao fatal error")
        }
    }

    override fun flush() = Unit
    override fun close() = Unit
}
