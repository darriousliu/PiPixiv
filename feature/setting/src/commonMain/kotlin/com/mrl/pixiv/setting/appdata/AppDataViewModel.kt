package com.mrl.pixiv.setting.appdata

import androidx.annotation.IntRange
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.AccountHandle
import com.mrl.pixiv.common.datasource.local.AppDatabaseBackup
import com.mrl.pixiv.common.datasource.local.PixivDatabase
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity
import com.mrl.pixiv.common.datasource.local.globalDatabasePath
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.repository.NovelReadLaterRepository
import com.mrl.pixiv.common.repository.NovelSeriesProgressRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.UserManager
import com.mrl.pixiv.common.repository.recovery.DataRecoveryRepository
import com.mrl.pixiv.common.util.BackupSpaceException
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.ZipEntryTooLargeException
import com.mrl.pixiv.common.util.ZipUtil
import com.mrl.pixiv.common.util.adaptiveFileSize1
import com.mrl.pixiv.common.util.calculateSize
import com.mrl.pixiv.common.util.deleteRecursively
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.strings.backup_export_insufficient_space
import com.mrl.pixiv.strings.backup_import_file_too_large
import com.mrl.pixiv.strings.backup_import_insufficient_space
import com.mrl.pixiv.strings.backup_import_translation_incomplete
import com.mrl.pixiv.strings.cache_cleared
import com.mrl.pixiv.strings.export_failed
import com.mrl.pixiv.strings.export_success
import com.mrl.pixiv.strings.exporting
import com.mrl.pixiv.strings.import_failed
import com.mrl.pixiv.strings.import_success
import com.mrl.pixiv.strings.importing
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.list
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.StringResource
import org.koin.android.annotation.KoinViewModel
import kotlin.uuid.Uuid

@Stable
data class AppDataState(
    val oldImageCount: Int = 0,
    val isMigrating: Boolean = false,
    val progress: Float = 0f,
    @IntRange(from = 0)
    val migratedCount: Int = 0,
    val isLoading: Boolean = false,
    val loadingMessage: StringResource? = null
)

data class ConfirmHistoryImportEffect(
    val requestId: Long,
    val currentUserId: Long,
    val importUserId: Long,
) : SideEffect

@KoinViewModel
class AppDataViewModel(
    private val database: PixivDatabase,
    private val accountDatabaseProvider: AccountDatabaseProvider,
    private val readLaterRepository: NovelReadLaterRepository,
    private val seriesProgressRepository: NovelSeriesProgressRepository,
    dataRecoveryRepository: DataRecoveryRepository,
    zipUtil: ZipUtil,
) : BaseMviViewModel<AppDataState, ViewIntent>(
    initialState = AppDataState(),
) {
    val hasPendingRecovery = dataRecoveryRepository.hasPending

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        exceptionsWithDebugInfo = false
    }
    private val backupFiles = AppBackupFiles(zipUtil)
    private val backupMutex = Mutex()
    private var historyImportRequestId = 0L
    private val historyImportResultFlow = MutableSharedFlow<HistoryImportResult>(replay = 1)
    private val historyImportConfirmCache = mutableMapOf<Long, Boolean>()

    var cacheDirSize by mutableStateOf(0L.adaptiveFileSize1())

    init {
        refreshCacheSize()
        checkOldData()
    }

    override suspend fun handleIntent(intent: ViewIntent) {

    }

    private fun checkOldData() {
        launchIO {
            androidCheckOldData(::updateState)
        }
    }

    fun exportData(file: PlatformFile) {
        runBackupAction(
            loading = RStrings.exporting,
            success = RStrings.export_success,
            failure = RStrings.export_failed,
            spaceFailure = RStrings.backup_export_insufficient_space,
        ) { workDir ->
            val account = accountDatabaseProvider.requireCurrent()
            val userId = account.userId
            val data = withBackupAccount(userId) {
                val history = SearchRepository.historyForAccount(userId)
                AppExportDataV4(
                    userId = userId,
                    settings = SettingsData(SettingRepository.preferenceForAccount(userId)),
                    search = SearchData(
                        illustSearch = history.illustSearch,
                        illustSearchIds = history.illustSearchIds,
                        novelSearch = history.novelSearch,
                        novelSearchIds = history.novelSearchIds,
                        savedFilter = history.savedFilter,
                        rememberFilter = history.rememberFilter,
                    ),
                    bookmarks = BookmarksData(BookmarkedTagRepository.tagsForAccount(userId)),
                    seriesProgress = seriesProgressRepository.exportForAccount(userId),
                    userInfo = UserManager.profileForAccount(userId),
                )
            }

            backupFiles.exportV4(
                metadata = json.encodeToString(AppExportDataV4.serializer(), data),
                global = database,
                account = account,
                globalDatabasePath = globalDatabasePath().toString(),
                accountDatabasePath = accountDatabaseProvider.databasePath(userId).toString(),
                workDir = workDir,
                destination = file,
            )
            null
        }
    }

    fun importData(file: PlatformFile) {
        runBackupAction(
            loading = RStrings.importing,
            success = RStrings.import_success,
            failure = RStrings.import_failed,
            spaceFailure = RStrings.backup_import_insufficient_space,
        ) { workDir ->
            val account = accountDatabaseProvider.requireCurrent()
            historyImportConfirmCache.clear()
            when (val backup = backupFiles.read(file, workDir)) {
                is AppBackupContents.V4 -> {
                    val data = json.decodeFromString<AppExportDataV4>(backup.metadata)
                    require(data.version == 4 && data.userId > 0) { "Unsupported app backup" }
                    if (importV4Data(data, account, backup)) null else RStrings.backup_import_translation_incomplete
                }
                is AppBackupContents.Legacy -> {
                    val rootObject = json.parseToJsonElement(backup.metadata).jsonObject
                    val isGroupedExport = "version" in rootObject ||
                        "settings" in rootObject || "search" in rootObject || "blocking" in rootObject
                    if (isGroupedExport) {
                        importV3Data(parseV3ImportData(json, rootObject), account)
                    } else {
                        importV1Data(json.decodeFromJsonElement<AppExportData>(rootObject), account.userId)
                    }
                    null
                }
            }
        }
    }

    private fun runBackupAction(
        loading: StringResource,
        success: StringResource,
        failure: StringResource,
        spaceFailure: StringResource,
        action: suspend (PlatformFile) -> StringResource?,
    ) {
        launchIO {
            if (!backupMutex.tryLock()) return@launchIO
            var workDir: PlatformFile? = null
            try {
                updateState { copy(isLoading = true, loadingMessage = loading) }
                val directory = FileKit.cacheDir / "app-data-${Uuid.random()}"
                workDir = directory
                directory.createDirectories()
                val warning = action(directory)
                if (warning == null) {
                    ToastUtil.safeShortToast(success)
                } else {
                    ToastUtil.safeShortToast(warning)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: BackupSpaceException) {
                ToastUtil.safeShortToast(spaceFailure)
            } catch (_: ZipEntryTooLargeException) {
                ToastUtil.safeShortToast(RStrings.backup_import_file_too_large)
            } catch (error: Exception) {
                Logger.e(throwable = error, tag = "AppData") { "App data backup operation failed" }
                ToastUtil.safeShortToast(failure, error.message.orEmpty())
            } finally {
                withContext(NonCancellable) {
                    try {
                        workDir?.delete(mustExist = false, recursively = true)
                    } catch (error: Exception) {
                        Logger.e(throwable = error, tag = "AppData") { "Unable to remove temporary backup files" }
                    } finally {
                        updateState { copy(isLoading = false) }
                        backupMutex.unlock()
                    }
                }
            }
        }
    }

    private suspend fun importV4Data(
        data: AppExportDataV4,
        account: AccountHandle,
        backup: AppBackupContents.V4,
    ): Boolean {
        val includeAccountHistory = confirmBackupAccountImport(data.userId, account.userId, ::requestHistoryImportConfirm)
        var translationsComplete = true

        accountDatabaseProvider.withAccountFilesLocked {
            check(AccountStore.find(account.userId) != null) { "The backup account was removed" }
            // Restored tasks must see the restored translation settings when they are claimed.
            readLaterRepository.withQueueClaimsPaused {
                translationsComplete = AppDatabaseBackup.restore(
                    global = database,
                    account = account,
                    globalPath = backup.globalPath,
                    accountPath = backup.accountPath,
                    backupUserId = data.userId,
                    includeAccountData = includeAccountHistory,
                )
                restorePreferences(data.settings, data.search, data.bookmarks, account.userId, restoreLegacySearchFilters = true)
            }
            if (includeAccountHistory) {
                seriesProgressRepository.restoreForAccount(account.userId, data.seriesProgress)
            }
            if (data.userId == account.userId) {
                data.userInfo?.takeIf { it.user.id == account.userId }?.let {
                    UserManager.restoreProfileForAccount(account.userId, it)
                }
            }
        }
        return translationsComplete
    }

    @Suppress("DEPRECATION")
    private fun restorePreferences(
        settings: SettingsData,
        search: SearchData,
        bookmarks: BookmarksData,
        userId: Long,
        restoreLegacySearchFilters: Boolean = false,
    ) = withBackupAccount(userId) {
        SettingRepository.restore(settings.userPreference, userId)
        SearchRepository.restore(
            illustSearch = search.illustSearch,
            searchIds = search.illustSearchIds,
            novelSearch = search.novelSearch,
            novelSearchIds = search.novelSearchIds,
            userId = userId,
            savedFilter = search.savedFilter.takeIf { restoreLegacySearchFilters },
            rememberFilter = search.rememberFilter.takeIf { restoreLegacySearchFilters },
        )
        BookmarkedTagRepository.restore(bookmarks.bookmarkedTags, userId)
    }

    private fun <T> withBackupAccount(userId: Long, block: () -> T): T = withAccountStorageLock {
        check(AccountStore.find(userId) != null) { "The backup account was removed" }
        block()
    }

    private fun parseV3ImportData(json: Json, rootObject: JsonObject): AppExportDataV3 {
        return runCatching {
            json.decodeFromJsonElement<AppExportDataV3>(rootObject)
        }.getOrElse {
            val legacyData = json.decodeFromJsonElement<AppExportDataV2>(rootObject)
            legacyData.toV3()
        }
    }

    private fun AppExportDataV2.toV3(): AppExportDataV3 {
        return AppExportDataV3(
            version = version,
            settings = settings,
            search = search,
            blocking = blocking.toV3(),
            bookmarks = bookmarks,
            downloads = downloads,
            novelHistory = novelHistory,
        )
    }

    private fun BlockingData.toV3(): BlockingDataV2 {
        return BlockingDataV2(
            blockIllusts = blockIllusts
                .mapNotNull { it.toLongOrNull() }
                .distinct()
                .map { BlockIllustEntity(illustId = it) },
            blockUsers = blockUsers
                .mapNotNull { it.toLongOrNull() }
                .distinct()
                .map { BlockUserEntity(userId = it) },
            blockComments = blockComments.distinctBy { it.id },
        )
    }

    private suspend fun importV3Data(data: AppExportDataV3, account: AccountHandle) {
        restorePreferences(data.settings, data.search, data.bookmarks, account.userId)
        BlockingRepositoryV2.restore(
            data.blocking.blockIllusts,
            data.blocking.blockUsers,
            data.blocking.blockComments,
            data.blocking.blockNovels,
            data.blocking.blockTags,
        )

        if (data.downloads.downloads.isNotEmpty()) {
            database.downloadDao().insertAll(data.downloads.downloads)
        }

        importNovelHistory(data.novelHistory, account)
        importBrowsingHistory(data.browsingHistory, account)
    }

    private suspend fun importV1Data(data: AppExportData, userId: Long) {
        withBackupAccount(userId) {
            SettingRepository.restore(data.userPreference, userId)
            SearchRepository.restore(
                illustSearch = data.searchHistory,
                searchIds = data.searchIdHistory,
                novelSearch = NovelSearch(),
                novelSearchIds = emptySet(),
                userId = userId,
            )
            BookmarkedTagRepository.restore(data.bookmarkedTags, userId)
        }

        // Blocking
        BlockingRepositoryV2.restore(
            data.blockIllusts,
            data.blockUsers,
            data.blockComments
        )

        // Downloads
        if (data.downloads.isNotEmpty()) {
            database.downloadDao().insertAll(data.downloads)
        }
    }

    private suspend fun importNovelHistory(data: NovelHistoryData, account: AccountHandle) {
        if (data.histories.isEmpty()) return

        val currentUserId = account.userId
        if (data.userId > 0L && data.userId != currentUserId) {
            val confirmed = requestHistoryImportConfirm(
                currentUserId = currentUserId,
                importUserId = data.userId
            )
            if (!confirmed) return
        }

        account.db.novelReadingProgressDao().upsertAll(
            data.histories.map {
                NovelReadingProgressEntity(
                    novelId = it.novelId,
                    userId = currentUserId,
                    paragraphIndex = it.paragraphIndex,
                    charIndex = it.charIndex,
                    paragraphHash = it.paragraphHash,
                    updatedAtMillis = it.updatedAtMillis,
                )
            }
        )
    }

    private suspend fun importBrowsingHistory(data: BrowsingHistoryData, account: AccountHandle) {
        if (data.illusts.isEmpty() && data.novels.isEmpty()) return

        val currentUserId = account.userId
        if (data.userId > 0L && data.userId != currentUserId) {
            val confirmed = requestHistoryImportConfirm(
                currentUserId = currentUserId,
                importUserId = data.userId
            )
            if (!confirmed) return
        }

        val dao = account.db.browsingHistoryDao()
        val illusts = data.illusts
            .map { it.copy(userId = currentUserId) }
            .sortedByDescending { it.viewedAtMillis }
            .distinctBy { it.illustId }
        val novels = data.novels
            .map { it.copy(userId = currentUserId) }
            .sortedByDescending { it.viewedAtMillis }
            .distinctBy { it.novelId }

        if (illusts.isNotEmpty()) {
            dao.upsertIllusts(illusts)
        }
        if (novels.isNotEmpty()) {
            dao.upsertNovels(novels)
        }
    }

    private suspend fun requestHistoryImportConfirm(
        currentUserId: Long,
        importUserId: Long,
    ): Boolean {
        historyImportConfirmCache[importUserId]?.let { return it }

        historyImportRequestId += 1
        val requestId = historyImportRequestId
        sendEffect(
            ConfirmHistoryImportEffect(
                requestId = requestId,
                currentUserId = currentUserId,
                importUserId = importUserId
            )
        )
        return historyImportResultFlow
            .filter { it.requestId == requestId }
            .map { it.confirmed }
            .first()
            .also { historyImportConfirmCache[importUserId] = it }
    }

    fun onHistoryImportConfirm(requestId: Long, confirmed: Boolean) {
        historyImportResultFlow.tryEmit(
            HistoryImportResult(
                requestId = requestId,
                confirmed = confirmed
            )
        )
    }

    fun migrateData() {
        launchIO {
            androidMigrateData(
                updateState = ::updateState,
                sendEffect = ::sendEffect,
                checkOldData = ::checkOldData
            )
        }
    }

    fun refreshCacheSize() {
        launchIO {
            cacheDirSize = FileKit.cacheDir.calculateSize().adaptiveFileSize1()
        }
    }

    fun clearCache() {
        launchIO {
            val dirSize = FileKit.cacheDir.calculateSize().adaptiveFileSize1()
            FileKit.cacheDir.list().forEach {
                it.deleteRecursively()
            }
            ToastUtil.safeShortToast(RStrings.cache_cleared, dirSize)
            refreshCacheSize()
        }
    }
}

private data class HistoryImportResult(
    val requestId: Long,
    val confirmed: Boolean
)

expect fun androidCheckOldData(
    updateState: (AppDataState.() -> AppDataState) -> Unit,
)

expect fun androidMigrateData(
    updateState: (AppDataState.() -> AppDataState) -> Unit,
    sendEffect: (SideEffect) -> Unit = {},
    checkOldData: () -> Unit = {}
)
