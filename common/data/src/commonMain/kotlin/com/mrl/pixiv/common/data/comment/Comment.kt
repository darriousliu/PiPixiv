package com.mrl.pixiv.common.data.comment

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.PixivLocalDateTimeSerializer
import com.mrl.pixiv.common.data.User
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val dateTimeFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}

@Stable
@Serializable
data class Comment(
    val id: Long,
    val comment: String,
    @Serializable(with = PixivLocalDateTimeSerializer::class)
    val date: LocalDateTime,
    val user: User,
    @SerialName("has_replies")
    val hasReplies: Boolean = false,
    val stamp: Stamp? = null,
    @SerialName("parent_comment")
    val parentComment: Comment? = null
) {
    val dateString = date.format(dateTimeFormat)
}

@Stable
@Serializable
data class Stamp(
    @SerialName("stamp_id")
    val stampId: Int,
    @SerialName("stamp_url")
    val stampUrl: String
)

@Stable
@Serializable
data class Emoji(
    val id: Int,
    val slug: String,
    @SerialName("image_url_medium")
    val imageUrlMedium: String
)


