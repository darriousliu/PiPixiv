package com.mrl.pixiv.setting.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.data.Constants
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.repository.VersionManager.getCurrentFlavorAsset
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RDrawables
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.setting.components.SettingPageColumn
import com.mrl.pixiv.setting.components.SettingRow
import com.mrl.pixiv.strings.about
import com.mrl.pixiv.strings.app_name
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.check_update
import com.mrl.pixiv.strings.current_version
import com.mrl.pixiv.strings.download
import com.mrl.pixiv.strings.feedback
import com.mrl.pixiv.strings.feedback_content
import com.mrl.pixiv.strings.ic_launcher
import com.mrl.pixiv.strings.new_version_available
import com.mrl.pixiv.strings.project_url
import com.mrl.pixiv.strings.recommend_content
import com.mrl.pixiv.strings.recommend_this_app
import com.mrl.pixiv.strings.share_app
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()
    val latestVersionInfo by VersionManager.latestVersionInfo.collectAsStateWithLifecycle()
    var showUpdateDialog by retain { mutableStateOf(false) }

    AboutContent(
        versionName = AppUtil.versionName,
        hasNewVersion = hasNewVersion,
        onBack = navigationManager::popBackStack,
        onOpenProject = { uriHandler.openUri(Constants.GITHUB_URL) },
        onFeedback = { uriHandler.openUri(Constants.GITHUB_ISSUE_URL) },
        onShare = {
            coroutineScope.launch {
                ShareUtil.shareText(
                    AppUtil.getString(RStrings.recommend_content, Constants.GITHUB_RELEASE_URL)
                )
            }
        },
        onCheckUpdate = {
            VersionManager.checkUpdate(true)
            showUpdateDialog = true
        },
        modifier = modifier,
    )

    if (showUpdateDialog && latestVersionInfo != null) {
        val latestVersionInfo = latestVersionInfo!!
        val asset = latestVersionInfo.getCurrentFlavorAsset()
        val releaseNotes = remember(latestVersionInfo.body) {
            normalizeReleaseNotesLineEndings(latestVersionInfo.body.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            confirmButton = {
                TextButton(
                    onClick = click@{
                        val url = asset?.downloadUrl
                            ?: run {
                                VersionManager.checkUpdate()
                                uriHandler.openUri(Constants.GITHUB_RELEASE_URL)
                                return@click
                            }
                        uriHandler.openUri(url)
                    }
                ) {
                    Text(text = stringResource(RStrings.download))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false
                    }
                ) {
                    Text(text = stringResource(RStrings.cancel))
                }
            },
            title = {
                Text(text = asset?.name ?: latestVersionInfo.tagName)
            },
            text = {
                Markdown(
                    content = releaseNotes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        )
    }
}

@Composable
internal fun AboutContent(
    versionName: String,
    hasNewVersion: Boolean,
    onBack: () -> Unit,
    onOpenProject: () -> Unit,
    onFeedback: () -> Unit,
    onShare: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingPageColumn(innerPadding) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(RDrawables.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                )
                Text(
                    text = stringResource(RStrings.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = stringResource(RStrings.current_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SettingRow(
                title = stringResource(RStrings.project_url),
                icon = Icons.Rounded.Link,
                onClick = rememberThrottleClick(onClick = onOpenProject),
                supportingContent = { Text(Constants.GITHUB_URL) },
            )
            SettingRow(
                title = stringResource(RStrings.feedback),
                icon = Icons.Rounded.Feedback,
                onClick = rememberThrottleClick(onClick = onFeedback),
                supportingContent = { Text(stringResource(RStrings.feedback_content)) },
            )
            SettingRow(
                title = stringResource(RStrings.share_app),
                icon = Icons.Rounded.Share,
                onClick = rememberThrottleClick(onClick = onShare),
                supportingContent = { Text(stringResource(RStrings.recommend_this_app)) },
            )
            BoxWithConstraints {
                val stackBadge = maxWidth < 350.dp * LocalDensity.current.fontScale
                val badge: (@Composable () -> Unit)? = if (hasNewVersion) {
                    {
                        Badge {
                            Text(
                                text = stringResource(RStrings.new_version_available),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = LocalTextStyle.current.copy(hyphens = Hyphens.Auto),
                            )
                        }
                    }
                } else {
                    null
                }
                SettingRow(
                    title = stringResource(RStrings.check_update),
                    icon = Icons.Rounded.Refresh,
                    onClick = rememberThrottleClick(onClick = onCheckUpdate),
                    supportingContent = if (stackBadge) badge else null,
                    trailingContent = if (stackBadge) null else badge,
                )
            }
        }
    }
}

// The Markdown parser requires LF line endings for tables and thematic breaks.
internal fun normalizeReleaseNotesLineEndings(content: String): String =
    content.replace("\r\n", "\n").replace('\r', '\n')
