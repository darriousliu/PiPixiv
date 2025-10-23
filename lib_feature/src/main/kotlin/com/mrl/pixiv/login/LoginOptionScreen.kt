package com.mrl.pixiv.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RDrawable
import com.mrl.pixiv.common.util.RString
import org.koin.compose.koinInject

@Composable
fun LoginOptionScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    Scaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(start = 16.dp, top = 75.dp, end = 16.dp),
            verticalArrangement = 10f.spaceBy,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = RDrawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(75.dp)
                    .clip(CircleShape)
            )
            Button(
                onClick = {
                    navigationManager.navigate(Destination.LoginScreen(generateWebViewUrl(false)))
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(RString.sign_in)
                )
            }
            Button(
                onClick = {
                    navigationManager.navigate(Destination.LoginScreen(generateWebViewUrl(true)))
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(RString.sign_up)
                )
            }
            OutlinedButton(
                onClick = {
                    navigationManager.navigate(Destination.OAuthLoginScreen)
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(RString.sign_with_token)
                )
            }
        }
    }
}