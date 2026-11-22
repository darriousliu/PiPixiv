package com.mrl.pixiv.common.logs

import co.touchlab.kermit.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.Path
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppLogStoreTest {
    @Test
    fun capturesInfoAndAboveInTheNewestThousandEntries() = runBlocking {
        withRecorder { recorder ->
            repeat(1001) { recorder.writer.log(Severity.Info, "info-$it", "Test") }
            recorder.writer.log(Severity.Warn, "warning", "Test")
            recorder.writer.log(Severity.Error, "error", "Test")
            recorder.writer.log(Severity.Debug, "debug", "Test")

            val entries = recorder.state.value.current
            assertEquals(1000, entries.size)
            assertEquals("info-3", entries.first().message)
            assertEquals(listOf(Severity.Info, Severity.Warn, Severity.Error), entries.takeLast(3).map { it.level })
        }
    }

    @Test
    fun stripsOnlyUrlQueryAndFragmentFromStoredMessageAndTrace() = runBlocking {
        val url = "https://a.example/x?word=secret#f"
        assertEquals("https://a.example/x", stripAppLogUrlQueryAndFragment(url))
        assertEquals("Text without a URL", stripAppLogUrlQueryAndFragment("Text without a URL"))
        assertEquals(
            "(https://a.example/x) and HTTP://b.example/path",
            stripAppLogUrlQueryAndFragment("($url) and HTTP://b.example/path#fragment"),
        )
        assertEquals(
            "pixiv://account/login",
            stripAppLogUrlQueryAndFragment("pixiv://account/login?code=abc&via=login"),
        )
        assertEquals(
            "(App+auth.v2-test://account/login).",
            stripAppLogUrlQueryAndFragment("(App+auth.v2-test://account/login?code=abc#fragment)."),
        )

        withRecorder { recorder ->
            recorder.writer.log(Severity.Error, "Request failed ($url)", "Test", IllegalStateException(url))
            val entry = recorder.state.value.current.single()
            assertEquals("Request failed (https://a.example/x)", entry.message)
            val trace = assertNotNull(entry.stackTrace)
            assertTrue("IllegalStateException: https://a.example/x" in trace)
            assertFalse("secret" in trace)

            recorder.writer.log(Severity.Info, "截".repeat(3000), "Test")
            val truncated = recorder.state.value.current.last().formatText()
            assertTrue(truncated.endsWith("[truncated]"))
            assertTrue(truncated.encodeToByteArray().size <= 8 * 1024)
            assertFalse('\uFFFD' in truncated)
        }
    }

    @Test
    fun rotatesCappedCurrentLogAndClearsBothRuns() = runBlocking {
        val directory = Files.createTempDirectory("pipixiv-app-logs-test")
        val current = directory.resolve("current.log")
        val previous = directory.resolve("previous.log")
        val logDirectory = Path(directory.toString())
        try {
            val oldCurrent = withRecorder(logDirectory) { recorder ->
                repeat(110) {
                    recorder.writer.log(Severity.Info, "entry-$it " + "x".repeat(6000), "Test")
                }
                withTimeout(5_000L) { recorder.awaitWrites() }
                assertTrue(Files.size(current) <= 512 * 1024)
                val written = Files.readString(current)
                assertFalse("entry-0 " in written)
                assertTrue("entry-109 " in written)
                withRecorder(logDirectory) { secondRecorder ->
                    secondRecorder.writer.log(Severity.Info, "second instance", "Test")
                    withTimeout(5_000L) { secondRecorder.awaitWrites() }
                    assertEquals("second instance", secondRecorder.state.value.current.single().message)
                    assertEquals(written, Files.readString(current))
                    assertFalse(Files.exists(previous))
                }
                Files.readAllBytes(current)
            }

            withRecorder(logDirectory) { recorder ->
                recorder.start()
                withTimeout(5_000L) { recorder.awaitWrites() }
                assertContentEquals(oldCurrent, Files.readAllBytes(previous))
                assertEquals(0L, Files.size(current))
                assertTrue(recorder.state.value.previous.last().message.startsWith("entry-109 "))
                assertTrue(recorder.state.value.current.isEmpty())

                recorder.clear()
                withTimeout(5_000L) { recorder.awaitWrites() }
                assertTrue(recorder.state.value.previous.isEmpty())
                assertFalse(Files.exists(current))
                assertFalse(Files.exists(previous))
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun keepsPreviousLogAfterAnEmptyRun() = runBlocking {
        val directory = Files.createTempDirectory("pipixiv-app-logs-test")
        val logDirectory = Path(directory.toString())
        val current = directory.resolve("current.log")
        val previous = directory.resolve("previous.log")
        try {
            val crashLog = withRecorder(logDirectory) { recorder ->
                recorder.writer.log(Severity.Assert, "Unhandled exception", "AppCrash", IllegalStateException("crash"))
                withTimeout(5_000L) { recorder.awaitWrites() }
                Files.readAllBytes(current)
            }

            withRecorder(logDirectory) { recorder ->
                recorder.start()
                withTimeout(5_000L) { recorder.awaitWrites() }
                assertContentEquals(crashLog, Files.readAllBytes(previous))
                assertEquals(0L, Files.size(current))
            }

            withRecorder(logDirectory) { recorder ->
                recorder.start()
                withTimeout(5_000L) { recorder.awaitWrites() }
                assertContentEquals(crashLog, Files.readAllBytes(previous))
                assertEquals(0L, Files.size(current))
                val entry = recorder.state.value.previous.single()
                assertEquals(Severity.Assert, entry.level)
                assertTrue("IllegalStateException: crash" in assertNotNull(entry.stackTrace))
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun copiesOnlyNewestLogsUnderTheByteLimitWithAHeaderAndOmittedCount() {
        val header = AppLogHeader(
            appVersion = "2.6.0",
            appBuild = "20600",
            flavor = "default",
            platform = "Desktop",
            osVersion = "OS 15",
            deviceModel = "Device A",
            copiedAt = "2026-11-20T12:00:00+08:00",
        )
        val older = AppLogEntry("2026-11-20T16:00:00+08:00", Severity.Info, "Test", "older", null)
        val newer = AppLogEntry("2026-11-20T09:00:00Z", Severity.Error, "Test", "newer", "stack")
        val copy = formatCopiedLogs(header, listOf(newer, older)) { "$it older entries omitted" }
        assertTrue(
            copy.startsWith(
                "App version: 2.6.0 (20600)\nFlavor: default\nPlatform: Desktop\nOS version: OS 15\n" +
                    "Device model: Device A\nCopy time: 2026-11-20T12:00:00+08:00\n",
            ),
        )
        assertTrue(copy.indexOf("older") in 0 until copy.indexOf("newer"))
        val smallerCopy = formatCopiedLogs(header, listOf(newer, older), maxBytes = 218) {
            "$it older entries omitted"
        }
        assertTrue("1 older entries omitted" in smallerCopy)
        assertFalse("] Test: older" in smallerCopy)
        assertTrue(smallerCopy.endsWith("newer\nstack\n"))
        assertTrue(smallerCopy.encodeToByteArray().size <= 218)
    }

    private suspend fun <T> withRecorder(path: Path? = null, block: suspend (AppLogRecorder) -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return try {
            block(AppLogRecorder(scope) { path })
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }
}
