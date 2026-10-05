package com.mrl.pixiv.common.datasource.remote

import com.mrl.pixiv.common.data.discovery.CommunityUsersResponse
import com.mrl.pixiv.common.data.notification.NotificationsResponse
import com.mrl.pixiv.common.data.notification.UnreadNotificationsResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.QueryMap

/** Contracts verified against the 6.198.0 APK; authentication uses the shared client. */
interface CommunityApi {
    @GET("v1/notification/has-unread-notifications")
    suspend fun getUnreadNotifications(): UnreadNotificationsResponse

    @GET("v1/notification/list")
    suspend fun getNotifications(@Query("limit") limit: Int = 30): NotificationsResponse

    @GET("v1/notification/list")
    suspend fun getNotificationsNext(@QueryMap query: Map<String, String>): NotificationsResponse

    @GET("v1/notification/view-more")
    suspend fun getNotificationGroup(
        @Query("notification_id") notificationId: Long,
        @Query("limit") limit: Int = 30,
    ): NotificationsResponse

    @GET("v1/notification/view-more")
    suspend fun getNotificationGroupNext(@QueryMap query: Map<String, String>): NotificationsResponse

    @GET("v1/user/recommended")
    suspend fun getRecommendedUsers(@Query("filter") filter: String = "for_android"): CommunityUsersResponse

    @GET("v1/user/recommended")
    suspend fun getRecommendedUsersNext(@QueryMap query: Map<String, String>): CommunityUsersResponse

    @GET("v1/user/related")
    suspend fun getRelatedUsers(
        @Query("seed_user_id") seedUserId: Long,
        @Query("filter") filter: String = "for_android",
    ): CommunityUsersResponse

    // The official declaration has no user_id: this is the signed-in account's followers.
    @GET("v1/user/follower")
    suspend fun getMyFollowers(): CommunityUsersResponse

    @GET("v1/user/follower")
    suspend fun getMyFollowersNext(@QueryMap query: Map<String, String>): CommunityUsersResponse
}
