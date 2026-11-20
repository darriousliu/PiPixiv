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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.withAccountStorageLock
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RDrawables
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.ic_launcher
import com.mrl.pixiv.strings.network_setting
import com.mrl.pixiv.strings.sign_in
import com.mrl.pixiv.strings.sign_in_with_cookie
import com.mrl.pixiv.strings.sign_in_with_token
import com.mrl.pixiv.strings.sign_up
import com.mrl.pixiv.strings.switch_to_another_account
import com.mrl.pixiv.strings.webview_runtime_required
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginOptionScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
    addAccount: Boolean = false,
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
    val onSwitchAccount: (() -> Unit)? = if (addAccount) {
        null
    } else {
        val accountChanges by AccountStore.accountChanges.collectAsStateWithLifecycle()
        val hasOtherAccounts = remember(accountChanges) {
            withAccountStorageLock {
                val activeUserId = AccountStore.activeUserId
                AccountStore.sessions.any { it.userId != activeUserId }
            }
        }
        if (hasOtherAccounts) {
            navigationManager::navigateToAccountManagementScreen
        } else {
            null
        }
    }

    LoginOptionContent(
        availability = availability,
        onSignIn = {
            navigationManager.navigate(Destination.Login(generateWebViewUrl(false), addAccount))
        },
        onSignUp = {
            navigationManager.navigate(Destination.Login(generateWebViewUrl(true), addAccount))
        },
        onTokenLogin = { navigationManager.navigate(Destination.OAuthLogin) },
        onCookieLogin = { navigationManager.navigate(Destination.WebCookieLogin) },
        onNetworkSettings = navigationManager::navigateToNetworkSettingScreen,
        modifier = modifier,
        onBack = if (addAccount) navigationManager::popBackStack else null,
        onSwitchAccount = onSwitchAccount,
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
    onBack: (() -> Unit)? = null,
    onSwitchAccount: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(RStrings.back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNetworkSettings, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(RStrings.network_setting),
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (onBack == null) {
                FloatingActionButton(onClick = onNetworkSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(RStrings.network_setting),
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    top = if (onBack == null) 75.dp else 24.dp,
                    end = 16.dp,
                    bottom = if (onBack == null) 96.dp else 24.dp,
                ),
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
            if (onSwitchAccount != null) {
                FilledTonalButton(
                    onClick = onSwitchAccount,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(RStrings.switch_to_another_account),
                        textAlign = TextAlign.Center,
                    )
                }
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
