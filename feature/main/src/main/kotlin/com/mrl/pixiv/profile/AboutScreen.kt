package com.mrl.pixiv.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.throttleClick
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(RString.about)) },
                navigationIcon = {
                    IconButton(onClick = { navigationManager.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Icon and Version
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info, // Placeholder for App Icon
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(RString.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "${stringResource(RString.current_version)}: 1.0.0", // Placeholder version
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(
                modifier = Modifier.weight(2f)
            ) {
                // Project URL
                ListItem(
                    headlineContent = { Text(text = stringResource(RString.project_url)) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .throttleClick(indication = ripple()) {
                            uriHandler.openUri("https://github.com/Mrl98/PiPixiv")
                        }
                )

                // Feedback
                ListItem(
                    headlineContent = { Text(text = stringResource(RString.feedback)) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .throttleClick(indication = ripple()) {
                            uriHandler.openUri("https://github.com/Mrl98/PiPixiv/issues")
                        }
                )

                // Share App
                ListItem(
                    headlineContent = { Text(text = stringResource(RString.share_app)) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .throttleClick(indication = ripple()) {
                            val intent = ShareUtil.createShareIntent("Check out this app: https://github.com/Mrl98/PiPixiv")
                            context.startActivity(intent)
                        }
                )

                // Check Update
                ListItem(
                    headlineContent = { Text(text = stringResource(RString.check_update)) },
                    trailingContent = {
                        if (hasNewVersion) {
                            Text(
                                text = stringResource(RString.new_version_available),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .throttleClick(indication = ripple()) {
                            VersionManager.checkUpdate()
                        }
                )
            }
        }
    }
}
