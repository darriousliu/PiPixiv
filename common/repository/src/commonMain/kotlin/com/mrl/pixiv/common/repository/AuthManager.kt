package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.data.auth.AuthTokenFieldReq
import com.mrl.pixiv.common.data.auth.AuthTokenResp
import com.mrl.pixiv.common.data.auth.GrantType
import com.mrl.pixiv.common.mmkv.AccountSession
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.currentTimeMillis
import com.mrl.pixiv.strings.network_error
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AuthManager {
    private val refreshLocks = MutableStateFlow<Map<Long, Mutex>>(emptyMap())

    val userRefreshToken: String get() = AccountStore.current?.refreshToken.orEmpty()

    suspend fun requireUserAccessToken(force: Boolean = false): String {
        val userId = AccountStore.activeUserId
        return requireUserAccessToken(userId, force)
    }

    /** Resolves the credentials for the account that initiated the request. */
    suspend fun requireUserAccessToken(userId: Long, force: Boolean = false): String {
        if (userId <= 0) return ""
        return withIOContext {
            val lock = refreshLocks.updateAndGet { locks ->
                if (userId in locks) locks else locks + (userId to Mutex())
            }.getValue(userId)
            lock.withLock {
                val session = AccountStore.find(userId)
                    ?.takeIf(AccountSession::hasUsableCredentials)
                    ?: return@withLock ""
                if (!force && !session.hasExpired()) return@withLock session.accessToken
                try {
                    refresh(session)
                } catch (_: InvalidRefreshGrantException) {
                    AccountStore.storage.markNeedsRelogin(session)
                        ?.takeIf { it.hasUsableCredentials() && !it.hasExpired() }
                        ?.accessToken.orEmpty()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    ToastUtil.safeShortToast(RStrings.network_error)
                    ""
                }
            }
        }
    }

    suspend fun login(code: String, codeVerifier: String): AuthTokenResp =
        PixivRepository.login(
            AuthTokenFieldReq(
                grantType = GrantType.AUTHORIZATION_CODE.value,
                code = code,
                codeVerifier = codeVerifier,
                redirectUri = Constants.PIXIV_LOGIN_REDIRECT_URL,
            )
        )

    suspend fun login(refreshToken: String): AuthTokenResp = requestRefresh(refreshToken)

    private suspend fun requestRefresh(refreshToken: String) =
        PixivRepository.refreshToken(
            AuthTokenFieldReq(
                grantType = GrantType.REFRESH_TOKEN.value,
                refreshToken = refreshToken,
            )
        )

    private suspend fun refresh(session: AccountSession): String {
        val updated = requestRefresh(session.refreshToken).toAccountSession()
        return withAccountStorageLock {
            val saved = AccountStore.storage.saveRefreshedTokens(session, updated)
            if (saved != null && saved.hasUsableCredentials() && !saved.hasExpired()) {
                UserManager.updateUserInfo(saved)
                saved.accessToken
            } else {
                ""
            }
        }
    }
}

internal fun AuthTokenResp.toAccountSession(): AccountSession {
    val payload = response ?: this
    val user = requireNotNull(payload.user) { "The login response is missing the account" }
    val userId = user.id.toLongOrNull()
    require(userId != null && userId > 0 && payload.refreshToken.isNotEmpty() && payload.accessToken.isNotEmpty()) {
        "The login response contains incomplete credentials"
    }
    return AccountSession(
        userId = userId,
        name = user.name,
        account = user.account,
        avatarUrl = user.profileImageUrls.px170X170.ifEmpty { user.profileImageUrls.px50X50 },
        isPremium = user.isPremium,
        refreshToken = payload.refreshToken,
        accessToken = payload.accessToken,
        accessTokenExpiresAt = currentTimeMillis() + payload.expiresIn * 1_000,
    )
}
