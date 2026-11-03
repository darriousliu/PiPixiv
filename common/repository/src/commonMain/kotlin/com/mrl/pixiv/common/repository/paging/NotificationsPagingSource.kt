package com.mrl.pixiv.common.repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mrl.pixiv.common.data.notification.NotificationsResponse
import com.mrl.pixiv.common.data.notification.PixivNotification
import com.mrl.pixiv.common.repository.CommunityRepository
import com.mrl.pixiv.common.repository.communityCursorQuery
import kotlinx.coroutines.CancellationException

class NotificationsPagingSource(
    private val notificationId: Long? = null,
    private val loadInitial: suspend () -> NotificationsResponse = {
        CommunityRepository.getNotifications(notificationId)
    },
    private val loadMore: suspend (Map<String, String>) -> NotificationsResponse = {
        CommunityRepository.getNotificationsNext(notificationId, it)
    },
    private val onInitialLoaded: () -> Unit = {},
) : PagingSource<String, PixivNotification>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, PixivNotification> = try {
        val response = if (params.key == null) loadInitial() else loadMore(
            communityCursorQuery(params.key!!, if (notificationId == null) {
                "v1/notification/list"
            } else "v1/notification/view-more")
        )
        if (params.key == null) onInitialLoaded()
        LoadResult.Page(
            data = response.notifications.distinctBy { it.id },
            prevKey = null,
            nextKey = response.nextUrl?.takeIf { it.isNotBlank() && it != params.key },
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<String, PixivNotification>): String? = null
}
