package com.mrl.pixiv.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RDrawables
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.login.browser.isBrowserAvailable
import com.mrl.pixiv.login.browser.isJetbrainsRuntime
import com.mrl.pixiv.strings.browser_reason
import com.mrl.pixiv.strings.download_browser
import com.mrl.pixiv.strings.ic_launcher
import com.mrl.pixiv.strings.jetbrains_runtime_error
import com.mrl.pixiv.strings.sign_in
import com.mrl.pixiv.strings.sign_up
import com.mrl.pixiv.strings.sign_with_token
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun LoginOptionScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val isBrowserAvailable = isBrowserAvailable()
    val isJetbrainsRuntime = isJetbrainsRuntime()
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navigationManager.navigateToNetworkSettingScreen()
                }
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
                .padding(start = 16.dp, top = 75.dp, end = 16.dp),
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
                onClick = {
                    navigationManager.navigate(Destination.Login(generateWebViewUrl(false)))
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
                enabled = isBrowserAvailable && !isJetbrainsRuntime,
            ) {
                Text(
                    text = stringResource(RStrings.sign_in)
                )
            }
            Button(
                onClick = {
                    navigationManager.navigate(Destination.Login(generateWebViewUrl(true)))
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
                enabled = isBrowserAvailable && !isJetbrainsRuntime,
            ) {
                Text(
                    text = stringResource(RStrings.sign_up)
                )
            }
            OutlinedButton(
                onClick = {
                    navigationManager.navigate(Destination.OAuthLogin)
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(RStrings.sign_with_token)
                )
            }
            if (!isBrowserAvailable || isJetbrainsRuntime) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        navigationManager.navigate(Destination.DownloadBrowser)
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isJetbrainsRuntime
                ) {
                    Text(
                        text = stringResource(RStrings.download_browser)
                    )
                }
                Text(
                    text = stringResource(
                        if (isJetbrainsRuntime) RStrings.jetbrains_runtime_error
                        else RStrings.browser_reason
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
