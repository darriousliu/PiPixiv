package com.mrl.pixiv.common.data.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationsResponse(
    val notifications: List<PixivNotification> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null,
)

@Serializable
data class PixivNotification(
    val id: Long,
    // Keep unknown server types readable instead of failing enum deserialization.
    val type: Int = 0,
    @SerialName("created_datetime") val createdDatetime: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("target_url") val targetUrl: String? = null,
    val content: NotificationContent = NotificationContent(),
    @SerialName("view_more") val viewMore: NotificationViewMore? = null,
)

@Serializable
data class NotificationContent(
    val text: String = "",
    @SerialName("left_icon") val leftIcon: String? = null,
    @SerialName("left_image") val leftImage: String? = null,
    @SerialName("right_icon") val rightIcon: String? = null,
    @SerialName("right_image") val rightImage: String? = null,
)

@Serializable
data class NotificationViewMore(
    val title: String = "",
    @SerialName("unread_exists") val unreadExists: Boolean = false,
)

@Serializable
data class UnreadNotificationsResponse(
    @SerialName("has_unread_notifications") val hasUnreadNotifications: Boolean = false,
    @SerialName("notify_user_ids") val notifyUserIds: List<Long> = emptyList(),
)
