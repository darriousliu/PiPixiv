package com.mrl.pixiv.login

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.login.browser.initKCEF
import io.ktor.util.PlatformUtils
import org.koin.android.annotation.KoinViewModel

@Stable
data class LoginState(
    val loading: Boolean = false,
    val webViewInitialized: Boolean = false
)

sealed class LoginAction : ViewIntent {
    data class Login(val code: String, val codeVerifier: String) : LoginAction()
}

sealed class LoginEvent : SideEffect {
    data object NavigateToMain : LoginEvent()
}

@KoinViewModel
class LoginViewModel : BaseMviViewModel<LoginState, LoginAction>(
    initialState = LoginState(webViewInitialized = !PlatformUtils.IS_JVM || isKCEFInitialized),
) {
    init {
        if (PlatformUtils.IS_JVM && !isKCEFInitialized) {
            launchIO {
                initKCEF(
                    onInit = { updateState { copy(webViewInitialized = false) } },
                    onInitialized = {
                        isKCEFInitialized = true
                        updateState { copy(webViewInitialized = true) }
                    },
                    onError = {
                        updateState { copy(webViewInitialized = false) }
                    }
                )
            }
        }
    }

    override suspend fun handleIntent(intent: LoginAction) {
        when (intent) {
            is LoginAction.Login -> login(intent.code, intent.codeVerifier)
        }
    }

    private fun login(code: String, codeVerifier: String) =
        launchIO {
            updateState { copy(loading = true) }
            AuthManager.login(code, codeVerifier)
            sendEffect(LoginEvent.NavigateToMain)
        }
}