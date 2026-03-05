package com.mrl.pixiv.common.data.mute

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MutedResp(
    @SerialName("for_text")
    val forText: ForText,

    @SerialName("mute_limit_count")
    val muteLimitCount: Long,

    @SerialName("muted_count")
    val mutedCount: Long,

    @SerialName("muted_tags")
    val mutedTags: List<MutedTag>,

    @SerialName("muted_tags_count")
    val mutedTagsCount: Long,

    @SerialName("muted_users")
    val mutedUsers: List<MutedUser>,

    @SerialName("muted_users_count")
    val mutedUsersCount: Long
)

@Serializable
data class ForText(
    @SerialName("mute_limit_count_if_no_premium")
    val muteLimitCountIfNoPremium: Long,

    @SerialName("mute_limit_count_if_premium")
    val muteLimitCountIfPremium: Long
)

@Serializable
data class MutedTag(
    @SerialName("is_premium_slot")
    val isPremiumSlot: Boolean,

    val tag: Tag
)

@Serializable
data class MutedUser(
    @SerialName("is_premium_slot")
    val isPremiumSlot: Boolean,

    val user: User
)