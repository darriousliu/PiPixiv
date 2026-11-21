package com.mrl.pixiv.splash

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.ctrip.flight.mmkv.MMKVCLibLoader
import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.ImageUrls
import com.mrl.pixiv.common.data.MetaPage
import com.mrl.pixiv.common.data.MetaSinglePage
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.ProfileImageUrls
import com.mrl.pixiv.common.data.Series
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.XRestrict
import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.search.SearchHistory
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.data.setting.HistorySettings
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.mmkv.mmkvJson
import com.mrl.pixiv.common.mmkv.mmkvOwner
import com.mrl.pixiv.common.repository.NovelSeriesReadingProgress
import com.mrl.pixiv.common.serialize.JSON
import io.github.vinceglb.filekit.FileKit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.random.Random

private const val generatorVersion = 3
private const val fixtureSeed = 128L
private const val fixtureDate = "2025-08-05T14:12:09+09:00"
private const val timestamp = 1754370729000L
private const val fixtureHost = "https://images.example.invalid"

internal enum class MigrationTier(
    val accounts: Int,
    val history: Int,
    val progress: Int,
    val queue: Int,
    val translations: Int,
    val translationLength: Int,
    val duplicateTranslations: Int,
    val downloads: Int,
    val searches: Int,
    val tags: Int,
    val series: Int,
    val limitMs: Long,
) {
    Typical(1, 2000, 300, 50, 30, 20000, 0, 2000, 200, 100, 200, 300),
    Heavy(3, 10000, 2000, 300, 300, 60000, 90, 10000, 1000, 500, 2000, 2000),
    Extreme(1, 50000, 10000, 1000, 400, 175000, 0, 30000, 1000, 500, 2000, 8000);

    val directory get() = name.lowercase()
    val orphanRows get() = if (this == Heavy) 500 else 0

    fun userIds(syntheticOnly: Boolean): List<Long> {
        val synthetic = List(accounts) { index -> 815001L + index }
        if (syntheticOnly) return synthetic
        val extra = System.getenv("PIPIXIV_MIGRATION_UIDS").orEmpty().split(',').filter { it.isNotBlank() }
            .map { it.trim().toLong().also { uid -> require(uid > 0) } }
        return (extra + synthetic).distinct().take(maxOf(accounts, extra.size))
    }
}

@Serializable
internal data class FixtureManifest(
    val tier: String,
    val generatorVersion: Int,
    val seed: Long,
    val userIds: List<Long>,
    val tableRows: Map<String, Long>,
    val accountRows: Map<String, Long>,
    val uniqueTranslations: Int,
    val mmkvCounts: Map<String, Long>,
    val averageIllustJsonBytes: Long,
    val averageNovelJsonBytes: Long,
    val files: Map<String, FixtureFile>,
)

@Serializable
internal data class FixtureFile(val bytes: Long, val sha256: String)

internal object LegacyMigrationFixture {
    private val manifestJson = Json { prettyPrint = true }
    private val exampleUser = User(
        id = 709001L, name = "Fixture Artist", account = "fixture_artist",
        profileImageUrls = ProfileImageUrls("$fixtureHost/avatar/fixture_artist_170.png"),
        comment = "Original fiction and illustration", isFollowed = true,
    )
    private val tags = List(12) {
        Tag(name = "創作テーマ $it", translatedName = "Original theme $it", addedByUploadedUser = it % 2 == 0)
    }

    fun export(root: File, tier: MigrationTier): Pair<File, FixtureManifest> =
        export(root, tier, tier.directory, corruptSession = false)

    fun exportCorruptSession(root: File): Pair<File, FixtureManifest> =
        export(root, MigrationTier.Typical, "typical-corrupt-session", corruptSession = true)

    fun exportIos(root: File, tier: MigrationTier): Pair<File, FixtureManifest> =
        export(root, tier, "${tier.directory}-ios", corruptSession = false, omitSearchIdHistory = true)

    private fun export(
        root: File,
        tier: MigrationTier,
        name: String,
        corruptSession: Boolean,
        omitSearchIdHistory: Boolean = false,
    ): Pair<File, FixtureManifest> {
        val directory = root.resolve(name)
        val manifestFile = directory.resolve("manifest.json")
        val userIds = tier.userIds(syntheticOnly = corruptSession)
        if (manifestFile.isFile) {
            val existing = runCatching { manifestJson.decodeFromString<FixtureManifest>(manifestFile.readText()) }.getOrNull()
            if (existing != null && existing.generatorVersion == generatorVersion &&
                existing.tier == name && existing.seed == fixtureSeed && existing.userIds == userIds &&
                existing.files.all { (name, info) -> directory.resolve(name).length() == info.bytes }
            ) return directory to existing
        }
        val temporary = root.resolve("$name.tmp")
        temporary.deleteRecursively()
        check(temporary.mkdirs())
        try {
            val figures = generateDatabase(temporary.resolve("pixiv_db"), tier, userIds)
            val counts = generatePreferences(temporary.resolve("mmkv"), tier, userIds, corruptSession, omitSearchIdHistory)
            val files = temporary.walkTopDown().filter { it.isFile }.associate {
                it.relativeTo(temporary).invariantSeparatorsPath to FixtureFile(it.length(), it.sha256())
            }
            val accountRows = mapOf(
                "browsing_history_illust" to tier.history.toLong(),
                "browsing_history_novel" to tier.history.toLong(),
                "novel_reading_progress" to tier.progress.toLong(),
                "novel_read_later" to tier.queue.toLong(),
            )
            val totalRows = accountRows.mapValues { (_, rows) -> rows * userIds.size + tier.orphanRows } +
                mapOf(
                    "novel_translation" to tier.translations.toLong(),
                    "download" to tier.downloads.toLong(),
                    "block_illust" to 300L,
                    "block_novel" to 0L,
                    "block_user" to 0L,
                    "block_tag" to 0L,
                    "block_comment" to 0L,
                )
            val manifest = FixtureManifest(
                name, generatorVersion, fixtureSeed, userIds, totalRows, accountRows,
                tier.translations - tier.duplicateTranslations, counts, figures.first, figures.second, files,
            )
            temporary.resolve("manifest.json").writeText(manifestJson.encodeToString(manifest))
            directory.deleteRecursively()
            check(temporary.renameTo(directory))
            return directory to manifest
        } finally {
            temporary.deleteRecursively()
        }
    }

    private fun generateDatabase(file: File, tier: MigrationTier, ids: List<Long>): Pair<Long, Long> {
        val random = Random(fixtureSeed)
        var illustBytes = 0L
        var novelBytes = 0L
        val driver = BundledSQLiteDriver()
        driver.open(file.absolutePath).use { db ->
            db.execSQL("PRAGMA journal_mode=WAL")
            val schema = checkNotNull(javaClass.getResource("/room2/pixiv-v9.sql")).readText()
            schema.lineSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("--") }
                .forEach(db::execSQL)
            check(db.text("SELECT identity_hash FROM room_master_table WHERE id = 42") == "115367479a96f680e52d2ee7d6dc6421")
            db.execSQL("BEGIN IMMEDIATE")
            for (uid in ids + if (tier.orphanRows > 0) listOf(0L) else emptyList()) {
                val history = if (uid == 0L) tier.orphanRows else tier.history
                val progress = if (uid == 0L) tier.orphanRows else tier.progress
                val queue = if (uid == 0L) tier.orphanRows else tier.queue
                db.prepare("INSERT INTO browsing_history_illust VALUES (?, ?, ?, ?)").use { stmt ->
                    repeat(history) { index ->
                        val id = 123_000_000L + index
                        val json = JSON.encodeToString(illust(id, random))
                        illustBytes += json.encodeToByteArray().size
                        stmt.insert(id, uid, timestamp + index, json)
                    }
                }
                db.prepare("INSERT INTO browsing_history_novel VALUES (?, ?, ?, ?)").use { stmt ->
                    repeat(history) { index ->
                        val id = 24_000_000L + index
                        val json = JSON.encodeToString(novel(id, random))
                        novelBytes += json.encodeToByteArray().size
                        stmt.insert(id, uid, timestamp + index, json)
                    }
                }
                db.prepare("INSERT INTO novel_reading_progress VALUES (?, ?, ?, ?, ?, ?)").use { stmt ->
                    repeat(progress) { i -> stmt.insert(24_000_000L + i, uid, i % 80, i % 120, i * 443, timestamp + i) }
                }
                db.prepare("INSERT INTO novel_read_later VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                    repeat(queue) { i ->
                        stmt.insert(
                            24_000_000L + i, uid, "en", "Queued novel $i", "Chapter notes", exampleUser.name,
                            "$fixtureHost/novel/$i.jpg", "[\"fiction\",\"adventure\"]", timestamp + i, "OPENAI",
                            "fixture-model", "https://api.example.invalid/v1", 0, "", "fixture-config",
                            "fixture-source", "READY", "", 0, null, timestamp + i,
                        )
                    }
                }
            }
            db.prepare("INSERT INTO download VALUES (?, 0, ?, ?, ?, ?, ?, NULL, 0, 1.0, ?, '', ?)").use { stmt ->
                repeat(tier.downloads) { i ->
                    stmt.insert(123_000_000L + i, "Illustration $i", exampleUser.id, exampleUser.name,
                        "$fixtureHost/thumb/$i.jpg", "$fixtureHost/original/$i.jpg", "Downloads/$i.jpg", timestamp + i)
                }
            }
            db.prepare("INSERT INTO block_illust VALUES (?, ?)").use { stmt ->
                repeat(300) { i -> stmt.insert(141_000_000L + i, "Blocked illustration $i") }
            }
            val unique = tier.translations - tier.duplicateTranslations
            db.prepare("INSERT INTO novel_translation VALUES (?, ?, 'en', 'OPENAI', 'fixture-model', 'fixture-config', ?, ?, ?, ?, '', '')").use { stmt ->
                // Use different owners for the older rows and deliberately insert duplicates out of date order.
                repeat(tier.translations) { i ->
                    val duplicate = i >= unique
                    val id = 24_000_000L + i % unique
                    stmt.insert(
                        id, ids[if (duplicate) 1 else 0], "source-$id", randomText(random, tier.translationLength),
                        timestamp + if (duplicate) i.toLong() - tier.translations else i.toLong(),
                        if (duplicate) "Older translation" else "Latest translation",
                    )
                }
            }
            db.execSQL("COMMIT")
            db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        }
        val historyRows = tier.history.toLong() * ids.size + tier.orphanRows
        return illustBytes / historyRows to novelBytes / historyRows
    }

    private fun generatePreferences(
        path: File,
        tier: MigrationTier,
        ids: List<Long>,
        corruptSession: Boolean,
        omitSearchIdHistory: Boolean,
    ): Map<String, Long> {
        check(path.mkdirs())
        val app = mmkvOwner("pixiv.app", path.absolutePath).kv
        val legacy = mmkvOwner("pixiv.user", path.absolutePath).kv
        try {
            val searches = List(tier.searches) { SearchHistory("fixture keyword $it", timestamp + it) }
            check(legacy.set("userRefreshToken", "fixture-refresh-token"))
            check(legacy.set("userAccessToken", "fixture-access-token"))
            check(legacy.set("accessTokenExpiresTime", timestamp))
            val userInfo = if (corruptSession) "{" else {
                mmkvJson.encodeToString(UserDetailResp(user = exampleUser.copy(id = ids.first())))
            }
            check(legacy.set("userInfo", userInfo.encodeToByteArray()))
            check(legacy.set("userPreference", mmkvJson.encodeToString(UserPreference(
                appLanguage = "ja", theme = "DARK", bypassSetting = UserPreference.BypassSetting.Proxy("localhost", 7890),
                isR18Enabled = true, defaultPrivateBookmark = true,
                aiTranslationConfig = AiTranslationConfig(apiKey = "fixture-example-key"),
                spanCountPortrait = 3, appApiProxyHost = "api.example.invalid",
                historySettings = HistorySettings(unlimited = tier == MigrationTier.Extreme),
            )).encodeToByteArray()))
            check(legacy.set("searchHistory", mmkvJson.encodeToString(IllustSearch(searches)).encodeToByteArray()))
            check(legacy.set("novelSearchHistory", mmkvJson.encodeToString(NovelSearch(searches)).encodeToByteArray()))
            if (!omitSearchIdHistory) {
                val idSearches = List(tier.searches) { (123_000_000 + it).toString() }.toSet()
                check(legacy.set("searchIdHistory", idSearches))
                check(legacy.set("novelSearchIdHistory", idSearches))
            }
            check(legacy.set("bookmarkedTagsDelegate", mmkvJson.encodeToString(List(tier.tags) { Tag(name = "Fixture tag $it") }).encodeToByteArray()))
            val series = buildMap {
                for (uid in ids) repeat(tier.series) { i ->
                    put("$uid:${91000 + i}", NovelSeriesReadingProgress(24_000_000L + i, "Fixture novel $i", 0.35f))
                }
            }
            check(app.set("novelSeriesProgress", mmkvJson.encodeToString(series).encodeToByteArray()))
        } finally {
            legacy.close()
            app.close()
        }
        val migratedAccounts = if (corruptSession) 0L else 1L
        val searchIdCount = if (omitSearchIdHistory) 0L else tier.searches.toLong()
        return mapOf(
            "accounts" to migratedAccounts,
            "searchHistory" to tier.searches.toLong(), "novelSearchHistory" to tier.searches.toLong(),
            "searchIdHistory" to searchIdCount, "novelSearchIdHistory" to searchIdCount,
            "bookmarkedTags" to tier.tags.toLong(), "novelSeriesProgress" to tier.series.toLong() * ids.size,
            "deviceTheme" to 1L, "deviceProxy" to 1L, "deviceAiTranslation" to 1L,
            "accountR18" to migratedAccounts, "accountPrivateBookmark" to migratedAccounts,
            "skippedCorrupted" to if (corruptSession) 1L else 0L,
        )
    }

    private fun illust(id: Long, random: Random): Illust {
        val images = urls("illust", id, 0)
        return Illust(
            id, "Original illustration $id", Type.Manga, images, caption = randomText(random, 55),
            user = exampleUser, tags = tags, tools = listOf("CLIP STUDIO PAINT", "Krita"), createDate = fixtureDate,
            pageCount = 2, width = 1600, height = 2500, sanityLevel = 2, series = Series(id / 100, "Illustration series"),
            metaSinglePage = MetaSinglePage(images.original), metaPages = List(2) { MetaPage(urls("illust", id, it)) },
            totalView = id % 5000L, totalBookmarks = id % 120L, isBookmarked = true, visible = true,
            isMuted = false, totalComments = 22, illustAIType = AiType.NotAiGeneratedWork,
        )
    }

    private fun novel(id: Long, random: Random) = Novel(
        id, "Original novel $id", randomText(random, 80), 0, XRestrict.Normal, true, urls("novel", id, 0).copy(original = ""),
        fixtureDate, tags, 10, 20000, exampleUser, Series(id / 100, "Fiction series"),
        true, id % 200, id % 7500, true, 22, false, false, false, AiType.NotAiGeneratedWork,
    )

    private fun urls(kind: String, id: Long, page: Int): ImageUrls {
        val name = "/2025/08/05/14/12/09/${id}_p$page.jpg"
        return ImageUrls("$fixtureHost/square_$kind$name", "$fixtureHost/medium_$kind$name",
            "$fixtureHost/large_$kind$name", "$fixtureHost/original_$kind$name")
    }

    private fun randomText(random: Random, length: Int) = buildString(length) {
        repeat(length) { i ->
            append(when {
                i % 67 == 66 -> '\n'
                i % 19 == 18 -> '\u3002'
                i % 3 == 0 -> random.nextInt(0x3041, 0x3097).toChar()
                else -> random.nextInt(0x4e00, 0x9fa6).toChar()
            })
        }
    }
}

internal fun initializeMigrationTestStorage() {
    migrationTestStorage
}

private val migrationTestStorage by lazy {
    val directory = Files.createTempDirectory("pipixiv-migration-test-runtime").toFile()
    Runtime.getRuntime().addShutdownHook(Thread { directory.deleteRecursively() })
    FileKit.init(filesDir = directory.resolve("files"), cacheDir = directory.resolve("cache"))
    val osName = System.getProperty("os.name")
    val name = when {
        osName == "Mac OS X" -> "libmmkvc.dylib"
        osName.startsWith("Windows") -> "mmkvc.dll"
        else -> "libmmkvc.so"
    }
    val library = directory.resolve(name)
    checkNotNull(LegacyMigrationFixture::class.java.classLoader.getResourceAsStream(name)).use { input ->
        library.outputStream().use(input::copyTo)
    }
    initialize(directory.resolve("default-mmkv").absolutePath, MMKVCLibLoader { library.absolutePath }, MMKVLogLevel.LevelNone)
}

internal fun SQLiteConnection.count(sql: String): Long = prepare(sql).use { stmt ->
    check(stmt.step())
    stmt.getLong(0)
}

internal fun SQLiteConnection.text(sql: String): String = prepare(sql).use { stmt ->
    check(stmt.step())
    stmt.getText(0)
}

private fun SQLiteStatement.insert(vararg values: Any?) {
    values.forEachIndexed { index, value ->
        when (value) {
            null -> bindNull(index + 1)
            is Int -> bindInt(index + 1, value)
            is Long -> bindLong(index + 1, value)
            is String -> bindText(index + 1, value)
            else -> error("Unsupported fixture column type")
        }
    }
    step()
    reset()
}

internal fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val bytes = ByteArray(128 * 1024)
        while (true) {
            val read = input.read(bytes)
            if (read < 0) break
            digest.update(bytes, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
