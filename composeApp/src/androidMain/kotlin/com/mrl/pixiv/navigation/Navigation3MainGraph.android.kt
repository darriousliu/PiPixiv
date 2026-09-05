package com.mrl.pixiv.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.router.DestinationsDeepLink
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.splash.SplashViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal actual fun HandleDeeplink(
    navigationManager: NavigationManager
) {
    val splashViewModel: SplashViewModel =
        koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
    val intent = splashViewModel.intent.collectAsStateWithLifecycle().value
    LaunchedEffect(intent) {
        if (intent != null) {
            val data = intent.data ?: return@LaunchedEffect
            DestinationsDeepLink.findLinks(data.toString())
                .singleOrNull()
                ?.toDestination()
                ?.let(navigationManager::navigate)
        }
    }
}
