package com.mrl.pixiv.common.datasource.local.mmkv

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mrl.pixiv.common.coroutine.ProcessLifecycleScope
import com.mrl.pixiv.common.coroutine.launchCatch
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.repository.PixivRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val requireUserInfoValue
    get() = UserManager.userInfoFlow.value

val requireUserInfoFlow
    get() = UserManager.userInfoFlow

val Long.isSelf: Boolean
    get() = requireUserInfoValue.user.id == this

object UserManager : MMKVUser {
    private val userInfo by mmkvSerializable(UserDetailResp()).asMutableStateFlow()
    internal val userInfoFlow = userInfo.asStateFlow()

    internal fun updateUserInfo(
        id: Long,
        name: String,
        account: String,
    ) {
        userInfo.update {
            it.copy(user = it.user.copy(id = id, name = name, account = account))
        }
    }

    fun updateUserInfo(
        lifecycleOwner: LifecycleOwner? = null,
        coroutineScope: CoroutineScope? = null,
    ) {
        val scope = coroutineScope ?: lifecycleOwner?.lifecycleScope ?: ProcessLifecycleScope
        scope.launchCatch(
            context = Dispatchers.Main.immediate,
            error = {

            },
            block = { updateUserInfoAsync() }
        )
    }

    suspend fun updateUserInfoAsync() {
        withIOContext {
            val resp = PixivRepository.getUserDetail(userId = userInfo.value.user.id)
            userInfo.update { resp }
        }
    }

    fun logout() {
        userInfo.value = UserDetailResp()
    }
}