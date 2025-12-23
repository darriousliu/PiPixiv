package com.mrl.pixiv.login.oauth

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
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
}