package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreference(
    val appLanguage: String? = null,
    val theme: String = SettingTheme.SYSTEM.name,
    @Deprecated("use bypassSetting")
    val enableBypassSniffing: Boolean = false,
    val bypassSetting: BypassSetting = if (enableBypassSniffing) BypassSetting.ECH() else BypassSetting.System,
    val isR18Enabled: Boolean = false,
    val imageHost: String = IMAGE_HOST,
    val hasShowBookmarkTip: Boolean = false,
    val downloadSubFolderByUser: Boolean = false,
    val spanCountPortrait: Int = 2,
    val spanCountLandscape: Int = -1,
    val fileNameFormat: String = DEFAULT_FILE_NAME_FORMAT,
    val defaultPrivateBookmark: Boolean = false,
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
) {
    companion object {
        const val TEMPLATE_ILLUST_ID = "{illust_id}"
        const val TEMPLATE_TITLE = "{title}"
        const val TEMPLATE_USER_ID = "{user_id}"
        const val TEMPLATE_USER_NAME = "{user_name}"
        const val TEMPLATE_INDEX = "{index}"

        const val DEFAULT_FILE_NAME_FORMAT = "{illust_id}_p{index}"
    }

    @Serializable
    sealed interface BypassSetting {
        @Serializable
        // 旧版 none 沿用引擎的默认代理选择器，保留存储名称以兼容历史设置与备份。
        @SerialName("none")
        data object System : BypassSetting

        @Serializable
        @SerialName("direct")
        data object Direct : BypassSetting

        @Serializable
        @SerialName("proxy")
        data class Proxy(
            val host: String = "localhost",
            val port: Int = 7890,
            val proxyType: ProxyType = ProxyType.HTTP
        ) : BypassSetting {
            enum class ProxyType {
                HTTP, SOCKS
            }
        }

        @Serializable(with = EchSettingSerializer::class)
        // 保留旧存储类型名称，使历史设置和备份自动升级为 ECH。
        @SerialName("sni")
        data class ECH(
            val url: String = DEFAULT_DOH_URL,
        ) : BypassSetting {
            companion object {
                const val DEFAULT_DOH_URL = "https://223.5.5.5/resolve"
            }
        }
    }
}
