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
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
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

data class ConfirmNovelHistoryImportEffect(
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
    private var novelHistoryImportRequestId = 0L
    private val novelHistoryImportResultFlow =
        MutableSharedFlow<NovelHistoryImportResult>(extraBufferCapacity = 1)

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

                val dataV2 = AppExportDataV3(
                    settings = SettingsData(
                        userPreference = SettingRepository.userPreferenceFlow.value,
                    ),
                    search = SearchData(
                        illustSearch = SearchRepository.searchHistoryFlow.value,
                        illustSearchIds = SearchRepository.searchIdHistoryFlow.value.orEmpty(),
                        novelSearch = SearchRepository.novelSearchHistoryFlow.value,
                        novelSearchIds = SearchRepository.novelSearchIdHistoryFlow.value.orEmpty(),
                        savedFilter = SearchRepository.savedSearchFilterValue,
                        rememberFilter = SearchRepository.rememberSearchFilterValue,
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
                )


                val jsonString = json.encodeToString(AppExportDataV3.serializer(), dataV2)
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

                // V2 has grouped top-level keys; support both old/new blocking payload in parseV2ImportData.
                val isV2OrV3Like = "version" in rootObject ||
                        "settings" in rootObject ||
                        "search" in rootObject ||
                        "blocking" in rootObject

                if (isV2OrV3Like) {
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
            savedFilter = data.search.savedFilter,
            rememberFilter = data.search.rememberFilter
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
            savedFilter = data.savedSearchFilter,
            rememberFilter = data.rememberSearchFilter
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
            val confirmed = requestNovelHistoryImportConfirm(
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

    private suspend fun requestNovelHistoryImportConfirm(
        currentUserId: Long,
        importUserId: Long,
    ): Boolean {
        novelHistoryImportRequestId += 1
        val requestId = novelHistoryImportRequestId
        sendEffect(
            ConfirmNovelHistoryImportEffect(
                requestId = requestId,
                currentUserId = currentUserId,
                importUserId = importUserId
            )
        )
        return novelHistoryImportResultFlow
            .filter { it.requestId == requestId }
            .map { it.confirmed }
            .first()
    }

    fun onNovelHistoryImportConfirm(requestId: Long, confirmed: Boolean) {
        novelHistoryImportResultFlow.tryEmit(
            NovelHistoryImportResult(
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
}

private data class NovelHistoryImportResult(
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
