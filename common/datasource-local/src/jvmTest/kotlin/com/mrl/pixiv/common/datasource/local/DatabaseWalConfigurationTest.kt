package com.mrl.pixiv.common.datasource.local

import androidx.room3.RoomDatabase
import androidx.room3.Transactor
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseWalConfigurationTest {
    @Test
    fun globalAndAccountConnectionsUseTheWalSizeLimit() = runTest {
        val directory = createTempDirectory("pixiv-wal-config-").toFile()
        val global = buildGlobalDatabase(Path(directory.resolve("pixiv_db").absolutePath))
        val account = buildAccountDatabase(Path(directory.resolve("account.db").absolutePath))
        try {
            assertWalSizeLimit(global)
            assertWalSizeLimit(account)
        } finally {
            account.close()
            global.close()
            directory.deleteRecursively()
        }
    }

    private suspend fun assertWalSizeLimit(database: RoomDatabase) {
        assertEquals(4L * 1024L * 1024L, database.useReaderConnection { it.journalSizeLimit() })
        assertEquals(4L * 1024L * 1024L, database.useWriterConnection { it.journalSizeLimit() })
    }

    private suspend fun Transactor.journalSizeLimit(): Long =
        usePrepared("PRAGMA main.journal_size_limit") { statement ->
            check(statement.step())
            statement.getLong(0)
        }
}
