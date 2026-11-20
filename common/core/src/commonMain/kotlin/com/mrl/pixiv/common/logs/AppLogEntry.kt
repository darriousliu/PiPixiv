package com.mrl.pixiv.common.logs

import co.touchlab.kermit.Severity
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.format
import kotlinx.datetime.offsetAt
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

private const val entryByteLimit = 8 * 1024
private const val copyByteLimit = 200 * 1024
private const val truncationMarker = "… [truncated]"

@Serializable
data class AppLogEntry(
    val timestamp: String,
    val level: Severity,
    val tag: String,
    val message: String,
    val stackTrace: String?,
    val id: Long = 0,
) {
    fun formatText(): String = buildString {
        append(header())
        append(message)
        stackTrace?.let { append('\n').append(it) }
    }

    private fun header() = "$timestamp [${level.name}] $tag: "

    internal fun limitSize(): AppLogEntry {
        val bounded = copy(tag = tag.truncateUtf8(256))
        val remaining = entryByteLimit - bounded.header().utf8Size()
        val traceSpace = if (stackTrace == null) 0 else truncationMarker.utf8Size() + 1
        val boundedMessage = message.truncateUtf8(remaining - traceSpace)
        val boundedTrace = stackTrace?.truncateUtf8(remaining - boundedMessage.utf8Size() - 1)
        return bounded.copy(message = boundedMessage, stackTrace = boundedTrace)
    }
}

data class AppLogHeader(
    val appVersion: String,
    val appBuild: String,
    val flavor: String,
    val platform: String,
    val osVersion: String,
    val deviceModel: String,
    val copiedAt: String = appLogTimestamp(),
) {
    internal fun formatText() = buildString {
        appendLine("App version: $appVersion ($appBuild)")
        appendLine("Flavor: $flavor")
        appendLine("Platform: $platform")
        appendLine("OS version: $osVersion")
        appendLine("Device model: $deviceModel")
        appendLine("Copy time: $copiedAt")
    }
}

fun appLogTimestamp(): String {
    val instant = Clock.System.now()
    val offset = TimeZone.currentSystemDefault().offsetAt(instant)
    return DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.format { setDateTimeOffset(instant, offset) }
}

fun formatCopiedLogs(
    header: AppLogHeader,
    entries: List<AppLogEntry>,
    maxBytes: Int = copyByteLimit,
    omittedNote: (Int) -> String,
): String {
    val headerText = header.formatText() + "\n"
    val rendered = entries
        .map { entry -> runCatching { Instant.parse(entry.timestamp) }.getOrNull() to entry }
        .sortedBy { it.first }
        .map { it.second.formatText() + "\n" }
    val sizes = rendered.map(String::utf8Size)
    var bytes = headerText.utf8Size() + sizes.sum()
    var omitted = 0
    while (bytes > maxBytes && omitted < rendered.size) {
        bytes -= sizes[omitted]
        omitted++
    }
    var note = if (omitted == 0) "" else omittedNote(omitted) + "\n\n"
    while (bytes + note.utf8Size() > maxBytes && omitted < rendered.size) {
        bytes -= sizes[omitted]
        omitted++
        note = omittedNote(omitted) + "\n\n"
    }
    return buildString {
        append(headerText)
        append(note)
        for (index in omitted until rendered.size) append(rendered[index])
    }.truncateUtf8(maxBytes)
}

private val schemeUrl = Regex("""\b[a-zA-Z][a-zA-Z0-9+.-]*://[^\s<>"']+""")

internal fun stripAppLogUrlQueryAndFragment(text: String): String = schemeUrl.replace(text) { match ->
    val url = match.value
    val prefix = url.substringBefore('?').substringBefore('#')
    if (prefix == url) url else prefix + url.takeLastWhile { it in ")]},.;" }
}

private fun String.utf8Size() = encodeToByteArray().size

private fun String.truncateUtf8(maxBytes: Int): String {
    val bytes = encodeToByteArray()
    if (bytes.size <= maxBytes) return this
    val markerSize = truncationMarker.utf8Size()
    if (maxBytes < markerSize) return ""
    var end = maxBytes - markerSize
    // Do not leave a partial code point at the end of the UTF-8 prefix.
    while (end > 0 && (bytes[end].toInt() and 0xC0) == 0x80) end--
    return bytes.decodeToString(endIndex = end) + truncationMarker
}
