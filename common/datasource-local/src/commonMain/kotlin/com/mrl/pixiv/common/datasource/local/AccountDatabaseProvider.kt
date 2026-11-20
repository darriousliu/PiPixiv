package com.mrl.pixiv.common.datasource.local

import androidx.room3.useReaderConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class AccountHandle(val userId: Long, val db: AccountDatabase)

class AccountDatabaseProvider(
    private val accountsRoot: () -> Path = ::accountStorageRoot,
) {
    private val mutex = Mutex()
    private val handles = mutableMapOf<Long, AccountHandle>()
    private val currentHandle = MutableStateFlow<AccountHandle?>(null)
    val current: StateFlow<AccountHandle?> = currentHandle.asStateFlow()

    fun requireCurrent(): AccountHandle =
        checkNotNull(current.value) { "No account database is active." }

    fun databasePath(userId: Long): Path = Path(accountDirectory(userId), "account.db")

    suspend fun open(userId: Long): AccountHandle = mutex.withLock {
        openLocked(userId)
    }

    suspend fun publish(userId: Long?) = mutex.withLock {
        currentHandle.value = userId?.let { openLocked(it) }
    }

    fun clearCurrent() {
        currentHandle.value = null
    }

    suspend fun closeAndDelete(userId: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val directory = accountDirectory(userId)
            if (currentHandle.value?.userId == userId) currentHandle.value = null
            handles.remove(userId)?.db?.close()
            val parent = checkNotNull(directory.parent)
            if (SystemFileSystem.exists(parent)) {
                deleteAccountDirectory(Path(SystemFileSystem.resolve(parent), directory.name))
            }
        }
    }

    suspend fun close() = mutex.withLock {
        currentHandle.value = null
        handles.values.forEach { it.db.close() }
        handles.clear()
    }

    private fun accountDirectory(userId: Long): Path {
        require(userId > 0L) { "An account database requires a positive user ID." }
        return Path(accountsRoot(), userId.toString())
    }

    private suspend fun openLocked(userId: Long): AccountHandle =
        handles[userId] ?: withContext(Dispatchers.IO) {
            val database = buildAccountDatabase(databasePath(userId))
            try {
                database.useReaderConnection { }
                AccountHandle(userId, database).also { handles[userId] = it }
            } catch (error: Throwable) {
                database.close()
                throw error
            }
        }

    private fun deleteAccountDirectory(path: Path) {
        val metadata = SystemFileSystem.metadataOrNull(path)
        // An account directory may contain a symbolic link to data owned elsewhere.
        if (metadata?.isDirectory == true && SystemFileSystem.resolve(path) == path) {
            SystemFileSystem.list(path).forEach(::deleteAccountDirectory)
        }
        SystemFileSystem.delete(path, mustExist = false)
    }
}
