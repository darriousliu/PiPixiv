package com.mrl.pixiv.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RDrawables
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.strings.ic_launcher
import com.mrl.pixiv.strings.sign_in
import com.mrl.pixiv.strings.sign_in_with_cookie
import com.mrl.pixiv.strings.sign_in_with_token
import com.mrl.pixiv.strings.sign_up
import com.mrl.pixiv.strings.webview_runtime_required
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginOptionScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    var availability by remember {
        mutableStateOf(
            if (platform == Platform.Desktop.Windows) null else WebLoginAvailability.Available
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (platform == Platform.Desktop.Windows) availability = null
            availability = checkWebLoginAvailability()
            awaitCancellation()
        }
    }

    LoginOptionContent(
        availability = availability,
        onSignIn = { navigationManager.navigate(Destination.Login(generateWebViewUrl(false))) },
        onSignUp = { navigationManager.navigate(Destination.Login(generateWebViewUrl(true))) },
        onTokenLogin = { navigationManager.navigate(Destination.OAuthLogin) },
        onCookieLogin = { navigationManager.navigate(Destination.WebCookieLogin) },
        onNetworkSettings = navigationManager::navigateToNetworkSettingScreen,
        modifier = modifier,
    )
}

@Composable
internal fun LoginOptionContent(
    availability: WebLoginAvailability?,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onTokenLogin: () -> Unit,
    onCookieLogin: () -> Unit,
    onNetworkSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNetworkSettings,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 75.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = 10f.spaceBy,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(RDrawables.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(75.dp)
                    .clip(CircleShape)
            )
            Button(
                onClick = onSignIn,
                enabled = availability == WebLoginAvailability.Available,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(RStrings.sign_in)
                )
            }
            Button(
                onClick = onSignUp,
                enabled = availability == WebLoginAvailability.Available,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(RStrings.sign_up)
                )
            }
            if (availability == WebLoginAvailability.Unavailable) {
                Text(
                    text = stringResource(RStrings.webview_runtime_required),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            OutlinedButton(
                onClick = onTokenLogin,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(RStrings.sign_in_with_token)
                )
            }
            OutlinedButton(
                onClick = onCookieLogin,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(RStrings.sign_in_with_cookie)
                )
            }
        }
    }
}
