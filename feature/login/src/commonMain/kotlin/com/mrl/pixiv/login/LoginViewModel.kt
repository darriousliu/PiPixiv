package com.mrl.pixiv.login

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.mrl.pixiv.common.network.NetworkSettings
import com.mrl.pixiv.common.network.withNetworkPreference
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@Stable
data class LoginState(
    val loading: Boolean = false,
)

sealed class LoginAction : ViewIntent {
    data class Login(
        val code: String,
        val codeVerifier: String,
        val networkPreference: NetworkSettings,
    ) : LoginAction()
}

sealed class LoginEvent : SideEffect {
    data object NavigateToMain : LoginEvent()
}

@KoinViewModel
class LoginViewModel : BaseMviViewModel<LoginState, LoginAction>(
    initialState = LoginState(),
) {
    override suspend fun handleIntent(intent: LoginAction) {
        when (intent) {
            is LoginAction.Login -> login(intent)
        }
    }

    private fun login(intent: LoginAction.Login) =
        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(loading = true) }
            val loggedIn = recordLoginAttempt {
                withNetworkPreference({ intent.networkPreference }) {
                    AuthManager.login(intent.code, intent.codeVerifier)
                }
            }
            if (loggedIn) {
                sendEffect(LoginEvent.NavigateToMain)
            } else {
                updateState { copy(loading = false) }
            }
        }
}
