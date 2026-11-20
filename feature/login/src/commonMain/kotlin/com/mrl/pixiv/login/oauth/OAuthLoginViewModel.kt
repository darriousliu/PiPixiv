package com.mrl.pixiv.login.oauth

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.network.configurePixivNetworkProxy
import com.mrl.pixiv.common.network.networkSettings
import com.mrl.pixiv.common.network.withNetworkPreference
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.login.PixivCookieAuthHelper
import com.mrl.pixiv.login.recordLoginAttempt
import com.mrl.pixiv.strings.cookies_parse_error
import io.ktor.http.Cookie
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.android.annotation.KoinViewModel

@Stable
data class OAuthLoginState(
    val isLogin: Boolean = false,
    val loading: Boolean = false,
)

sealed class OAuthLoginAction : ViewIntent {
    data class Login(val refreshToken: String) : OAuthLoginAction()
}

@KoinViewModel
class OAuthLoginViewModel : BaseMviViewModel<OAuthLoginState, OAuthLoginAction>(
    initialState = OAuthLoginState(),
) {
    override suspend fun handleIntent(intent: OAuthLoginAction) {
        when (intent) {
            is OAuthLoginAction.Login -> login(intent.refreshToken)
        }
    }

    private fun login(refreshToken: String) {
        launchLogin { AuthManager.login(refreshToken) }
    }

    fun loginWithCookies(cookieString: String) {
        launchLogin(onFailure = { ToastUtil.safeShortToast(RStrings.cookies_parse_error) }) {
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
            withNetworkPreference({ requireUserPreferenceValue.networkSettings() }) { preference ->
                PixivCookieAuthHelper {
                    configurePixivNetworkProxy(preference.bypassSetting)
                }.use { helper -> helper.login(cookies) }
            }
        }
    }

    private fun launchLogin(onFailure: () -> Unit = {}, login: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(loading = true) }
            val loggedIn = recordLoginAttempt(login)
            updateState { copy(isLogin = isLogin || loggedIn, loading = false) }
            if (!loggedIn) onFailure()
        }
    }
}
