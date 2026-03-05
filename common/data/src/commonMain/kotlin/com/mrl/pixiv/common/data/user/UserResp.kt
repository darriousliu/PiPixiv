package com.mrl.pixiv.common.data.user

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class UserIllustsResp(
    val user: User? = null,
    val illusts: List<Illust>,

    @SerialName("next_url")
    val nextURL: String? = null
)

@Serializable
data class UserDetailResp(
    val user: User = User(),
    val profile: Profile = Profile(),

    @SerialName("profile_publicity")
    val profilePublicity: ProfilePublicity? = null,

    val workspace: Workspace? = null
)

@Serializable
data class UserNovelsResp(
    val novels: List<Novel>,

    @SerialName("next_url")
    val nextUrl: JsonElement? = null
)

@Serializable
data class UserBookmarkTagsResp(
    @SerialName("bookmark_tags")
    val bookmarkTags: List<BookmarkTag>,

    @SerialName("next_url")
    val nextUrl: String? = null
)

@Serializable
data class UserFollowingResp(
    @SerialName("user_previews")
    val userPreviews: List<UserPreview>,

    @SerialName("next_url")
    val nextUrl: String? = null
)

@Serializable
@Stable
data class UserPreview(
    val user: User,
    val illusts: List<Illust>,
    val novels: List<Novel>,
    @SerialName("is_muted")
    val isMuted: Boolean
)

@Serializable
data class IllustsWithNextUrl(
    val illusts: List<Illust>,
    @SerialName("next_url")
    val nextUrl: String? = null
)