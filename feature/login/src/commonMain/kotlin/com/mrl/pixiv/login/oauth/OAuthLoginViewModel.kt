package com.mrl.pixiv.login.oauth

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.serialize.JSON
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.login.PixivCookieAuthHelper
import com.mrl.pixiv.strings.cookies_parse_error
import io.ktor.http.Cookie
import io.ktor.util.date.GMTDate
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
    private val cookieAuthHelper by lazy { PixivCookieAuthHelper() }

    override suspend fun handleIntent(intent: OAuthLoginAction) {
        when (intent) {
            is OAuthLoginAction.Login -> login(intent.refreshToken)
        }
    }

    private fun login(refreshToken: String) {
        launchIO(
            onError = {
                updateState { copy(loading = false) }
            }
        ) {
            updateState { copy(loading = true) }
            AuthManager.login(refreshToken)
            updateState { copy(isLogin = true, loading = false) }
        }
    }

    fun loginWithCookies(cookieString: String) {
        launchIO(
            onError = {
                updateState { copy(loading = false) }
                ToastUtil.safeShortToast(RStrings.cookies_parse_error)
            }
        ) {
            updateState { copy(loading = true) }
            val cookies = JSON.parseToJsonElement(cookieString).jsonArray.map {
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
            cookieAuthHelper.login(cookies)
            cookieAuthHelper.close()
            updateState { copy(isLogin = true, loading = false) }
        }
    }
}