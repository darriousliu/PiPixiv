package com.mrl.pixiv.common.data.discovery

import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CommunityUsersKind { RECOMMENDED, RELATED, FOLLOWERS }

@Serializable
data class CommunityUsersResponse(
    @SerialName("user_previews") val userPreviews: List<CommunityUserPreview> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class CommunityUserPreview(
    val user: User,
    val illusts: List<Illust> = emptyList(),
    @SerialName("is_muted") val isMuted: Boolean = false,
)
