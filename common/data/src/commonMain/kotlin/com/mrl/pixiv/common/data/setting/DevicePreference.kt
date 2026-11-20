@file:Suppress("DEPRECATION")

package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import kotlinx.serialization.Serializable

@Serializable
data class DevicePreference(
    val appLanguage: String? = null,
    val theme: String = SettingTheme.SYSTEM.name,
    @Deprecated("use bypassSetting")
    val enableBypassSniffing: Boolean = false,
    val bypassSetting: UserPreference.BypassSetting =
        if (enableBypassSniffing) UserPreference.BypassSetting.ECH() else UserPreference.BypassSetting.System,
    val imageHost: String = IMAGE_HOST,
    val hasShowBookmarkTip: Boolean = false,
    val downloadSubFolderByUser: Boolean = false,
    val spanCountPortrait: Int = 2,
    val spanCountLandscape: Int = -1,
    val fileNameFormat: String = UserPreference.DEFAULT_FILE_NAME_FORMAT,
    val readClipboardOnSearch: Boolean = false,
    val appViewMode: AppViewMode = AppViewMode.ILLUST,
    val collectionViewMode: AppViewMode = AppViewMode.ILLUST,
    val historyViewMode: AppViewMode = AppViewMode.ILLUST,
    val readingTab: ReadingTab = ReadingTab.READ_LATER,
    val aiTranslationConfig: AiTranslationConfig = AiTranslationConfig(),
    val browsingSettings: BrowsingSettings = BrowsingSettings(),
    val searchSettings: SearchSettings = SearchSettings(),
    val historySettings: HistorySettings = HistorySettings(),
    val novelReaderSettings: NovelReaderSettings = NovelReaderSettings(),
    val useLiquidGlassNavigation: Boolean = true,
    val appApiProxyHost: String = "",
)

@Serializable
data class AccountPreference(
    val isR18Enabled: Boolean = false,
    val defaultPrivateBookmark: Boolean = false,
)

fun UserPreference.toDevicePreference() = DevicePreference(
    appLanguage = appLanguage,
    theme = theme,
    enableBypassSniffing = enableBypassSniffing,
    bypassSetting = bypassSetting,
    imageHost = imageHost,
    hasShowBookmarkTip = hasShowBookmarkTip,
    downloadSubFolderByUser = downloadSubFolderByUser,
    spanCountPortrait = spanCountPortrait,
    spanCountLandscape = spanCountLandscape,
    fileNameFormat = fileNameFormat,
    readClipboardOnSearch = readClipboardOnSearch,
    appViewMode = appViewMode,
    collectionViewMode = collectionViewMode,
    historyViewMode = historyViewMode,
    readingTab = readingTab,
    aiTranslationConfig = aiTranslationConfig,
    browsingSettings = browsingSettings,
    searchSettings = searchSettings,
    historySettings = historySettings,
    novelReaderSettings = novelReaderSettings,
    useLiquidGlassNavigation = useLiquidGlassNavigation,
    appApiProxyHost = appApiProxyHost,
)

fun UserPreference.toAccountPreference() = AccountPreference(
    isR18Enabled = isR18Enabled,
    defaultPrivateBookmark = defaultPrivateBookmark,
)

fun DevicePreference.toUserPreference(account: AccountPreference) = UserPreference(
    appLanguage = appLanguage,
    theme = theme,
    enableBypassSniffing = enableBypassSniffing,
    bypassSetting = bypassSetting,
    isR18Enabled = account.isR18Enabled,
    imageHost = imageHost,
    hasShowBookmarkTip = hasShowBookmarkTip,
    downloadSubFolderByUser = downloadSubFolderByUser,
    spanCountPortrait = spanCountPortrait,
    spanCountLandscape = spanCountLandscape,
    fileNameFormat = fileNameFormat,
    defaultPrivateBookmark = account.defaultPrivateBookmark,
    readClipboardOnSearch = readClipboardOnSearch,
    appViewMode = appViewMode,
    collectionViewMode = collectionViewMode,
    historyViewMode = historyViewMode,
    readingTab = readingTab,
    aiTranslationConfig = aiTranslationConfig,
    browsingSettings = browsingSettings,
    searchSettings = searchSettings,
    historySettings = historySettings,
    novelReaderSettings = novelReaderSettings,
    useLiquidGlassNavigation = useLiquidGlassNavigation,
    appApiProxyHost = appApiProxyHost,
)
