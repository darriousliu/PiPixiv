package com.mrl.pixiv.login.oauth

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.auth.AuthTokenResp
import com.mrl.pixiv.common.network.configurePixivNetworkProxy
import com.mrl.pixiv.common.network.networkSettings
import com.mrl.pixiv.common.network.withNetworkPreference
import com.mrl.pixiv.common.repository.AccountSessionManager
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import com.mrl.pixiv.login.PixivCookieAuthHelper
import com.mrl.pixiv.login.recordLoginAttempt
import com.mrl.pixiv.strings.cookies_parse_error
import io.ktor.http.Cookie
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.android.annotation.KoinViewModel

@Stable
data class OAuthLoginState(
    val loading: Boolean = false,
)

sealed class OAuthLoginAction : ViewIntent {
    data class Login(val refreshToken: String, val canActivate: () -> Boolean) : OAuthLoginAction()
    data class LoginWithCookies(val cookieString: String, val canActivate: () -> Boolean) : OAuthLoginAction()
}

@KoinViewModel
class OAuthLoginViewModel(
    private val sessions: AccountSessionManager,
) : BaseMviViewModel<OAuthLoginState, OAuthLoginAction>(
    initialState = OAuthLoginState(),
) {
    override suspend fun handleIntent(intent: OAuthLoginAction) {
        when (intent) {
            is OAuthLoginAction.Login -> login(intent.canActivate) {
                AuthManager.login(intent.refreshToken)
            }
            is OAuthLoginAction.LoginWithCookies -> login(
                intent.canActivate,
                onFailure = { ToastUtil.safeShortToast(RStrings.cookies_parse_error) },
            ) {
                exchangeCookies(intent.cookieString)
            }
        }
    }

    private suspend fun exchangeCookies(cookieString: String): AuthTokenResp {
        val cookies = JSON.parseToJsonElement(cookieString.replace("\\_", "_")).jsonArray.map {
            val obj = it.jsonObject
            Cookie(
                name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                value = obj["value"]?.jsonPrimitive?.content.orEmpty(),
                expires = obj["expirationDate"]?.jsonPrimitive?.doubleOrNull?.let { GMTDate((it * 1000).toLong()) },
                domain = obj["domain"]?.jsonPrimitive?.content.orEmpty(),
                path = obj["path"]?.jsonPrimitive?.content.orEmpty(),
                secure = obj["secure"]?.jsonPrimitive?.booleanOrNull ?: false,
                httpOnly = obj["httpOnly"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }
        return withNetworkPreference({ requireUserPreferenceValue.networkSettings() }) { preference ->
            PixivCookieAuthHelper {
                configurePixivNetworkProxy(preference.bypassSetting)
            }.use { helper -> helper.login(cookies) }
        }
    }

    private suspend fun login(
        canActivate: () -> Boolean,
        onFailure: () -> Unit = {},
        exchange: suspend () -> AuthTokenResp,
    ) {
        if (state.loading || !canActivate()) return
        updateState { copy(loading = true) }
        try {
            val loggedIn = recordLoginAttempt {
                val response = withContext(Dispatchers.IO) { exchange() }
                sessions.addOrUpdate(response, canActivate)
            }
            if (!loggedIn && canActivate()) onFailure()
        } finally {
            updateState { copy(loading = false) }
        }
    }
}
