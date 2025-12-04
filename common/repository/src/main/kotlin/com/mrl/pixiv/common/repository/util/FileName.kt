package com.mrl.pixiv.common.repository.util

import com.mrl.pixiv.common.data.setting.UserPreference
import com.mrl.pixiv.common.repository.requireUserPreferenceValue

fun generateFileName(
    illustId: Long,
    title: String,
    userId: Long,
    userName: String,
    index: Int
): String {
    val template = requireUserPreferenceValue.fileNameFormat
    return template
        .replace(UserPreference.TEMPLATE_ILLUST_ID, illustId.toString())
        .replace(UserPreference.TEMPLATE_TITLE, title)
        .replace(UserPreference.TEMPLATE_USER_ID, userId.toString())
        .replace(UserPreference.TEMPLATE_USER_NAME, userName)
        .replace(UserPreference.TEMPLATE_INDEX, index.toString())
}