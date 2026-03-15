package com.mrl.pixiv.setting.appdata

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.data.search.IllustSearch
import com.mrl.pixiv.common.data.search.LocalSearchFilter
import com.mrl.pixiv.common.data.search.NovelSearch
import com.mrl.pixiv.common.data.setting.UserPreference
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
    val settings: SettingsData,
    val search: SearchData,
    val blocking: BlockingData,
    val bookmarks: BookmarksData,
    val downloads: DownloadsData,
)

@Serializable
data class SettingsData(
    val userPreference: UserPreference,
)

@Serializable
data class SearchData(
    val illustSearch: IllustSearch,
    val illustSearchIds: Set<String>,
    val novelSearch: NovelSearch,
    val novelSearchIds: Set<String>,
    val savedFilter: LocalSearchFilter,
    val rememberFilter: Boolean,
)

@Serializable
data class BlockingData(
    val blockIllusts: Set<String>,
    val blockUsers: Set<String>,
    val blockComments: List<Comment>,
)

@Serializable
data class BookmarksData(
    val bookmarkedTags: List<Tag>,
)

@Serializable
data class DownloadsData(
    val downloads: List<DownloadEntity>,
)