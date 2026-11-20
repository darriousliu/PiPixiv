package com.mrl.pixiv.common.util

import com.mrl.pixiv.common.analytics.logException
import com.mrl.pixiv.common.analytics.recordCrashOperation
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.StringResource

suspend fun selectSaveFile(
    suggestedName: String,
    defaultExtension: String?,
    failureMessage: StringResource,
): PlatformFile? {
    recordCrashOperation("FilePickerOpened")
    return try {
        platformSelectSaveFile(suggestedName, defaultExtension).also {
            recordCrashOperation("FilePickerClosed")
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        recordCrashOperation("FilePickerFailed")
        logException(error)
        ToastUtil.safeShortToast(failureMessage, error.message.orEmpty())
        null
    }
}

internal expect suspend fun platformSelectSaveFile(
    suggestedName: String,
    defaultExtension: String?,
): PlatformFile?
