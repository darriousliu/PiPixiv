package com.mrl.pixiv.splash

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.coroutine.CancelException
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.repository.AuthManager
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.coroutines.CancellationException
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
            SplashAction.IsLoginIntent -> if (migration.run()) routeFromSession()
            SplashAction.RetryMigration -> if (migration.run(retry = true)) routeFromSession()
        }
    }

    private fun refreshAccessToken(userId: Long) {
        launchIO(
            onError = {
                if (it !is CancellationException && it !is CancelException) routeAfterRefresh()
            }
        ) {
            AuthManager.requireUserAccessToken(userId)
            routeAfterRefresh()
        }
    }

    private fun routeAfterRefresh() {
        if (AccountStore.current?.hasUsableCredentials() == true) {
            routeToHome()
        } else {
            routeToLogin()
        }
    }

    private suspend fun routeFromSession() {
        AuthManager.applyStartupDebugOverrides()
        val session = AccountStore.current
        when {
            session == null || !session.hasUsableCredentials() -> routeToLogin()
            session.hasExpired() -> refreshAccessToken(session.userId)
            else -> routeToHome()
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
