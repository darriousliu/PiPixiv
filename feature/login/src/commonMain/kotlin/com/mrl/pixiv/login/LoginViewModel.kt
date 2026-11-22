package com.mrl.pixiv.login

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.network.NetworkSettings
import com.mrl.pixiv.common.network.withNetworkPreference
import com.mrl.pixiv.common.repository.AccountSessionManager
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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
        val canActivate: () -> Boolean,
    ) : LoginAction()
}

@KoinViewModel
class LoginViewModel(
    private val sessions: AccountSessionManager,
) : BaseMviViewModel<LoginState, LoginAction>(
    initialState = LoginState(),
) {
    override suspend fun handleIntent(intent: LoginAction) {
        when (intent) {
            is LoginAction.Login -> login(intent)
        }
    }

    private suspend fun login(intent: LoginAction.Login) {
        if (state.loading || !intent.canActivate()) return
        updateState { copy(loading = true) }
        try {
            recordLoginAttempt {
                val response = withContext(Dispatchers.IO) {
                    withNetworkPreference({ intent.networkPreference }) {
                        AuthManager.login(intent.code, intent.codeVerifier)
                    }
                }
                sessions.addOrUpdate(response, intent.canActivate)
            }
        } finally {
            updateState { copy(loading = false) }
        }
    }
}
