package com.mrl.pixiv.common.data.user

import com.mrl.pixiv.common.data.Restrict

data class UserBookmarksIllustQuery(
    val restrict: Restrict,
    val userId: Long,
    val tag: String? = null,
    val maxBookmarkId: Long? = null,
) {
    fun toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["restrict"] = restrict.value
        map["user_id"] = userId.toString()
        if (!tag.isNullOrEmpty()) {
            map["tag"] = tag
        }
        if (maxBookmarkId != null) {
            map["max_bookmark_id"] = maxBookmarkId.toString()
        }
        return map
    }
}