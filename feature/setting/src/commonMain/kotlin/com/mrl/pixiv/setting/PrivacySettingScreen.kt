package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded._18UpRating
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.SettingSwitchRow
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.default_private_bookmark
import com.mrl.pixiv.strings.privacy_setting
import com.mrl.pixiv.strings.r18
import com.mrl.pixiv.strings.r18_alert_message
import com.mrl.pixiv.strings.read_clipboard_on_search
import com.mrl.pixiv.strings.read_clipboard_on_search_desc
import com.mrl.pixiv.strings.tips
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivacySettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()

    PrivacySettingContent(
        isR18Enabled = userPreference.isR18Enabled,
        defaultPrivateBookmark = userPreference.defaultPrivateBookmark,
        readClipboardOnSearch = userPreference.readClipboardOnSearch,
        onR18EnabledChange = SettingRepository::setIsR18Enabled,
        onDefaultPrivateBookmarkChange = SettingRepository::setDefaultPrivateBookmark,
        onReadClipboardOnSearchChange = SettingRepository::setReadClipboardOnSearch,
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    )
}

@Composable
internal fun PrivacySettingContent(
    isR18Enabled: Boolean,
    defaultPrivateBookmark: Boolean,
    readClipboardOnSearch: Boolean,
    onR18EnabledChange: (Boolean) -> Unit,
    onDefaultPrivateBookmarkChange: (Boolean) -> Unit,
    onReadClipboardOnSearchChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showR18Warning by rememberSaveable { mutableStateOf(false) }

    if (showR18Warning) {
        val tipText = stringResource(RStrings.r18_alert_message)
        AlertDialog(
            onDismissRequest = { showR18Warning = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onR18EnabledChange(true)
                        showR18Warning = false
                    },
                ) {
                    Text(text = stringResource(RStrings.confirm))
                }
            },
            title = { Text(text = stringResource(RStrings.tips)) },
            text = { Text(text = remember(tipText) { htmlToAnnotatedString(tipText) }) },
            dismissButton = {
                TextButton(onClick = { showR18Warning = false }) {
                    Text(text = stringResource(RStrings.cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(RStrings.privacy_setting),
                        style = LocalTextStyle.current.copy(
                            hyphens = Hyphens.Auto,
                            lineBreak = LineBreak.Heading,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SettingSwitchRow(
                    title = stringResource(RStrings.r18),
                    icon = Icons.Rounded._18UpRating,
                    checked = isR18Enabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showR18Warning = true
                        } else {
                            onR18EnabledChange(false)
                        }
                    },
                )
                SettingSwitchRow(
                    title = stringResource(RStrings.default_private_bookmark),
                    icon = Icons.Rounded.Favorite,
                    checked = defaultPrivateBookmark,
                    onCheckedChange = onDefaultPrivateBookmarkChange,
                )
                SettingSwitchRow(
                    title = stringResource(RStrings.read_clipboard_on_search),
                    icon = Icons.Rounded.ContentPaste,
                    checked = readClipboardOnSearch,
                    onCheckedChange = onReadClipboardOnSearchChange,
                    supportingText = stringResource(RStrings.read_clipboard_on_search_desc),
                )
            }
        }
    }
}
