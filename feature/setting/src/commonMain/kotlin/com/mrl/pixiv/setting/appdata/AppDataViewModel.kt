package com.mrl.pixiv.setting.appdata

import androidx.annotation.IntRange
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.datasource.local.PixivDatabase
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
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

@KoinViewModel
class AppDataViewModel(
    private val database: PixivDatabase,
    private val zipUtil: ZipUtil
) : BaseMviViewModel<AppDataState, ViewIntent>(
    initialState = AppDataState(),
) {
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
                val downloads = database.downloadDao().getAllDownloads().first()

                val dataV2 = AppExportDataV2(
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
                    blocking = BlockingData(
                        blockIllusts = BlockingRepositoryV2.blockIllustsFlow.value ?: emptySet(),
                        blockUsers = BlockingRepositoryV2.blockUsersFlow.value ?: emptySet(),
                        blockComments = BlockingRepositoryV2.blockCommentsFlow.value,
                    ),
                    bookmarks = BookmarksData(
                        bookmarkedTags = BookmarkedTagRepository.bookmarkedTags.value,
                    ),
                    downloads = DownloadsData(
                        downloads = downloads,
                    ),
                )

                val json = Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
                val jsonString = json.encodeToString(AppExportDataV2.serializer(), dataV2)
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

                // Try to detect version and parse accordingly
                val isV2 = jsonString.contains("\"version\"")

                if (isV2) {
                    // Import V2 format
                    val dataV2 = json.decodeFromString<AppExportDataV2>(jsonString)
                    importV2Data(dataV2)
                } else {
                    // Import V1 format (legacy)
                    val dataV1 = json.decodeFromString<AppExportData>(jsonString)
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

    private suspend fun importV2Data(data: AppExportDataV2) {
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
            data.blocking.blockComments
        )

        // Bookmarks
        BookmarkedTagRepository.restore(data.bookmarks.bookmarkedTags)

        // Downloads
        if (data.downloads.downloads.isNotEmpty()) {
            database.downloadDao().insertAll(data.downloads.downloads)
        }
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

expect fun androidCheckOldData(
    updateState: (AppDataState.() -> AppDataState) -> Unit,
)

expect fun androidMigrateData(
    updateState: (AppDataState.() -> AppDataState) -> Unit,
    sendEffect: (SideEffect) -> Unit = {},
    checkOldData: () -> Unit = {}
)
