package com.mrl.pixiv.login

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.analytics.recordCrashOperation
import com.mrl.pixiv.common.router.NavigationManager
import kotlinx.coroutines.CancellationException

internal fun NavigationManager.canFinishLogin(): Boolean = entryId == null || entryId == topEntryId

internal suspend fun recordLoginAttempt(login: suspend () -> Unit): Boolean {
    recordCrashOperation("LoginStarted")
    try {
        login()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Logger.e(tag = "Login") { "Login failed: ${error::class.simpleName ?: "Exception"}" }
        recordCrashOperation("LoginFailed")
        return false
    }
    recordCrashOperation("LoginSucceeded")
    return true
}
