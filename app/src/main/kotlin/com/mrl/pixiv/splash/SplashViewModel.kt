package com.mrl.pixiv.splash

import android.content.Intent
import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.datasource.local.mmkv.AuthManager
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.annotation.KoinViewModel

@Stable
data class SplashState(
    val isLoading: Boolean = true,
    val startDestination: Destination? = null,
)

sealed class SplashAction : ViewIntent {
    data object IsLoginIntent : SplashAction()
}

@KoinViewModel
class SplashViewModel : BaseMviViewModel<SplashState, SplashAction>(
    initialState = SplashState(),
) {
    val intent: MutableStateFlow<Intent?> = MutableStateFlow(null)

    init {
        dispatch(SplashAction.IsLoginIntent)
    }

    override suspend fun handleIntent(intent: SplashAction) {
        when (intent) {
            is SplashAction.IsLoginIntent -> isLogin()
        }
    }

    private fun refreshAccessToken() {
        launchIO(
            onError = {
                routeToLogin()
            }
        ) {
            AuthManager.requireUserAccessToken()
            if (AuthManager.isLogin) {
                routeToHome()
            }
        }
    }

    private fun isNeedRefreshToken() {
        if (AuthManager.isNeedRefreshToken) {
            refreshAccessToken()
        } else {
            routeToHome()
        }
    }

    private fun isLogin() {
        if (AuthManager.hasTokens) {
            isNeedRefreshToken()
        } else {
            routeToLogin()
        }
    }

    private fun routeToLogin() =
        updateState {
            copy(
                isLoading = false,
                startDestination = Destination.LoginOptionScreen
            )
        }


    private fun routeToHome() =
        updateState {
            copy(
                isLoading = false,
                startDestination = Destination.HomeScreen
            )
        }
}