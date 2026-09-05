package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
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
import org.koin.compose.koinInject

@Composable
fun PrivacySettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()
    var showR18Warning by rememberSaveable { mutableStateOf(false) }

    if (showR18Warning) {
        val tipText = stringResource(RStrings.r18_alert_message)
        AlertDialog(
            onDismissRequest = { showR18Warning = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        SettingRepository.setIsR18Enabled(true)
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
                title = { Text(text = stringResource(RStrings.privacy_setting)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
        ) {
            ListItem(
                headlineContent = { Text(text = stringResource(RStrings.r18)) },
                modifier = Modifier.throttleClick(indication = ripple()) {
                    if (userPreference.isR18Enabled) {
                        SettingRepository.setIsR18Enabled(false)
                    } else {
                        showR18Warning = true
                    }
                },
                leadingContent = {
                    Icon(imageVector = Icons.Rounded._18UpRating, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = userPreference.isR18Enabled,
                        onCheckedChange = { checked ->
                            if (checked) showR18Warning = true
                            else SettingRepository.setIsR18Enabled(false)
                        },
                    )
                },
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.default_private_bookmark))
                },
                modifier = Modifier.throttleClick(indication = ripple()) {
                    SettingRepository.setDefaultPrivateBookmark(
                        !userPreference.defaultPrivateBookmark
                    )
                },
                leadingContent = {
                    Icon(imageVector = Icons.Rounded.Favorite, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = userPreference.defaultPrivateBookmark,
                        onCheckedChange = SettingRepository::setDefaultPrivateBookmark,
                    )
                },
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.read_clipboard_on_search))
                },
                supportingContent = {
                    Text(text = stringResource(RStrings.read_clipboard_on_search_desc))
                },
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .throttleClick(indication = ripple()) {
                        SettingRepository.setReadClipboardOnSearch(
                            !userPreference.readClipboardOnSearch
                        )
                    },
                leadingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentPaste, contentDescription = null)
                    }
                },
                trailingContent = {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Switch(
                            checked = userPreference.readClipboardOnSearch,
                            onCheckedChange = SettingRepository::setReadClipboardOnSearch,
                        )
                    }
                },
            )
        }
    }
}
