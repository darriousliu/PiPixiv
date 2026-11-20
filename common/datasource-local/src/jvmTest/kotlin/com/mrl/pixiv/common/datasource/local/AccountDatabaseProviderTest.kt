package com.mrl.pixiv.common.datasource.local

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountDatabaseProviderTest {
    @Test
    fun `startup preserves corrupt account data and leaves an inaccessible path intact`() = runTest {
        val directory = Files.createTempDirectory("account-database-").toFile()
        val provider = AccountDatabaseProvider { Path(directory.absolutePath) }
        try {
            val corrupt = File(provider.databasePath(1L).toString())
            val bytes = Random(128).nextBytes(512)
            check(corrupt.parentFile.mkdirs())
            corrupt.writeBytes(bytes)
            val failure = mutableListOf<Exception>()

            provider.publish(1L, failure::add)

            assertEquals(1L, provider.requireCurrent().userId)
            assertEquals(0, provider.requireCurrent().db.browsingHistoryDao().countNovels(1L))
            val preserved = corrupt.parentFile.listFiles().orEmpty().single {
                it.name.matches(Regex("account\\.db\\.corrupt-\\d+"))
            }
            assertContentEquals(bytes, preserved.readBytes())
            assertEquals(1, failure.size)

            val inaccessible = File(provider.databasePath(2L).toString())
            check(inaccessible.mkdirs())
            provider.publish(2L, failure::add)

            assertNull(provider.current.value)
            assertTrue(inaccessible.isDirectory)
            assertTrue(inaccessible.parentFile.listFiles().orEmpty().none { it.name.contains(".corrupt-") })
            assertEquals(2, failure.size)
        } finally {
            provider.close()
            directory.deleteRecursively()
        }
    }
}
