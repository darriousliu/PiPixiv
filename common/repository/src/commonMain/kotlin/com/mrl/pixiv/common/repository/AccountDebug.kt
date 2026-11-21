package com.mrl.pixiv.common.repository

import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import com.mrl.pixiv.common.util.isDebug
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.CancellationException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object AccountDebug {
    suspend fun expireAccessTokenOnStartup() {
        if (!isDebug) return
        withIOContext {
            withAccountStorageLock {
                val session = AccountStore.current ?: return@withAccountStorageLock
                if (consume { if (it.expireAccessTokenOnce) it.copy(expireAccessTokenOnce = false) else null }) {
                    AccountStore.update(session.userId) { it.copy(accessTokenExpiresAt = 0L) }
                }
            }
        }
    }

    fun throwInvalidGrantIfRequested(userId: Long) {
        if (!isDebug) return
        withAccountStorageLock {
            if (AccountStore.activeUserId != userId) return@withAccountStorageLock
            if (consume { if (it.forceInvalidGrantOnce) it.copy(forceInvalidGrantOnce = false) else null }) {
                throw InvalidRefreshGrantException()
            }
        }
    }

    // The caller holds the account lock so a switch cannot race with consuming a flag.
    private fun consume(clearFlag: (Options) -> Options?): Boolean {
        try {
            val filesRoot = FileKit.filesDir.toKotlinxIoPath()
            val path = Path(filesRoot, "account-debug.json")
            if (!SystemFileSystem.exists(path)) return false
            val options = SystemFileSystem.source(path).buffered().use { source ->
                require(!source.request(4097)) { "Account debug configuration is too large" }
                Json.decodeFromString<Options>(source.readString())
            }
            val remaining = clearFlag(options) ?: return false
            if (remaining == Options()) {
                SystemFileSystem.delete(path)
            } else {
                val temporary = Path(filesRoot, "account-debug.json.tmp")
                try {
                    SystemFileSystem.sink(temporary).buffered().use { sink ->
                        sink.write(Json.encodeToString(remaining).encodeToByteArray())
                    }
                    SystemFileSystem.atomicMove(temporary, path)
                } finally {
                    runCatching { SystemFileSystem.delete(temporary, mustExist = false) }
                }
            }
            return true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Logger.w(tag = "AccountDebug") { "Could not read or consume account-debug.json" }
            return false
        }
    }

    @Serializable
    private data class Options(
        val forceInvalidGrantOnce: Boolean = false,
        val expireAccessTokenOnce: Boolean = false,
    )
}
