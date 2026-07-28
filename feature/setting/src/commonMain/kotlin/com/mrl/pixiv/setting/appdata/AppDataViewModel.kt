package com.mrl.pixiv.setting.appdata

import androidx.annotation.IntRange
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.datasource.local.PixivDatabase
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.datasource.local.entity.NovelReadingProgressEntity
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserInfoValue
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.ZipUtil
import com.mrl.pixiv.common.util.adaptiveFileSize1
import com.mrl.pixiv.common.util.calculateSize
import com.mrl.pixiv.common.util.deleteRecursively
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.strings.cache_cleared
import com.mrl.pixiv.strings.export_failed
import com.mrl.pixiv.strings.export_success
import com.mrl.pixiv.strings.exporting
import com.mrl.pixiv.strings.import_failed
import com.mrl.pixiv.strings.import_success
import com.mrl.pixiv.strings.importing
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.StringResource
import org.koin.android.annotation.KoinViewModel

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

private const val jsonDataFile = "data.json"

data class ConfirmHistoryImportEffect(
    val requestId: Long,
    val currentUserId: Long,
    val importUserId: Long,
) : SideEffect

@KoinViewModel
class AppDataViewModel(
    private val database: PixivDatabase,
    private val zipUtil: ZipUtil
) : BaseMviViewModel<AppDataState, ViewIntent>(
    initialState = AppDataState(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private var historyImportRequestId = 0L
    private val historyImportResultFlow =
        MutableSharedFlow<HistoryImportResult>(extraBufferCapacity = 1)
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
        launchIO {
            updateState { copy(isLoading = true, loadingMessage = RStrings.exporting) }
            try {
                val downloadsDeferred = async { database.downloadDao().getAllDownloads().first() }
                val currentUserId = requireUserInfoValue.user.id
                val novelHistoriesDeferred = async {
                    database.novelReadingProgressDao()
                        .getByUserId(currentUserId)
                        .map {
                            NovelHistoryItem(
                                novelId = it.novelId,
                                paragraphIndex = it.paragraphIndex,
                                charIndex = it.charIndex,
                                paragraphHash = it.paragraphHash,
                                updatedAtMillis = it.updatedAtMillis,
                            )
                        }
                }
                val browsingHistoryDeferred = async {
                    val browsingHistoryDao = database.browsingHistoryDao()
                    BrowsingHistoryData(
                        userId = currentUserId,
                        illusts = browsingHistoryDao.getAllIllusts(currentUserId),
                        novels = browsingHistoryDao.getAllNovels(currentUserId),
                    )
                }
                val blockIllustsDeferred = async { database.blockContentDao().getAllIllusts() }
                val blockNovelsDeferred = async { database.blockContentDao().getAllNovels() }
                val blockUsersDeferred = async { database.blockContentDao().getAllUsers() }
                val blockTagsDeferred = async { database.blockContentDao().getAllTags() }
                val blockCommentsDeferred = async { database.blockContentDao().getAllComments() }

                val downloads = downloadsDeferred.await()
                val novelHistories = novelHistoriesDeferred.await()
                val blockIllusts = blockIllustsDeferred.await()
                val blockNovels = blockNovelsDeferred.await()
                val blockUsers = blockUsersDeferred.await()
                val blockTags = blockTagsDeferred.await()
                val blockComments = blockCommentsDeferred.await()
                val browsingHistory = browsingHistoryDeferred.await()

                val dataV3 = AppExportDataV3(
                    settings = SettingsData(
                        userPreference = SettingRepository.userPreferenceFlow.value,
                    ),
                    search = SearchData(
                        illustSearch = SearchRepository.searchHistoryFlow.value,
                        illustSearchIds = SearchRepository.searchIdHistoryFlow.value.orEmpty(),
                        novelSearch = SearchRepository.novelSearchHistoryFlow.value,
                        novelSearchIds = SearchRepository.novelSearchIdHistoryFlow.value.orEmpty(),
                    ),
                    blocking = BlockingDataV2(
                        blockIllusts = blockIllusts,
                        blockNovels = blockNovels,
                        blockUsers = blockUsers,
                        blockTags = blockTags,
                        blockComments = blockComments.map { json.decodeFromString(it.commentJson) },
                    ),
                    bookmarks = BookmarksData(
                        bookmarkedTags = BookmarkedTagRepository.bookmarkedTags.value,
                    ),
                    downloads = DownloadsData(
                        downloads = downloads,
                    ),
                    novelHistory = NovelHistoryData(
                        userId = currentUserId,
                        histories = novelHistories,
                    ),
                    browsingHistory = browsingHistory,
                )


                val jsonString = json.encodeToString(AppExportDataV3.serializer(), dataV3)
                val jsonFile = PlatformFile(FileKit.cacheDir, jsonDataFile)
                jsonFile.writeString(jsonString)

                zipUtil.compress(jsonFile.absolutePath(), file.absolutePath())
                jsonFile.delete()
                ToastUtil.safeShortToast(RStrings.export_success)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtil.safeShortToast(RStrings.export_failed, e.message.orEmpty())
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    fun importData(file: PlatformFile) {
        launchIO {
            updateState { copy(isLoading = true, loadingMessage = RStrings.importing) }
            try {
                val path = file.absolutePath()
                val jsonString = zipUtil.getZipEntryContent(path, jsonDataFile)?.decodeToString()
                    ?: throw Exception("No data.json in zip file")
                val json = Json { ignoreUnknownKeys = true }
                val rootObject = json.parseToJsonElement(jsonString).jsonObject
                historyImportConfirmCache.clear()

                // V2/V3 have grouped top-level keys; support old/new blocking payloads.
                val isGroupedExport = "version" in rootObject ||
                        "settings" in rootObject ||
                        "search" in rootObject ||
                        "blocking" in rootObject

                if (isGroupedExport) {
                    val dataV3 = parseV3ImportData(json, rootObject)
                    importV3Data(dataV3)
                } else {
                    val dataV1 = json.decodeFromJsonElement<AppExportData>(rootObject)
                    importV1Data(dataV1)
                }

                ToastUtil.safeShortToast(RStrings.import_success)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtil.safeShortToast(RStrings.import_failed, e.message.orEmpty())
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
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

    private suspend fun importV3Data(data: AppExportDataV3) {
        // Settings
        SettingRepository.restore(data.settings.userPreference)

        // Search
        SearchRepository.restore(
            illustSearch = data.search.illustSearch,
            searchIds = data.search.illustSearchIds,
            novelSearch = data.search.novelSearch,
            novelSearchIds = data.search.novelSearchIds,
        )

        // Blocking
        BlockingRepositoryV2.restore(
            data.blocking.blockIllusts,
            data.blocking.blockUsers,
            data.blocking.blockComments,
            data.blocking.blockNovels,
            data.blocking.blockTags,
        )

        // Bookmarks
        BookmarkedTagRepository.restore(data.bookmarks.bookmarkedTags)

        // Downloads
        if (data.downloads.downloads.isNotEmpty()) {
            database.downloadDao().insertAll(data.downloads.downloads)
        }

        importNovelHistory(data.novelHistory)
        importBrowsingHistory(data.browsingHistory)
    }

    private suspend fun importV1Data(data: AppExportData) {
        // Settings
        SettingRepository.restore(data.userPreference)

        // Search (V1 doesn't have novel search, use empty defaults)
        SearchRepository.restore(
            illustSearch = data.searchHistory,
            searchIds = data.searchIdHistory,
            novelSearch = NovelSearch(),
            novelSearchIds = emptySet(),
        )

        // Blocking
        BlockingRepositoryV2.restore(
            data.blockIllusts,
            data.blockUsers,
            data.blockComments
        )

        // Bookmarks
        BookmarkedTagRepository.restore(data.bookmarkedTags)

        // Downloads
        if (data.downloads.isNotEmpty()) {
            database.downloadDao().insertAll(data.downloads)
        }
    }

    private suspend fun importNovelHistory(data: NovelHistoryData) {
        if (data.histories.isEmpty()) return

        val currentUserId = requireUserInfoValue.user.id
        if (data.userId > 0L && data.userId != currentUserId) {
            val confirmed = requestHistoryImportConfirm(
                currentUserId = currentUserId,
                importUserId = data.userId
            )
            if (!confirmed) return
        }

        database.novelReadingProgressDao().upsertAll(
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

    private suspend fun importBrowsingHistory(data: BrowsingHistoryData) {
        if (data.illusts.isEmpty() && data.novels.isEmpty()) return

        val currentUserId = requireUserInfoValue.user.id
        if (data.userId > 0L && data.userId != currentUserId) {
            val confirmed = requestHistoryImportConfirm(
                currentUserId = currentUserId,
                importUserId = data.userId
            )
            if (!confirmed) return
        }

        val dao = database.browsingHistoryDao()
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
