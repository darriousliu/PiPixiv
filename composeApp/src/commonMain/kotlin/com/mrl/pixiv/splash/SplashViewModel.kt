package com.mrl.pixiv.splash

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.annotation.KoinViewModel

expect class Intent

@Stable
data class SplashState(
    val isLoading: Boolean = true,
    val startDestination: Destination? = null,
)

sealed class SplashAction : ViewIntent {
    data object IsLoginIntent : SplashAction()
    data object RetryMigration : SplashAction()
}

@KoinViewModel
class SplashViewModel(
    private val migration: StartupMigration,
    private val accounts: AccountDatabaseProvider,
) : BaseMviViewModel<SplashState, SplashAction>(
    initialState = SplashState(),
) {
    val migrationState = migration.state

    val keepNativeSplash: Boolean
        get() = uiState.value.isLoading && when (migrationState.value) {
            MigrationState.Slow, is MigrationState.Blocked -> false
            else -> true
        }

    fun markSlowUiShown() = migration.markSlowUiShown()

    val intent: MutableStateFlow<Intent?> = MutableStateFlow(null)

    init {
        dispatch(SplashAction.IsLoginIntent)
    }

    override suspend fun handleIntent(intent: SplashAction) {
        when (intent) {
            SplashAction.IsLoginIntent -> if (migration.run()) isLogin()
            SplashAction.RetryMigration -> if (migration.run(retry = true)) isLogin()
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
            } else {
                routeToLogin()
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

    private fun routeToLogin() {
        accounts.clearCurrent()
        updateState {
            copy(
                isLoading = false,
                startDestination = Destination.LoginOption
            )
        }
    }

    private fun routeToHome() =
        updateState {
            copy(
                isLoading = false,
                startDestination = Destination.Main
            )
        }
}
