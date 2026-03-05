package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.data.auth.AuthTokenFieldReq
import com.mrl.pixiv.common.data.auth.AuthTokenResp
import com.mrl.pixiv.common.data.auth.GrantType
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.mmkvLong
import com.mrl.pixiv.common.mmkv.mmkvString
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.currentTimeMillis
import com.mrl.pixiv.strings.network_error
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AuthManager : MMKVUser {
    private val tokenRefreshMutex = Mutex()
    var userRefreshToken by mmkvString()
        private set
    private var userAccessToken by mmkvString()
    private var accessTokenExpiresTime: Long by mmkvLong()
    val hasTokens: Boolean
        get() = userRefreshToken.isNotEmpty() && userAccessToken.isNotEmpty()

    val isNeedRefreshToken: Boolean
        get() = accessTokenExpiresTime != 0L && accessTokenExpiresTime <= currentTimeMillis()

    val isLogin: Boolean
        get() = hasTokens && accessTokenExpiresTime > currentTimeMillis()


    suspend fun requireUserAccessToken(force: Boolean = false): String =
        withIOContext {
            tokenRefreshMutex.withLock {
                return@withLock runCatching {
                    if (userAccessToken.isEmpty()) {
                        throw IllegalStateException("User access token is empty")
                    }
                    if (isNeedRefreshToken || force) {
                        val resp = PixivRepository.refreshToken(
                            AuthTokenFieldReq(
                                grantType = GrantType.REFRESH_TOKEN.value,
                                refreshToken = userRefreshToken
                            )
                        )
                        updateUserInfo(resp)
                    }
                    userAccessToken
                }.onFailure {
                    ToastUtil.safeShortToast(RStrings.network_error)
                }.getOrNull().orEmpty()
            }
        }

    suspend fun login(code: String, codeVerifier: String) {
        val resp = PixivRepository.login(
            AuthTokenFieldReq(
                grantType = GrantType.AUTHORIZATION_CODE.value,
                code = code,
                codeVerifier = codeVerifier,
                redirectUri = Constants.PIXIV_LOGIN_REDIRECT_URL,
            )
        )
        updateUserInfo(resp)
    }

    suspend fun login(refreshToken: String) {
        val resp = PixivRepository.refreshToken(
            AuthTokenFieldReq(
                grantType = GrantType.REFRESH_TOKEN.value,
                refreshToken = refreshToken
            )
        )
        updateUserInfo(resp)
    }

    private fun updateUserInfo(resp: AuthTokenResp) {
        userAccessToken = resp.accessToken
        userRefreshToken = resp.refreshToken
        accessTokenExpiresTime = currentTimeMillis() + resp.expiresIn * 1000
        resp.user?.let { user ->
            UserManager.updateUserInfo(
                user.id.toLong(),
                user.name,
                user.account
            )
        }
    }

    fun logout() {
        userAccessToken = ""
        userRefreshToken = ""
        accessTokenExpiresTime = 0L
    }
}