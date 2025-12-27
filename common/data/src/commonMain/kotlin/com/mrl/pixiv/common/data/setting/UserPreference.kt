package com.mrl.pixiv.common.data.setting

import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import kotlinx.serialization.Serializable

@Serializable
data class UserPreference(
    val appLanguage: String? = null,
    val theme: String = SettingTheme.SYSTEM.name,
    val enableBypassSniffing: Boolean = false,
    val isR18Enabled: Boolean = false,
    val imageHost: String = IMAGE_HOST,
    val hasShowBookmarkTip: Boolean = false,
    val downloadSubFolderByUser: Boolean = false,
    val spanCountPortrait: Int = 2,
    val spanCountLandscape: Int = -1,
    val fileNameFormat: String = DEFAULT_FILE_NAME_FORMAT,
) {
    companion object {
        const val TEMPLATE_ILLUST_ID = "{illust_id}"
        const val TEMPLATE_TITLE = "{title}"
        const val TEMPLATE_USER_ID = "{user_id}"
        const val TEMPLATE_USER_NAME = "{user_name}"
        const val TEMPLATE_INDEX = "{index}"

        const val DEFAULT_FILE_NAME_FORMAT = "{illust_id}_p{index}"
    }
}
