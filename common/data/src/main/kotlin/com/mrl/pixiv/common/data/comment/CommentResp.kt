package com.mrl.pixiv.common.data.comment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmojiResp(
    @SerialName("emoji_definitions")
    val emojiDefinitions: List<Emoji>
)

@Serializable
data class StampsResp(
    val stamps: List<Stamp>
)

@Serializable
data class IllustCommentsResp(
    val comments: List<Comment>,
    @SerialName("next_url")
    val nextUrl: String? = null
)

@Serializable
data class CommentAddResp(
    val comment: Comment
)