package com.mrl.pixiv.common.data.novel

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Official /v1/novel/poll/answer response. Initial poll comes from the web reader bridge. */
@Serializable
@Immutable
data class NovelPollResponse(
    @SerialName("poll_data") val pollData: NovelPollData,
)

@Serializable
@Immutable
data class NovelPollData(
    val question: String,
    val total: Int,
    val choices: List<NovelPollChoice>,
    @SerialName("selected_id") val selectedId: Int,
)

@Serializable
@Immutable
data class NovelPollChoice(val id: Int, val text: String, val count: Int)

/** Public www.pixiv.net response, deliberately separate from the authenticated App API DTOs. */
@Serializable
@Immutable
data class PublicNovelRecommendations(
    val novels: List<PublicNovelPreview> = emptyList(),
    val nextIds: List<String> = emptyList(),
)

@Serializable
@Immutable
data class PublicNovelPreview(
    val id: String,
    val title: String = "",
    val url: String = "",
    val userId: String = "",
    val userName: String = "",
    val tags: List<String> = emptyList(),
    val textCount: Int = 0,
    val xRestrict: Int = 0,
    val isMasked: Boolean = false,
) {
    val navigableId: Long? get() = id.toLongOrNull()?.takeIf { it > 0 && !isMasked }
}

@Serializable
@Immutable
data class PublicNovelPoll(
    val question: String,
    val total: Int,
    val choices: List<NovelPollChoice>,
    val selectedValue: Int? = null,
)

@Serializable
@Immutable
data class PublicNovelInteractionDetail(val pollData: PublicNovelPoll? = null)
