package com.mrl.pixiv.splash

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mrl.pixiv.common.repository.LegacyBlocks
import com.mrl.pixiv.common.repository.readDevicePreference
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.repository.recovery.MigrationReason
import com.mrl.pixiv.common.util.JvmZipUtil
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StartupMigrationTest {
    @Test
    fun fallbackPreservesOriginalAndKeepsStartupUsable() = withFixture { directory, manifest ->
        MigrationTestRun(directory).useRun { run ->
            run.debug(MigrationDebug(availableBytes = 0))
            assertTrue(run.start())
            assertEquals(MigrationState.Done, run.migration.state.value)
            assertTrue(run.observedBlocks)
            assertEquals(ItemStatus.Pending(MigrationReason.StorageSpace), run.status(MigrationItem.TranslationCache))
            assertEquals(ItemStatus.Done, run.status(MigrationItem.GlobalDatabase))
            assertEquals(ItemStatus.Done, run.status(MigrationItem.BrowsingHistory))
            assertEquals(ItemStatus.Done, run.status(MigrationItem.ReadingProgress))
            assertEquals(ItemStatus.Done, run.status(MigrationItem.ReadLater))
            assertEquals(manifest.files.getValue("pixiv_db").sha256, directory.resolve("pixiv_db.legacy").sha256())
            assertEquals(manifest.userIds.first(), run.accounts.requireCurrent().userId)
            assertEquals(manifest.tableRows.getValue("download"), directory.databaseCount("SELECT COUNT(*) FROM download"))
            assertEquals(manifest.tableRows.getValue("block_illust"), directory.databaseCount("SELECT COUNT(*) FROM block_illust"))
        }
    }

    @Test
    fun failedItemsRetryOnceAfterNextHomeAndOtherItemsFinish() = withFixture { directory, _ ->
        val failed = MigrationDebug(fail = setOf("BrowsingHistory", "LegacyBlocking"))
        MigrationTestRun(directory).useRun { run ->
            run.debug(failed.copy(fail = setOf("BrowsingHistory")))
            assertTrue(run.start())
            assertFailed(run, MigrationItem.BrowsingHistory, 1)
            assertEquals(ItemStatus.Done, run.status(MigrationItem.ReadingProgress))
            run.debug(null)
            run.migration.maintainAfterHome(run.storage)
            assertFailed(run, MigrationItem.BrowsingHistory, 1)
        }
        MigrationTestRun(directory, migrateBlocks = { error("Legacy blocking unavailable") }).useRun { run ->
            // Force LegacyBlocking to be discovered independently of the previous run.
            run.storage.writeItems(run.storage.readItems() + (MigrationItem.LegacyBlocking to ItemStatus.Pending()))
            assertTrue(run.start())
            assertTrue(run.observedBlocks)
            assertFailed(run, MigrationItem.LegacyBlocking, 1)
            run.migration.maintainAfterHome(run.storage)
            assertEquals(ItemStatus.Done, run.status(MigrationItem.BrowsingHistory))
            assertFailed(run, MigrationItem.LegacyBlocking, 1)
        }
        MigrationTestRun(directory).useRun { run ->
            run.debug(failed)
            assertTrue(run.start())
            run.migration.maintainAfterHome(run.storage)
            assertFailed(run, MigrationItem.LegacyBlocking, 2)
        }
        MigrationTestRun(directory).useRun { run ->
            run.debug(null)
            assertTrue(run.start())
            run.migration.maintainAfterHome(run.storage)
            assertFailed(run, MigrationItem.LegacyBlocking, 2)
        }
    }

    @Test
    fun automaticRetryDoesNotRestoreDeletedAccountHistory() = withFixture { directory, manifest ->
        val userId = manifest.userIds.first()
        MigrationTestRun(directory).useRun { run ->
            run.debug(MigrationDebug(fail = setOf("BrowsingHistory")))
            assertTrue(run.start())
            assertFailed(run, MigrationItem.BrowsingHistory, 1)
            run.preferences.sessions.remove(userId, null, 0L, discardLocalData = true)
            assertTrue(run.files.resolve("accounts/$userId").exists())
        }
        MigrationTestRun(directory).useRun { run ->
            run.debug(null)
            assertTrue(run.start())
            assertFalse(run.files.resolve("accounts/$userId").exists())
            run.migration.maintainAfterHome(run.storage)

            assertEquals(ItemStatus.Done, run.status(MigrationItem.BrowsingHistory))
            assertFalse(run.files.resolve("accounts/$userId").exists())
        }
    }

    @Test
    fun cleanupWaitsForResolutionAndAnotherNormalLaunch() = withFixture { directory, _ ->
        MigrationTestRun(directory).useRun { run ->
            run.debug(MigrationDebug(fail = setOf("GlobalDatabase")))
            assertTrue(run.start())
            run.migration.maintainAfterHome(run.storage)
            assertTrue(directory.resolve("pixiv_db.legacy").isFile)
            assertTrue(directory.resolve("pixiv_db.bak").isFile)
            assertTrue(run.legacy.contains("userRefreshToken"))
        }
        MigrationTestRun(directory).useRun { run ->
            run.debug(null)
            assertTrue(run.start())
            assertEquals(ItemStatus.Done, run.migration.retryItem(run.storage, MigrationItem.TranslationCache, null))
            assertNull(run.migration.maintainAfterHome(run.storage))
            assertTrue(directory.resolve("pixiv_db.legacy").isFile)
        }
        MigrationTestRun(directory).useRun { run ->
            assertTrue(run.start())
            assertNotNull(run.migration.maintainAfterHome(run.storage))
            assertFalse(directory.resolve("pixiv_db.legacy").exists())
            assertFalse(directory.resolve("pixiv_db.bak").exists())
            assertFalse(run.legacy.contains("userRefreshToken"))
        }
    }

    @Test
    fun abandoningBeforeHomeSchedulesCleanupForTheNextSuccessfulLaunch() = withFixture { directory, _ ->
        MigrationTestRun(directory).useRun { run ->
            run.debug(MigrationDebug(fail = setOf("BrowsingHistory")))
            assertTrue(run.start())
            run.migration.abandonAll(run.storage)
            assertEquals(ItemStatus.Abandoned, run.status(MigrationItem.BrowsingHistory))
            assertTrue(directory.resolve("pixiv_db.bak").exists())
        }
        MigrationTestRun(directory).useRun { run ->
            run.debug(null)
            assertTrue(run.start())
            assertNotNull(run.migration.maintainAfterHome(run.storage))
            assertFalse(directory.resolve("pixiv_db.bak").exists())
        }
    }

    @Test
    fun normalCleanupDropsAccountTablesOnlyAfterAnotherNormalLaunch() = withFixture { directory, _ ->
        MigrationTestRun(directory).useRun { run ->
            assertTrue(run.start())
            assertNull(run.migration.maintainAfterHome(run.storage))
            assertEquals(1L, directory.databaseCount("SELECT COUNT(*) FROM sqlite_master WHERE name = 'novel_read_later'"))
        }
        MigrationTestRun(directory).useRun { run ->
            assertTrue(run.start())
            assertNotNull(run.migration.maintainAfterHome(run.storage))
            assertEquals(0L, directory.databaseCount("SELECT COUNT(*) FROM sqlite_master WHERE name = 'novel_read_later'"))
        }
    }

    @Test
    fun legacyPackageRoundTripOmitsTokensAndRemapsOnlySelectedAccount() = withFixture { directory, manifest ->
        MigrationTestRun(directory).useRun { run ->
            assertTrue(run.start())
            val archive = directory.resolve("legacy-export.zip")
            run.recovery.exportLegacyData(run.storage, PlatformFile(archive)) { LegacyBlocks() }
            val zip = JvmZipUtil()
            val metadata = checkNotNull(zip.getZipEntryContent(archive.path, "pipixiv-legacy/legacy.json", 16 * 1024 * 1024))
                .decodeToString()
            assertTrue(metadata.contains("userPreference"))
            for (field in listOf("userRefreshToken", "userAccessToken", "accessTokenExpiresTime")) {
                assertFalse(metadata.contains(field))
            }
            val session = checkNotNull(run.preferences.sessions.current)
            assertFalse(metadata.contains(session.accessToken))
            assertFalse(metadata.contains(session.refreshToken))
            val targetId = manifest.userIds.max() + 1
            val accountFile = File(run.accounts.databasePath(targetId).toString())
            val selected = run.recovery.importLegacyPackageItems(
                run.storage,
                PlatformFile(archive),
                targetId,
                selectedItems = setOf(MigrationItem.BrowsingHistory),
            ) { error("Importing history must not touch legacy blocks") }
            assertEquals(mapOf(MigrationItem.BrowsingHistory to ItemStatus.Done), selected)
            BundledSQLiteDriver().open(accountFile.path).use { target ->
                assertEquals(0L, target.count("SELECT COUNT(*) FROM novel_reading_progress"))
            }
            repeat(2) {
                run.recovery.importLegacyPackage(run.storage, PlatformFile(archive), targetId) { false }
            }
            BundledSQLiteDriver().open(accountFile.path).use { target ->
                assertEquals(manifest.accountRows.getValue("browsing_history_illust"),
                    target.count("SELECT COUNT(*) FROM browsing_history_illust WHERE userId = $targetId"))
            }
        }
    }

    @Test
    fun corruptLegacySessionDefersAccountPreferencesUntilAnExplicitTargetIsChosen() = withFixture { directory, manifest ->
        MigrationTestRun(directory).useRun { run ->
            assertTrue(run.legacy.kv.set("userInfo", "{".encodeToByteArray()))
            assertTrue(run.start())
            assertNull(run.accounts.current.value)
            assertEquals("DARK", readDevicePreference(run.app, run.legacy).theme)
            assertTrue(run.preferences.sessions.recoverAfterLogin.value)
            assertFailed(run, MigrationItem.Session, 1)
            assertEquals(ItemStatus.Pending(MigrationReason.Corrupted), run.status(MigrationItem.Settings))
            assertEquals(ItemStatus.Pending(MigrationReason.Corrupted), run.status(MigrationItem.SearchAndTags))
            assertEquals(
                ItemStatus.Failed(1, MigrationReason.Corrupted),
                run.migration.retryItem(run.storage, MigrationItem.SearchAndTags, null),
            )
            assertFailed(run, MigrationItem.SearchAndTags, 1)
            assertEquals(ItemStatus.Done, run.migration.retryItem(run.storage, MigrationItem.SearchAndTags, manifest.userIds.first()))
        }
    }

    @Test
    fun completeCheckpointSkipsMigration() = withStorage { directory ->
        MigrationTestRun(directory, debug = false).useRun { run ->
            assertTrue(run.app.kv.set("accountStorageMigrationCheckpoint", 2))
            assertTrue(run.start())
            assertEquals(MigrationItem.entries.associateWith { ItemStatus.Done }, run.migration.items.value)
            assertTrue(run.observedBlocks)
        }
    }

    @Test
    fun releaseIgnoresDebugFile() = withFixture { directory, _ ->
        MigrationTestRun(directory, debug = false).useRun { run ->
            run.debug(MigrationDebug(setOf("Session", "GlobalDatabase"), availableBytes = 0))
            assertTrue(run.start())
            assertEquals(ItemStatus.Done, run.status(MigrationItem.Session))
            assertEquals(ItemStatus.Skipped, run.status(MigrationItem.TranslationCache))
            assertFalse(directory.resolve("pixiv_db.legacy").exists())
        }
    }

    @Test
    fun onlyUnavailableGlobalStorageBlocksStartupAndCanRetry() = withStorage { directory ->
        MigrationTestRun(directory).useRun { run ->
            val file = directory.resolve("pixiv_db")
            assertTrue(file.mkdir())
            assertFalse(run.start())
            assertTrue(run.migration.state.value is MigrationState.Blocked)
            assertTrue(file.delete())
            assertTrue(run.start(retry = true))
            assertEquals(MigrationState.Done, run.migration.state.value)
        }
    }

    @Test
    fun unexpectedExceptionBlocksStartupAndCanRetry() = withStorage { directory ->
        var fail = true
        MigrationTestRun(directory, filesRoot = { files ->
            check(!fail) { "Files directory lookup failed" }
            Path(files.absolutePath)
        }).useRun { run ->
            assertFalse(run.start())
            assertEquals(MigrationState.Blocked(MigrationReason.Unknown), run.migration.state.value)
            assertEquals("blocked", lastMigrationTiming().outcome)

            fail = false
            assertTrue(run.start(retry = true))
            assertEquals(MigrationState.Done, run.migration.state.value)
        }
    }

    @Test
    fun timingLogFailureKeepsStartupComplete() = withStorage { directory ->
        MigrationTestRun(directory).useRun { run ->
            var attemptedLog = false
            val started = run.migration.run(run.storage) {
                attemptedLog = true
                error("Timing writer unavailable")
            }
            assertTrue(attemptedLog)
            assertTrue(started)
            assertEquals(MigrationState.Done, run.migration.state.value)
        }
    }

    @Test
    fun migrationLogHidesPathsAndAccountIds() {
        val database = "/private/db"
        val files = "/private/files"
        val error = IllegalStateException(
            "Cannot read $database/pixiv_db or $files/accounts/815001/account.db " + "x".repeat(400)
        )
        val sanitized = sanitizedMigrationError(error, database, files)
        assertTrue(sanitized.startsWith("IllegalStateException: Cannot read <db>/pixiv_db or <files>/accounts/<id>/account.db"))
        assertEquals(300, sanitized.length)
        assertFalse(sanitized.contains("815001"))
        assertFalse(sanitized.contains("/private/"))
    }

    private fun MigrationTestRun.status(item: MigrationItem) = migration.items.value.getValue(item)

    private fun assertFailed(run: MigrationTestRun, item: MigrationItem, attempts: Int) {
        val status = run.status(item) as ItemStatus.Failed
        assertEquals(attempts, status.attempts)
    }

    private fun File.databaseCount(sql: String) =
        BundledSQLiteDriver().open(resolve("pixiv_db").absolutePath).use { it.count(sql) }

    private fun withFixture(block: suspend (File, FixtureManifest) -> Unit) = withStorage { root ->
        val (fixture, manifest) = LegacyMigrationFixture.export(StartupMigrationBenchmarkTest.fixtureRoot, MigrationTier.Typical)
        block(prepareMigrationRun(root, fixture, "run"), manifest)
    }

    private fun withStorage(block: suspend (File) -> Unit) = runBlocking {
        initializeMigrationTestStorage()
        val directory = createTempDirectory("pixiv-startup-test-").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
