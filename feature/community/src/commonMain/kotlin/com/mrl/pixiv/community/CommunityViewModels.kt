package com.mrl.pixiv.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.discovery.CommunityUsersKind
import com.mrl.pixiv.common.repository.CommunityRepository
import com.mrl.pixiv.common.repository.paging.CommunityUsersPagingSource
import com.mrl.pixiv.common.repository.paging.NotificationsPagingSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class NotificationViewModel(notificationId: Long?) : ViewModel() {
    private var unreadJob: Job? = null
    val notifications = Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
        NotificationsPagingSource(notificationId, onInitialLoaded = ::refreshUnread)
    }.flow.cachedIn(viewModelScope)

    fun refreshUnread() {
        // A list response may have changed server-side read state after an earlier check.
        unreadJob?.cancel()
        unreadJob = viewModelScope.launch { CommunityRepository.refreshUnread() }
    }
}

@KoinViewModel
class CommunityUsersViewModel(kind: CommunityUsersKind, seedUserId: Long?) : ViewModel() {
    val users = Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
        CommunityUsersPagingSource(kind, seedUserId)
    }.flow.cachedIn(viewModelScope)
}
