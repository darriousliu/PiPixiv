package com.mrl.pixiv.common.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 应用视图模式枚举
 * 用于区分插画和小说浏览模式
 */
@Serializable
enum class AppViewMode {
    @SerialName("illust")
    ILLUST,

    @SerialName("novel")
    NOVEL
}
