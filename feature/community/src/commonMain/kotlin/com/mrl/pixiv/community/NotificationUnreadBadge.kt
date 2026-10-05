package com.mrl.pixiv.community

import androidx.compose.material3.Badge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.CommunityRepository
import com.mrl.pixiv.common.repository.requireUserInfoFlow
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.community_unread
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Place in the notification entry's trailing content. Refreshes only while foregrounded. */
@Composable
fun NotificationUnreadBadge(modifier: Modifier = Modifier) {
    val userInfo by requireUserInfoFlow.collectAsStateWithLifecycle()
    val state by CommunityRepository.unreadState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(userInfo.user.id) {
        val job = scope.launch { CommunityRepository.refreshUnread() }
        onPauseOrDispose { job.cancel() }
    }
    if (state.accountId == userInfo.user.id && state.hasUnread == true) {
        val description = stringResource(RStrings.community_unread)
        Badge(modifier.semantics { contentDescription = description })
    }
}
