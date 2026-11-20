package com.mrl.pixiv.splash

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.datasource.local.AppDatabaseBackup
import com.mrl.pixiv.common.datasource.local.GlobalDatabaseProvider
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import com.mrl.pixiv.common.repository.NovelSeriesReadingProgress
import com.mrl.pixiv.common.repository.recovery.ItemStatus
import com.mrl.pixiv.common.repository.recovery.MigrationItem
import com.mrl.pixiv.common.util.JvmZipUtil
import com.mrl.pixiv.setting.appdata.AppBackupContents
import com.mrl.pixiv.setting.appdata.AppBackupFiles
import com.mrl.pixiv.setting.appdata.BookmarksData
import com.mrl.pixiv.setting.appdata.SearchData
import com.mrl.pixiv.setting.appdata.SettingsData
import com.mrl.pixiv.setting.appdata.confirmBackupAccountImport
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import java.io.File
import java.lang.management.ManagementFactory
import java.util.Locale
import kotlin.concurrent.thread
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

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
        initializeMigrationTestStorage()
        val working = createTempDirectory("pixiv-startup-benchmark-").toFile()
        try {
            val tiers = mutableListOf<TierMeasurement>()
            val backups = mutableListOf<BackupMeasurement>()
            for (tier in MigrationTier.entries) {
                val (fixture, manifest) = LegacyMigrationFixture.export(fixtureRoot, tier)
                val scenarios = if (tier == MigrationTier.Heavy) MigrationScenario.entries else listOf(MigrationScenario.Normal)
                for (scenario in scenarios) {
                    val runs = List(if (tier == MigrationTier.Extreme) 1 else 3) { index ->
                        val path = prepareMigrationRun(working, fixture, "${tier.directory}-$scenario-$index")
                        try {
                            val startup = measureRun(path, manifest, scenario)
                            if (index == 0 && scenario == MigrationScenario.Normal && tier != MigrationTier.Typical) {
                                backups += measureBackup(path, manifest, tier)
                            }
                            startup
                        } finally {
                            path.deleteRecursively()
                        }
                    }
                    tiers += TierMeasurement(tier.directory, scenario, tier.limitMs, manifest.averageIllustJsonBytes, manifest.averageNovelJsonBytes, runs)
                }
            }
            val report = BenchmarkReport(tiers, backups)
            val output = buildDirectory.resolve("reports/migration-benchmark").apply { mkdirs() }
            output.resolve("result.json").writeText(reportJson.encodeToString(report))
            val markdown = report.markdown()
            output.resolve("result.md").writeText(markdown)
            println(markdown)
            assertTrue(report.passes, "Migration or backup exceeded a threshold in ${output.resolve("result.md")}")
        } finally {
            working.deleteRecursively()
        }
    }

    private suspend fun measureRun(path: File, manifest: FixtureManifest, scenario: MigrationScenario): RunMeasurement {
        val (timing, migrationResources) = MigrationTestRun(path).useRun { run ->
            run.debug(scenario.debug)
            val measurement = measureResources(path) {
                assertTrue(run.start())
                lastMigrationTiming()
            }
            assertEquals("success", measurement.first.outcome)
            assertEquals(manifest.mmkvCounts, measurement.first.mmkvCounts)
            assertEquals(manifest.userIds.first(), run.accounts.requireCurrent().userId)
            verifyDatabase(path, manifest, scenario != MigrationScenario.Normal)
            val preserved = if (scenario == MigrationScenario.Normal) "pixiv_db.bak" else "pixiv_db.legacy"
            assertEquals(manifest.files.getValue("pixiv_db").sha256, path.resolve(preserved).sha256())
            assertEquals(null, run.migration.maintainAfterHome(run.storage))
            run.debug(null)
            measurement
        }
        val finished = MigrationTestRun(path).useRun { run ->
            val start = TimeSource.Monotonic.markNow()
            assertTrue(run.start())
            val elapsedMs = start.elapsedNow().inWholeMicroseconds / 1000.0
            val finishedTiming = lastMigrationTiming()
            assertEquals("alreadyComplete", finishedTiming.outcome)
            assertEquals(manifest.userIds.first(), run.accounts.requireCurrent().userId)
            assertNotNull(run.reloaded)
            val cleanupMs = run.migration.maintainAfterHome(run.storage)
            Triple(elapsedMs, finishedTiming, cleanupMs)
        }
        return RunMeasurement(timing, migrationResources.extraHeapBytes, finished.first, finished.second.steps, finished.third)
    }

    private suspend fun measureBackup(path: File, manifest: FixtureManifest, tier: MigrationTier): BackupMeasurement =
        MigrationTestRun(path).useRun { run ->
            assertTrue(run.start())
            val global = run.database.requireOpen()
            val account = run.accounts.requireCurrent()
            val targetAccount = run.accounts.open(manifest.userIds.max() + 1)
            val paths = AppBackupFiles(JvmZipUtil())
            val exportWork = path.resolve("export-work").apply { mkdirs() }
            val importWork = path.resolve("import-work").apply { mkdirs() }
            val archive = path.resolve("export.zip")
            val databaseBytes = databaseBytes(path.resolve("pixiv_db")) +
                databaseBytes(File(run.accounts.databasePath(account.userId).toString()))
            val (_, export) = measureResources(exportWork, targetBytes = { archive.length() }) {
                paths.exportV4(
                    backupMetadata(run, account.userId),
                    global, account, run.databasePath.toString(), run.accounts.databasePath(account.userId).toString(),
                    PlatformFile(exportWork), PlatformFile(archive),
                )
            }
            val snapshotSize = exportWork.resolve("pipixiv-backup").allocatedBytes()
            val targetGlobal = path.resolve("restored/pixiv_db")
            val target = File(run.accounts.databasePath(targetAccount.userId).toString())
            val targetProvider = GlobalDatabaseProvider { Path(targetGlobal.absolutePath) }
            val import = try {
                val destinationDatabase = targetProvider.open()
                val importBase = databaseBytes(targetGlobal) + databaseBytes(target)
                val (_, resources) = measureResources(importWork, targetBytes = {
                    (databaseBytes(targetGlobal) + databaseBytes(target) - importBase).coerceAtLeast(0L)
                }) {
                    val contents = paths.read(PlatformFile(archive), PlatformFile(importWork)) as AppBackupContents.V4
                    val importedUid = reportJson.parseToJsonElement(contents.metadata)
                        .jsonObject.getValue("userId").jsonPrimitive.long
                    val include = confirmBackupAccountImport(importedUid, targetAccount.userId) { current, source ->
                        assertEquals(targetAccount.userId, current)
                        assertEquals(account.userId, source)
                        true
                    }
                    assertTrue(AppDatabaseBackup.restore(
                        destinationDatabase, targetAccount, contents.globalPath, contents.accountPath, account.userId, include
                    ))
                }
                resources
            } finally {
                targetProvider.close()
            }
            BundledSQLiteDriver().open(targetGlobal.absolutePath).use { restored ->
                assertEquals(manifest.uniqueTranslations.toLong(), restored.count("SELECT COUNT(*) FROM novel_translation"))
            }
            BundledSQLiteDriver().open(target.absolutePath).use { restored ->
                assertEquals(manifest.accountRows.getValue("browsing_history_illust"),
                    restored.count("SELECT COUNT(*) FROM browsing_history_illust WHERE userId = ${targetAccount.userId}"))
            }
            BackupMeasurement(
                tier.directory, export, import, archive.length(), snapshotSize,
                databaseBytes * 5 / 2 + diskMargin, archive.length() + 3 * snapshotSize + diskMargin,
                if (tier == MigrationTier.Heavy) 3000 else 10000,
            )
        }

    private fun backupMetadata(run: MigrationTestRun, userId: Long): String {
        val owner = run.accountPreferences(userId)
        val settings = checkNotNull(run.reloaded)
        val search = SearchData(
            illustSearch = owner.mmkvSerializable(IllustSearch()).read(owner, "searchHistory"),
            illustSearchIds = owner.mmkvStringSet().read(owner, "searchIdHistory").orEmpty(),
            novelSearch = owner.mmkvSerializable(NovelSearch()).read(owner, "novelSearchHistory"),
            novelSearchIds = owner.mmkvStringSet().read(owner, "novelSearchIdHistory").orEmpty(),
        )
        val tags = owner.mmkvSerializable(emptyList<Tag>()).read(owner, "bookmarkedTagsDelegate")
        val progress = owner.mmkvSerializable(emptyMap<Long, NovelSeriesReadingProgress>())
            .read(owner, "novelSeriesProgress")
        val profile = owner.mmkvSerializable<UserDetailResp?>(null).read(owner, "userInfo")
        return buildJsonObject {
            put("version", 4)
            put("userId", userId)
            put("settings", reportJson.encodeToJsonElement(SettingsData(settings)))
            put("search", reportJson.encodeToJsonElement(search))
            put("bookmarks", reportJson.encodeToJsonElement(BookmarksData(tags)))
            put("seriesProgress", reportJson.encodeToJsonElement(progress))
            put("userInfo", reportJson.encodeToJsonElement(profile))
        }.toString()
    }

    private fun verifyDatabase(path: File, manifest: FixtureManifest, degraded: Boolean) {
        val driver = BundledSQLiteDriver()
        driver.open(path.resolve("pixiv_db").absolutePath).use { global ->
            assertEquals(10L, global.count("PRAGMA user_version"))
            val translations = if (degraded) 0L else manifest.uniqueTranslations.toLong()
            assertEquals(translations, global.count("SELECT COUNT(*) FROM novel_translation"))
            assertEquals(translations, global.count("SELECT COUNT(*) FROM novel_translation WHERE translatedTitle = 'Latest translation'"))
            assertEquals(manifest.tableRows.getValue("download"), global.count("SELECT COUNT(*) FROM download"))
            assertEquals(manifest.tableRows.getValue("block_illust"), global.count("SELECT COUNT(*) FROM block_illust"))
            if (!degraded) for (table in manifest.accountRows.keys) {
                assertEquals(1L, global.count("SELECT COUNT(*) FROM sqlite_master WHERE name = '$table'"))
            }
        }
        assertFalse(path.resolve("files/accounts/0").exists())
        for (uid in manifest.userIds) {
            driver.open(path.resolve("files/accounts/$uid/account.db").absolutePath).use { database ->
                for ((table, expectedRows) in manifest.accountRows) {
                    assertEquals(expectedRows, database.count("SELECT COUNT(*) FROM $table WHERE userId = $uid"), table)
                    assertEquals(expectedRows, database.count("SELECT COUNT(*) FROM $table"), table)
                }
            }
        }
    }

    companion object {
        private val enabled get() = System.getenv("PIPIXIV_MIGRATION_BENCHMARK") == "1"
        private val buildDirectory get() = File(requireNotNull(System.getProperty("pipixiv.benchmark.dir")))
        internal val fixtureRoot get() = buildDirectory.resolve("migration-fixtures")
    }
}

@Serializable
private enum class MigrationScenario {
    Normal, LowStorage, V10Failure;

    val debug get() = when (this) {
        Normal -> null
        LowStorage -> MigrationDebug(availableBytes = 0)
        V10Failure -> MigrationDebug(fail = setOf(MigrationItem.GlobalDatabase.name))
    }
}

@Serializable
private data class RunMeasurement(
    val timing: MigrationTiming,
    val extraHeapBytes: Long,
    val completedStartupMs: Double,
    val completedSteps: List<MigrationStep>,
    val cleanupMs: Long?,
)

@Serializable
private data class TierMeasurement(
    val tier: String,
    val scenario: MigrationScenario,
    val limitMs: Long,
    val averageIllustJsonBytes: Long,
    val averageNovelJsonBytes: Long,
    val runs: List<RunMeasurement>,
)

@Serializable
private data class ResourceUse(val elapsedMs: Long, val extraHeapBytes: Long, val cachePeak: Long, val targetPeak: Long) {
    val totalPeak get() = cachePeak + targetPeak
}

@Serializable
private data class BackupMeasurement(
    val tier: String,
    val export: ResourceUse,
    val import: ResourceUse,
    val zipBytes: Long,
    val snapshotBytes: Long,
    val exportBudget: Long,
    val importBudget: Long,
    val limitMs: Long,
) {
    val passes get() = export.elapsedMs <= limitMs && import.elapsedMs <= limitMs &&
        export.extraHeapBytes <= heapLimit && import.extraHeapBytes <= heapLimit &&
        export.cachePeak <= exportBudget && import.totalPeak <= importBudget
}

@Serializable
private data class BenchmarkReport(val tiers: List<TierMeasurement>, val backups: List<BackupMeasurement>) {
    val passes get() = tiers.all { tier ->
        tier.runs.all { run ->
            run.timing.totalMs <= tier.limitMs && run.completedStartupMs <= 5.0 && run.extraHeapBytes <= heapLimit
        }
    } && backups.all { it.passes }

    fun markdown(): String = buildString {
        appendLine("# JVM migration benchmark")
        appendLine()
        appendLine("| Tier | Path | Previous ms | Median ms | Max ms | Heap MiB | Peak disk MiB | Max completed ms | Cleanup ms | History JSON bytes (illust / novel) |")
        appendLine("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
        for (tier in tiers) {
            val times = tier.runs.map { it.timing.totalMs }.sorted()
            val previous = if (tier.scenario != MigrationScenario.Normal) "—" else when (tier.tier) {
                "typical" -> "126–142"
                "heavy" -> "~1550 median, 1736–1798 max"
                else -> "3600–3900"
            }
            appendLine(
                "| ${tier.tier} | ${tier.scenario} | $previous | ${times[times.size / 2]} | ${times.last()} |" +
                    " ${tier.runs.maxOf { it.extraHeapBytes }.mib()} | ${tier.runs.maxOf { it.timing.peakDiskBytes }.mib()} |" +
                    " ${"%.3f".format(Locale.ROOT, tier.runs.maxOf { it.completedStartupMs })} |" +
                    " ${tier.runs.mapNotNull { it.cleanupMs }.maxOrNull() ?: "—"} |" +
                    " ${tier.averageIllustJsonBytes} / ${tier.averageNovelJsonBytes} |",
            )
        }
        appendLine()
        appendLine("The earlier 0.4 ms figure only read the checkpoint. Completed startup now opens the global and active account databases and reloads device/account settings. The isolated JVM harness omits publication to the production singleton flows, Koin's series repository, the blocking DB observers and navigation.")
        appendLine()
        appendLine("| Tier / path | Step | Median ms | Max ms | Rows |")
        appendLine("| --- | --- | ---: | ---: | ---: |")
        tiers.forEach { tier ->
            val steps = tier.runs.flatMap { run ->
                run.timing.steps + run.completedSteps.map { it.copy(name = "completed/${it.name}") }
            }
            steps.groupBy { it.name }.forEach { (name, timings) ->
                val times = timings.map { it.elapsedMs }.sorted()
                appendLine("| ${tier.tier} / ${tier.scenario} | $name | ${times[times.size / 2]} | ${times.last()} | ${timings.first().rows} |")
            }
        }
        appendLine()
        for (tier in tiers) {
            val states = tier.runs.map { run -> run.timing.items.entries.joinToString { (item, state) ->
                if (state is ItemStatus.Pending) "$item=Pending(${state.reason})" else "$item=$state"
            } }.distinct()
            appendLine("- ${tier.tier} / ${tier.scenario} — ${states.joinToString(" / ")}")
        }
        appendLine()
        appendLine("## Backup export and import")
        appendLine()
        appendLine("The JVM benchmark measures the V4 archive, JSON serialization/parsing and import into a different account database. Settings from the archive are not published to app repositories.")
        appendLine()
        appendLine("| Tier | Export ms | Import ms | Extra heap MiB (export / import) | Cache peak MiB (export / import) | Target growth MiB (import) | ZIP / snapshot MiB | ZIP % | Disk budget MiB (export / import) |")
        appendLine("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
        backups.forEach { row ->
            val ratio = "%.1f".format(Locale.ROOT, 100.0 * row.zipBytes / row.snapshotBytes)
            appendLine("| ${row.tier} | ${row.export.elapsedMs} | ${row.import.elapsedMs} | ${row.export.extraHeapBytes.mib()} / ${row.import.extraHeapBytes.mib()} |" +
                " ${row.export.cachePeak.mib()} / ${row.import.cachePeak.mib()} | ${row.import.targetPeak.mib()} |" +
                " ${row.zipBytes.mib()} / ${row.snapshotBytes.mib()} | $ratio% | ${row.exportBudget.mib()} / ${row.importBudget.mib()} |")
        }
    }
}

private const val diskMargin = 50L * 1024 * 1024
private const val heapLimit = 64L * 1024 * 1024
private fun Long.mib(): String = "%.1f".format(Locale.ROOT, this / 1048576.0)
private fun File.allocatedBytes() = walkTopDown().filter { it.isFile }.sumOf(File::length)
private fun databaseBytes(file: File) = file.length() + File(file.path + "-wal").length() + File(file.path + "-shm").length()

private suspend fun <T> measureResources(
    directory: File,
    targetBytes: () -> Long = { 0L },
    block: suspend () -> T,
): Pair<T, ResourceUse> {
    val heap = ManagementFactory.getMemoryMXBean()
    System.gc()
    val startHeap = heap.heapMemoryUsage.used
    val startDisk = directory.allocatedBytes()
    val running = AtomicBoolean(true)
    var maxHeap = startHeap
    var maxDisk = 0L
    var maxTarget = 0L
    fun sample() {
        maxHeap = maxOf(maxHeap, heap.heapMemoryUsage.used)
        maxDisk = maxOf(maxDisk, directory.allocatedBytes() - startDisk)
        maxTarget = maxOf(maxTarget, targetBytes())
    }
    val sampler = thread(isDaemon = true, name = "migration-benchmark-sampler") {
        while (running.get()) {
            sample()
            Thread.sleep(20)
        }
    }
    val start = TimeSource.Monotonic.markNow()
    val result: T
    val elapsed: Long
    try {
        result = block()
        elapsed = start.elapsedNow().inWholeMilliseconds
    } finally {
        running.set(false)
        sampler.join()
    }
    sample()
    return result to ResourceUse(elapsed, maxHeap - startHeap, maxDisk, maxTarget)
}
