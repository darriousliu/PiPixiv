package com.mrl.pixiv.splash

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.buildGlobalDatabase
import com.mrl.pixiv.common.logs.AppLogStore
import com.mrl.pixiv.common.mmkv.AccountMMKVOwner
import com.mrl.pixiv.common.mmkv.AccountSessionStorage
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.repository.LegacyAccountPreferencesMigration
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.measureTime

class StartupMigrationBenchmarkTest {
    private val reportJson = Json { prettyPrint = true; encodeDefaults = true }

    @Test
    fun exportFixtures() {
        assumeTrue(enabled || System.getenv("PIPIXIV_MIGRATION_FIXTURES") == "1")
        initializeMigrationTestStorage()
        MigrationTier.entries.forEach { LegacyMigrationFixture.export(fixtureRoot, it) }
    }

    @Test
    fun measureStartupMigration() = runBlocking {
        assumeTrue(enabled)
        val working = createTempDirectory("pixiv-startup-benchmark-").toFile()
        try {
            initializeMigrationTestStorage()
            val results = MigrationTier.entries.map { tier ->
                val (fixture, manifest) = LegacyMigrationFixture.export(fixtureRoot, tier)
                val runs = List(3) { index ->
                    val path = prepareRun(working, fixture, "${tier.directory}-$index")
                    try {
                        val pools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }
                        System.gc()
                        pools.forEach { it.resetPeakUsage() }
                        val poolStart = pools.sumOf { it.usage.used }
                        val timing = MigrationRun(path).useRun { run ->
                            assertNull(run.accounts.current.value)
                            assertTrue(run.migrate(), "${tier.directory} startup")
                            val result = lastTiming()
                            val poolPeak = pools.sumOf { it.peakUsage.used }
                            assertEquals("success", result.outcome)
                            assertEquals(manifest.mmkvCounts, result.mmkvCounts)
                            assertEquals(manifest.userIds.first(), run.accounts.requireCurrent().userId)
                            verifyDatabase(path, manifest)
                            val backup = path.resolve("pixiv_db.bak")
                            assertEquals(manifest.files.getValue("pixiv_db").sha256, backup.sha256())
                            val appPeak = (result.memoryPeakBytes ?: 0) - (result.memoryStartBytes ?: 0)
                            RunMeasurement(result, maxOf(appPeak, poolPeak - poolStart))
                        }
                        val finishedStartup = MigrationRun(path, publishAccount = false).useRun { run ->
                            val previous = lastTiming()
                            val elapsed = measureTime { assertTrue(run.migrate()) }
                            assertEquals(previous, lastTiming(), "Completed startup must skip migration")
                            elapsed.inWholeMicroseconds / 1000.0
                        }
                        timing.copy(completedStartupMs = finishedStartup)
                    } finally {
                        path.deleteRecursively()
                    }
                }
                TierMeasurement(tier.directory, tier.limitMs, manifest.averageIllustJsonBytes, manifest.averageNovelJsonBytes, runs)
            }
            val (fixture, manifest) = LegacyMigrationFixture.export(fixtureRoot, MigrationTier.Heavy)
            val retry = measureResume(working, fixture, manifest)
            val report = BenchmarkReport(results, retry)
            val output = buildDirectory.resolve("reports/migration-benchmark").apply { mkdirs() }
            output.resolve("result.json").writeText(reportJson.encodeToString(report))
            val markdown = report.markdown()
            output.resolve("result.md").writeText(markdown)
            println(markdown)
            assertTrue(
                results.all { result ->
                    result.runs.all { run ->
                        val before = checkNotNull(run.timing.before)
                        val estimatedExtraDisk = 3 * (before.globalBytes + before.walBytes) + 50 * 1024 * 1024
                        run.timing.totalMs <= result.limitMs && run.completedStartupMs <= 5.0 &&
                            run.extraHeapBytes <= 64 * 1024 * 1024 &&
                            run.timing.peakDiskBytes - before.totalBytes <= estimatedExtraDisk
                    }
                },
                "Migration exceeded a threshold in ${output.resolve("result.md")}",
            )
        } finally {
            working.deleteRecursively()
        }
    }

    @Test
    fun insufficientSpaceLeavesDatabaseUnopenedAndCanRetry() = runBlocking {
        val directory = createTempDirectory("pixiv-startup-storage-").toFile()
        try {
            initializeMigrationTestStorage()
            val root = directory.resolve("upgrade").apply { mkdirs() }
            root.resolve("mmkv").mkdirs()
            MigrationRun(root, publishAccount = false).useRun { run ->
                assertFalse(run.migrate(availableBytes = 0))
                assertEquals(MigrationState.Failed(MigrationFailure.StorageSpace), run.migration.state.value)
                assertFalse(root.resolve("pixiv_db").exists())
                assertNull(run.accounts.current.value)
                assertTrue(run.migrate(retry = true))
                assertEquals(MigrationState.Done, run.migration.state.value)
                assertTrue(root.resolve("pixiv_db").isFile)
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun measureResume(root: File, fixture: File, manifest: FixtureManifest): MigrationTiming {
        val path = prepareRun(root, fixture, "retry")
        val blockedFile = path.resolve("files/accounts/${manifest.userIds.max()}/account.db")
        check(blockedFile.mkdirs())
        try {
            MigrationRun(path).useRun { run ->
                assertFalse(run.migrate())
                assertEquals(MigrationState.Failed(MigrationFailure.SplitAccounts), run.migration.state.value)
                assertNull(run.accounts.current.value)
                assertEquals("SplitAccounts", lastTiming().outcome)
            }
            val driver = BundledSQLiteDriver()
            val firstAccount = path.resolve("files/accounts/${manifest.userIds.min()}/account.db")
            driver.open(firstAccount.absolutePath).use { partial ->
                assertTrue(partial.count("SELECT COUNT(*) FROM browsing_history_illust") > 0)
                partial.execSQL("UPDATE browsing_history_illust SET viewedAtMillis = 9000000000000 WHERE illustId = 123000000")
            }
            driver.open(path.resolve("pixiv_db").absolutePath).use { global ->
                assertEquals(manifest.tableRows.getValue("browsing_history_illust"), global.count("SELECT COUNT(*) FROM browsing_history_illust"))
            }
            assertTrue(blockedFile.delete())
            return MigrationRun(path).useRun { run ->
                assertTrue(run.migrate())
                val result = lastTiming()
                assertEquals(manifest.mmkvCounts, result.mmkvCounts)
                verifyDatabase(path, manifest)
                driver.open(firstAccount.absolutePath).use { db ->
                    assertEquals(9000000000000L, db.count("SELECT viewedAtMillis FROM browsing_history_illust WHERE illustId = 123000000"))
                }
                assertEquals(manifest.files.getValue("pixiv_db").sha256, path.resolve("pixiv_db.bak").sha256())
                result
            }
        } finally {
            path.deleteRecursively()
        }
    }

    private fun verifyDatabase(path: File, manifest: FixtureManifest) {
        val driver = BundledSQLiteDriver()
        driver.open(path.resolve("pixiv_db").absolutePath).use { global ->
            assertEquals(10L, global.count("PRAGMA user_version"))
            assertEquals(manifest.uniqueTranslations.toLong(), global.count("SELECT COUNT(*) FROM novel_translation"))
            assertEquals(manifest.uniqueTranslations.toLong(), global.count("SELECT COUNT(*) FROM novel_translation WHERE translatedTitle = 'Latest translation'"))
            assertEquals(manifest.tableRows.getValue("download"), global.count("SELECT COUNT(*) FROM download"))
            assertEquals(manifest.tableRows.getValue("block_illust"), global.count("SELECT COUNT(*) FROM block_illust"))
            for (table in manifest.accountRows.keys) {
                assertEquals(0L, global.count("SELECT COUNT(*) FROM sqlite_master WHERE name = '$table'"))
            }
        }
        assertFalse(path.resolve("files/accounts/0").exists())
        for (uid in manifest.userIds) {
            driver.open(path.resolve("files/accounts/$uid/account.db").absolutePath).use { db ->
                for ((table, expectedRows) in manifest.accountRows) {
                    assertEquals(expectedRows, db.count("SELECT COUNT(*) FROM $table WHERE userId = $uid"), table)
                    assertEquals(expectedRows, db.count("SELECT COUNT(*) FROM $table"), table)
                }
            }
        }
    }

    private fun lastTiming(): MigrationTiming {
        val message = assertNotNull(AppLogStore.state.value.current.lastOrNull { it.message.startsWith(migrationTimingPrefix) }).message
        return reportJson.decodeFromString(message.removePrefix(migrationTimingPrefix))
    }

    private fun prepareRun(root: File, fixture: File, name: String): File =
        root.resolve(name).also { path ->
            check(fixture.copyRecursively(path))
            path.resolve("pixiv_db-wal").createNewFile()
            path.resolve("pixiv_db-shm").createNewFile()
        }

    private class MigrationRun(private val path: File, private val publishAccount: Boolean = true) {
        private val files = path.resolve("files")
        private val accountsRoot = Path(files.resolve("accounts").absolutePath)
        private val databasePath = Path(path.resolve("pixiv_db").absolutePath)
        val accounts = AccountDatabaseProvider { accountsRoot }
        private val database = buildGlobalDatabase(databasePath)
        val migration = StartupMigration(database, accounts)
        private val preferences = path.resolve("mmkv").absolutePath
        private val app = mmkvOwner("pixiv.app", preferences)
        private val legacy = mmkvOwner("pixiv.user", preferences)
        private val openedAccounts = mutableMapOf<Long, AccountMMKVOwner>()

        suspend fun migrate(retry: Boolean = false, availableBytes: Long? = null): Boolean = migration.run(
            MigrationStorage(
                marker = app,
                databasePath = { databasePath },
                accountRoot = { accountsRoot },
                migratePreferences = {
                    LegacyAccountPreferencesMigration.migrate(app, legacy) { uid ->
                        openedAccounts.getOrPut(uid) { AccountMMKVOwner(uid, files.absolutePath) }
                    }
                },
                publish = {
                    if (publishAccount) accounts.publish(AccountSessionStorage(app).activeUserId.takeIf { it > 0 })
                },
                availableBytes = { availableBytes ?: migrationAvailableBytes(it) },
            ),
            retry,
        )

        suspend fun <T> useRun(block: suspend (MigrationRun) -> T): T = try {
            block(this)
        } finally {
            accounts.close()
            database.close()
            openedAccounts.values.forEach { it.kv.close() }
            legacy.kv.close()
            app.kv.close()
        }
    }

    companion object {
        private val enabled get() = System.getenv("PIPIXIV_MIGRATION_BENCHMARK") == "1"
        private val buildDirectory get() = File(requireNotNull(System.getProperty("pipixiv.benchmark.dir")))
        private val fixtureRoot get() = buildDirectory.resolve("migration-fixtures")
    }
}

@Serializable
private data class RunMeasurement(
    val timing: MigrationTiming,
    val extraHeapBytes: Long,
    val completedStartupMs: Double = 0.0,
)

@Serializable
private data class TierMeasurement(
    val tier: String,
    val limitMs: Long,
    val averageIllustJsonBytes: Long,
    val averageNovelJsonBytes: Long,
    val runs: List<RunMeasurement>,
)

@Serializable
private data class BenchmarkReport(val tiers: List<TierMeasurement>, val resumed: MigrationTiming) {
    fun markdown(): String = buildString {
        appendLine("# JVM migration benchmark")
        appendLine()
        appendLine("| Tier | Median ms | Maximum ms | Extra heap MiB | Peak disk MiB | Completed startup ms | Global DB before / after (MiB) | History JSON average bytes (illust / novel) |")
        appendLine("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
        for (tier in tiers) {
            val times = tier.runs.map { it.timing.totalMs }.sorted()
            val first = tier.runs.first().timing
            val globalBefore = checkNotNull(first.before).globalBytes.mib()
            val globalAfter = checkNotNull(first.after).globalBytes.mib()
            appendLine(
                "| ${tier.tier} | ${times[1]} | ${times.last()} | ${tier.runs.maxOf { it.extraHeapBytes }.mib()} |" +
                    " ${tier.runs.maxOf { it.timing.peakDiskBytes }.mib()} | ${tier.runs.maxOf { it.completedStartupMs }} |" +
                    " $globalBefore / $globalAfter | ${tier.averageIllustJsonBytes} / ${tier.averageNovelJsonBytes} |",
            )
        }
        appendLine()
        appendLine("## Steps")
        appendLine()
        appendLine("| Tier | Step | Median ms | Maximum ms | Rows |")
        appendLine("| --- | --- | ---: | ---: | ---: |")
        tiers.forEach { tier ->
            tier.runs.flatMap { it.timing.steps }.groupBy { it.name }.forEach { (name, steps) ->
                val times = steps.map { it.elapsedMs }.sorted()
                appendLine("| ${tier.tier} | $name | ${times[times.size / 2]} | ${times.last()} | ${steps.first().rows} |")
            }
        }
        appendLine()
        appendLine("Resumed heavy migration in ${resumed.totalMs} ms after an interrupted split.")
        appendLine("Peak disk includes the database files, their sidecars and the temporary/final database backups.")
        appendLine("The existing SQLite auto_vacuum mode controls whether dropping legacy tables shrinks pixiv_db.")
    }

    private fun Long.mib(): String = "%.1f".format(java.util.Locale.ROOT, this / 1048576.0)
}
