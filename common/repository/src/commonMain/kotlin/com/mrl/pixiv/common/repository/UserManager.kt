package com.mrl.pixiv.common.repository

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mrl.pixiv.common.coroutine.ProcessLifecycleScope
import com.mrl.pixiv.common.coroutine.launchCatch
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.ProfileImageUrls
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.mmkv.AccountMMKVProperty
import com.mrl.pixiv.common.mmkv.AccountScoped
import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

val requireUserInfoValue
    get() = UserManager.userInfoFlow.value

val requireUserInfoFlow
    get() = UserManager.userInfoFlow

val Long.isSelf: Boolean
    get() = requireUserInfoValue.user.id == this

val User.isSelf: Boolean
    get() = requireUserInfoValue.user.id == id

object UserManager : MMKVUser, AccountScoped {
    private val userInfo = AccountMMKVProperty(
        mmkvSerializable(UserDetailResp()), "userInfo", UserDetailResp(),
    )
    internal val userInfoFlow = userInfo.flow

    override fun reload() = userInfo.reload(AccountStore.activeUserId)

    fun profileForAccount(userId: Long): UserDetailResp? =
        userInfo.read(userId).takeIf { userId > 0 && it.user.id == userId }

    fun restoreProfileForAccount(userId: Long, profile: UserDetailResp) {
        require(userId > 0L && profile.user.id == userId)
        userInfo.write(userId, profile)
    }

    internal fun updateUserInfo(session: AccountSession) {
        userInfo.update(session.userId) {
            it.copy(
                user = it.user.copy(
                    id = session.userId,
                    name = session.name,
                    account = session.account,
                    profileImageUrls = ProfileImageUrls(session.avatarUrl.ifEmpty { it.user.profileImageUrls.medium }),
                ),
                profile = it.profile.copy(isPremium = session.isPremium),
            )
        }
    }

    fun updateUserInfo(
        lifecycleOwner: LifecycleOwner? = null,
        coroutineScope: CoroutineScope? = null,
        userId: Long = AccountStore.activeUserId,
    ): Job {
        val scope = coroutineScope ?: lifecycleOwner?.lifecycleScope ?: ProcessLifecycleScope
        return scope.launchCatch(
            context = Dispatchers.Main.immediate,
            block = { updateUserInfoAsync(userId) },
        )
    }

    suspend fun updateUserInfoAsync(userId: Long = AccountStore.activeUserId) {
        if (userId <= 0) return
        withIOContext {
            val resp = withContext(RequestAccount(userId)) {
                PixivRepository.getUserDetail(userId = userId)
            }
            check(resp.user.id == userId) { "The returned user details do not match the requested account" }
            withAccountStorageLock {
                val account = AccountStore.update(userId) {
                    it.copy(
                        name = resp.user.name,
                        account = resp.user.account,
                        avatarUrl = resp.user.profileImageUrls.medium,
                        isPremium = resp.profile.isPremium,
                    )
                }
                if (account != null) userInfo.write(userId, resp)
            }
        }
    }
}
