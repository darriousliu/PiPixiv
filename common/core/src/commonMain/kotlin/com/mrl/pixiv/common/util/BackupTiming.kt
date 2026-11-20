package com.mrl.pixiv.common.util

import kotlin.time.Duration
import kotlin.time.TimeSource

enum class BackupStep {
    SerializeJson,
    Compress,
    CopyToDestination,
    CopyToCache,
    ReadZipDirectory,
    ExtractSnapshots,
}

typealias BackupTiming = (BackupStep, Duration) -> Unit

inline fun <T> BackupTiming?.measureBackupStep(step: BackupStep, block: () -> T): T {
    if (this == null) return block()
    val start = TimeSource.Monotonic.markNow()
    val result = block()
    invoke(step, start.elapsedNow())
    return result
}
