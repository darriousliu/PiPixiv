package com.mrl.pixiv.common.logs

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.files.Path

private const val entryLimit = 1000
private const val crashFlushTimeoutMillis = 2_000L

data class AppLogSnapshot(
    val current: List<AppLogEntry> = emptyList(),
    val previous: List<AppLogEntry> = emptyList(),
    internal val revision: Long = 0,
    internal val clearedAt: Long = 0,
)

object AppLogStore {
    private val recorder = AppLogRecorder(CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
        Path(FileKit.filesDir.toKotlinxIoPath(), "logs")
    }

    val state: StateFlow<AppLogSnapshot> get() = recorder.state
    val writer: LogWriter get() = recorder.writer

    fun start() = recorder.start()

    fun clear() = recorder.clear()

    fun recordCrash(throwable: Throwable, tag: String, message: String) {
        // The crash caller waits briefly while the regular background writer drains its queue.
        runCatching {
            val revision = recorder.record(Severity.Assert, message, tag, throwable)
            runBlocking {
                withTimeoutOrNull(crashFlushTimeoutMillis) { recorder.awaitWrites(revision) }
            }
        }
    }
}

internal class AppLogRecorder(
    scope: CoroutineScope,
    private val directory: () -> Path?,
) {
    private val pendingWrite = Channel<Unit>(Channel.CONFLATED)
    private val writtenRevision = MutableStateFlow(-1L)
    private val mutableState = MutableStateFlow(AppLogSnapshot())

    val state = mutableState.asStateFlow()

    val writer = object : LogWriter() {
        override fun isLoggable(tag: String, severity: Severity) = severity >= Severity.Info

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            if (isLoggable(tag, severity)) record(severity, message, tag, throwable)
        }
    }

    init {
        scope.launch { writeLogs() }
    }

    fun start() {
        pendingWrite.trySend(Unit)
    }

    fun record(severity: Severity, message: String, tag: String, throwable: Throwable?): Long {
        val entry = AppLogEntry(
            timestamp = appLogTimestamp(),
            level = severity,
            tag = tag,
            message = stripAppLogUrlQueryAndFragment(message),
            stackTrace = throwable?.stackTraceToString()?.let(::stripAppLogUrlQueryAndFragment),
        ).limitSize()
        val snapshot = mutableState.updateAndGet {
            val revision = it.revision + 1
            it.copy(
                current = it.current.takeLast(entryLimit - 1) + entry.copy(id = revision),
                revision = revision,
            )
        }
        pendingWrite.trySend(Unit)
        return snapshot.revision
    }

    fun clear() {
        mutableState.update {
            val revision = it.revision + 1
            AppLogSnapshot(revision = revision, clearedAt = revision)
        }
        pendingWrite.trySend(Unit)
    }

    suspend fun awaitWrites(revision: Long = state.value.revision) {
        writtenRevision.first { it >= revision }
    }

    private suspend fun writeLogs() {
        // Wait for startup or the first log so the platform file service can be initialized first.
        pendingWrite.receive()
        runCatching { directory()?.let(::AppLogFiles) }.getOrNull().use { files ->
            val previous = files?.previousEntries.orEmpty()
            mutableState.update { if (it.clearedAt == 0L) it.copy(previous = previous.takeLast(entryLimit)) else it }

            var savedRevision = 0L
            var clearedAt = 0L
            do {
                val snapshot = mutableState.value
                runCatching {
                    if (snapshot.clearedAt != clearedAt) {
                        files?.clear()
                        clearedAt = snapshot.clearedAt
                    }
                    files?.append(snapshot.current.filter { it.id > savedRevision })
                    savedRevision = snapshot.revision
                }
                writtenRevision.value = snapshot.revision
            } while (pendingWrite.receiveCatching().isSuccess)
        }
    }
}
