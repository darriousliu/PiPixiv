package com.mrl.pixiv.setting.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.repository.VersionManager.getCurrentFlavorAsset
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.CmnRDrawable
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
    val latestVersionInfo by VersionManager.latestVersionInfo.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }

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
                Image(
                    painter = painterResource(id = CmnRDrawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = stringResource(RString.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = stringResource(RString.current_version, AppUtil.versionName),
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
                            uriHandler.openUri(Constants.GITHUB_URL)
                        },
                    supportingContent = {
                        Text(text = Constants.GITHUB_URL)
                    }
                )

                // Feedback
                ListItem(
                    headlineContent = { Text(text = stringResource(RString.feedback)) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .throttleClick(indication = ripple()) {
                            uriHandler.openUri(Constants.GITHUB_ISSUE_URL)
                        },
                    supportingContent = {
                        Text(text = stringResource(RString.feedback_content))
                    }
                )

                // Share App
                ListItem(
                    headlineContent = { Text(text = stringResource(RString.share_app)) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .throttleClick(indication = ripple()) {
                            val intent =
                                ShareUtil.createShareIntent(
                                    AppUtil.getString(
                                        RString.recommend_content,
                                        Constants.GITHUB_RELEASE_URL
                                    )
                                )
                            context.startActivity(intent)
                        },
                    supportingContent = {
                        Text(text = stringResource(RString.recommend_this_app))
                    }
                )

                // Check Update
                ListItem(
                    headlineContent = { Text(text = stringResource(RString.check_update)) },
                    trailingContent = {
                        if (hasNewVersion) {
                            Badge {
                                Text(
                                    text = stringResource(RString.new_version_available),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .throttleClick(indication = ripple()) {
                            VersionManager.checkUpdate()
                            showUpdateDialog = true
                        }
                )
            }
        }
    }

    if (showUpdateDialog && latestVersionInfo != null) {
        val latestVersionInfo = latestVersionInfo!!
        val asset = latestVersionInfo.getCurrentFlavorAsset()
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            confirmButton = {
                TextButton(
                    onClick = click@{
                        val url = asset?.downloadUrl
                            ?: run {
                                VersionManager.checkUpdate()
                                return@click
                            }
                        uriHandler.openUri(url)
                    }
                ) {
                    Text(text = stringResource(RString.download))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false
                    }
                ) {
                    Text(text = stringResource(RString.cancel))
                }
            },
            title = {
                Text(text = asset?.name.orEmpty())
            },
            text = {
                Text(
                    text = latestVersionInfo.body.orEmpty(),
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        )
    }
}
