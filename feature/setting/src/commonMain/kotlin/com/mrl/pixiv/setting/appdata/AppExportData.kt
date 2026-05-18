package com.mrl.pixiv.setting.appdata

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.LocalSearchFilter
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockNovelEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.datasource.local.entity.DownloadEntity
import kotlinx.serialization.Serializable

// V1 - Legacy format (deprecated, kept for backward compatibility)
@Serializable
data class AppExportData(
    val userPreference: UserPreference,
    val searchHistory: IllustSearch,
    val searchIdHistory: Set<String>,
    val blockIllusts: Set<String>,
    val blockUsers: Set<String>,
    val blockComments: List<Comment>,
    val bookmarkedTags: List<Tag>,
    val downloads: List<DownloadEntity> = emptyList(),
    val savedSearchFilter: LocalSearchFilter = LocalSearchFilter(),
    val rememberSearchFilter: Boolean = false,
)

// V2 - Categorized data structure
@Serializable
data class AppExportDataV2(
    val version: Int = 2,
    val settings: SettingsData = SettingsData(),
    val search: SearchData = SearchData(),
    val blocking: BlockingData = BlockingData(),
    val bookmarks: BookmarksData = BookmarksData(),
    val downloads: DownloadsData = DownloadsData(),
    val novelHistory: NovelHistoryData = NovelHistoryData(),
)

@Serializable
data class AppExportDataV3(
    val version: Int = 3,
    val settings: SettingsData = SettingsData(),
    val search: SearchData = SearchData(),
    val blocking: BlockingDataV2 = BlockingDataV2(),
    val bookmarks: BookmarksData = BookmarksData(),
    val downloads: DownloadsData = DownloadsData(),
    val novelHistory: NovelHistoryData = NovelHistoryData(),
)

@Serializable
data class SettingsData(
    val userPreference: UserPreference = UserPreference(),
)

@Serializable
data class SearchData(
    val illustSearch: IllustSearch = IllustSearch(),
    val illustSearchIds: Set<String> = emptySet(),
    val novelSearch: NovelSearch = NovelSearch(),
    val novelSearchIds: Set<String> = emptySet(),
    val savedFilter: LocalSearchFilter = LocalSearchFilter(),
    val rememberFilter: Boolean = false,
)

@Serializable
data class BlockingData(
    val blockIllusts: Set<String> = emptySet(),
    val blockUsers: Set<String> = emptySet(),
    val blockComments: List<Comment> = emptyList(),
)

@Serializable
data class BlockingDataV2(
    val blockIllusts: List<BlockIllustEntity> = emptyList(),
    val blockNovels: List<BlockNovelEntity> = emptyList(),
    val blockUsers: List<BlockUserEntity> = emptyList(),
    val blockTags: List<BlockTagEntity> = emptyList(),
    val blockComments: List<Comment> = emptyList(),
)

@Serializable
data class BookmarksData(
    val bookmarkedTags: List<Tag> = emptyList(),
)

@Serializable
data class DownloadsData(
    val downloads: List<DownloadEntity> = emptyList(),
)

@Serializable
data class NovelHistoryData(
    val userId: Long = 0L,
    val histories: List<NovelHistoryItem> = emptyList(),
)

@Serializable
data class NovelHistoryItem(
    val novelId: Long,
    val paragraphIndex: Int,
    val charIndex: Int,
    val paragraphHash: Int,
    val updatedAtMillis: Long,
)
