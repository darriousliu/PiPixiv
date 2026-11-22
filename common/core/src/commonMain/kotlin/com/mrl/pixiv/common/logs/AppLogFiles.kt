package com.mrl.pixiv.common.logs

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val fileByteLimit = 512 * 1024
private const val trimTargetBytes = fileByteLimit * 7 / 8

internal class AppLogFiles(directory: Path) : AutoCloseable {
    private val current = Path(directory, "current.log")
    private val previous = Path(directory, "previous.log")
    private val temporary = Path(directory, "current.log.tmp")
    private val records = ArrayDeque<ByteArray>()
    private var fileBytes = 0
    private var needsRewrite = false
    private val directoryLease: AutoCloseable
    val previousEntries: List<AppLogEntry>

    init {
        SystemFileSystem.createDirectories(directory)
        directoryLease = prepareAppLogDirectory(PlatformFile(directory.toString()))
        previousEntries = try {
            rotate()
        } catch (error: Throwable) {
            close()
            throw error
        }
    }

    private fun rotate(): List<AppLogEntry> {
        with(SystemFileSystem) {
            delete(temporary, mustExist = false)
            if ((metadataOrNull(current)?.size ?: 0L) > 0L) atomicMove(current, previous)
            sink(current).close()
            val size = metadataOrNull(previous)?.size ?: return emptyList()
            val text = source(previous).buffered().use { input ->
                // Files written by this app are capped, but still handle an oversized older file.
                input.skip((size - fileByteLimit).coerceAtLeast(0))
                input.readString()
            }
            val completeLines = if (size > fileByteLimit) text.substringAfter('\n', "") else text
            if (size > fileByteLimit) {
                write(temporary, listOf(completeLines.encodeToByteArray()))
                atomicMove(temporary, previous)
            }
            return completeLines.lineSequence().mapNotNull { line ->
                runCatching { Json.decodeFromString<AppLogEntry>(line) }.getOrNull()
            }.toList()
        }
    }

    fun append(entries: List<AppLogEntry>) {
        if (entries.isEmpty()) return
        val added = entries.map { (Json.encodeToString(it) + "\n").encodeToByteArray() }
        val size = fileBytes + added.sumOf { it.size }
        needsRewrite = try {
            if (size > fileByteLimit || needsRewrite) {
                val all = records + added
                val kept = ArrayDeque<ByteArray>()
                var remaining = 0
                val limit = if (size > fileByteLimit) trimTargetBytes else fileByteLimit
                for (record in all.asReversed()) {
                    if (remaining + record.size > limit) break
                    kept.addFirst(record)
                    remaining += record.size
                }
                write(temporary, kept)
                SystemFileSystem.atomicMove(temporary, current)
                records.clear()
                records.addAll(kept)
                fileBytes = remaining
            } else {
                write(current, added, append = true)
                records.addAll(added)
                fileBytes = size
            }
            false
        } catch (error: Exception) {
            // A failed append may have written a partial record. Replace it on the next write.
            needsRewrite = true
            throw error
        }
    }

    fun clear() {
        with(SystemFileSystem) {
            delete(temporary, mustExist = false)
            delete(current, mustExist = false)
            delete(previous, mustExist = false)
        }
        records.clear()
        fileBytes = 0
        needsRewrite = false
    }

    override fun close() = directoryLease.close()

    private fun write(path: Path, records: List<ByteArray>, append: Boolean = false) {
        SystemFileSystem.sink(path, append).buffered().use { output ->
            records.forEach { output.write(it) }
        }
    }
}
