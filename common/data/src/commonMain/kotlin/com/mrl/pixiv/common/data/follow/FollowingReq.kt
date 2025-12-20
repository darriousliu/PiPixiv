package com.mrl.pixiv.common.data.follow

import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Restrict

data class FollowingReq(
    val filter: String = Filter.ANDROID.value,
    val restrict: Restrict = Restrict.PUBLIC,
    val userId: Long,
    val offset: Int? = null,
) {
    fun toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["filter"] = filter
        map["restrict"] = restrict.value
        map["user_id"] = userId.toString()
        offset?.let { map["offset"] = it.toString() }
        return map
    }
}