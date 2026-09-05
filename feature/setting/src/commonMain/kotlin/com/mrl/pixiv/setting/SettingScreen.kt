package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.setting.components.DropDownSelector
import com.mrl.pixiv.strings.ai_translation_setting
import com.mrl.pixiv.strings.app_language
import com.mrl.pixiv.strings.browsing_setting
import com.mrl.pixiv.strings.file_name_format_title
import com.mrl.pixiv.strings.history_setting
import com.mrl.pixiv.strings.label_default
import com.mrl.pixiv.strings.network_setting
import com.mrl.pixiv.strings.privacy_setting
import com.mrl.pixiv.strings.search_setting
import com.mrl.pixiv.strings.setting
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

const val KEY_LANGUAGE = "language"
const val KEY_NETWORK_SETTING = "network_setting"
const val KEY_BROWSING_SETTING = "browsing_setting"
const val KEY_SEARCH_SETTING = "search_setting"
const val KEY_HISTORY_SETTING = "history_setting"
const val KEY_PRIVACY_SETTING = "privacy_setting"
const val KEY_FILE_NAME_FORMAT = "file_name_format"
const val KEY_AI_TRANSLATION_SETTING = "ai_translation_setting"
const val KEY_DEFAULT_OPEN_LINK = "default_open_link"

@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val labelDefault = stringResource(RStrings.label_default)
    val languages = remember { getLanguages() }
    var currentLanguage by remember(labelDefault) {
        mutableStateOf(getInitialLanguages() ?: labelDefault)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(RStrings.setting)) },
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
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
        ) {
            item(key = KEY_LANGUAGE) {
                var expanded by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = {
                        LaunchedEffect(currentLanguage, labelDefault) {
                            triggerLocaleChange(currentLanguage, labelDefault)
                        }
                        Text(text = stringResource(RStrings.app_language))
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.Translate, contentDescription = null)
                    },
                    trailingContent = {
                        DropDownSelector(
                            modifier = Modifier.throttleClick { expanded = !expanded },
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            current = currentLanguage,
                        ) {
                            languages.forEach { language ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = language.displayName,
                                                modifier = Modifier.padding(16.dp),
                                            )
                                            if (currentLanguage == language.langTag) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        currentLanguage = language.langTag
                                        expanded = false
                                    },
                                )
                            }
                        }
                    },
                )
            }
            item(key = KEY_NETWORK_SETTING) {
                SettingDestinationItem(
                    title = stringResource(RStrings.network_setting),
                    icon = Icons.Rounded.NetworkWifi,
                    onClick = navigationManager::navigateToNetworkSettingScreen,
                )
            }
            item(key = KEY_BROWSING_SETTING) {
                SettingDestinationItem(
                    title = stringResource(RStrings.browsing_setting),
                    icon = Icons.Rounded.Image,
                    onClick = navigationManager::navigateToBrowsingSettingScreen,
                )
            }
            item(key = KEY_SEARCH_SETTING) {
                SettingDestinationItem(
                    title = stringResource(RStrings.search_setting),
                    icon = Icons.Rounded.Search,
                    onClick = navigationManager::navigateToSearchSettingScreen,
                )
            }
            item(key = KEY_HISTORY_SETTING) {
                SettingDestinationItem(
                    title = stringResource(RStrings.history_setting),
                    icon = Icons.Rounded.History,
                    onClick = navigationManager::navigateToHistorySettingScreen,
                )
            }
            item(key = KEY_PRIVACY_SETTING) {
                SettingDestinationItem(
                    title = stringResource(RStrings.privacy_setting),
                    icon = Icons.Rounded.Lock,
                    onClick = navigationManager::navigateToPrivacySettingScreen,
                )
            }
            item(key = KEY_FILE_NAME_FORMAT) {
                SettingDestinationItem(
                    title = stringResource(RStrings.file_name_format_title),
                    icon = Icons.Rounded.Save,
                    onClick = navigationManager::navigateToFileNameFormatScreen,
                )
            }
            item(key = KEY_AI_TRANSLATION_SETTING) {
                SettingDestinationItem(
                    title = stringResource(RStrings.ai_translation_setting),
                    icon = Icons.Rounded.Translate,
                    onClick = navigationManager::navigateToAiTranslationSettingScreen,
                )
            }
            appLinkItem()
        }
    }
}

@Composable
private fun SettingDestinationItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        modifier = Modifier.throttleClick(indication = ripple(), onClick = onClick),
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
            )
        },
    )
}

expect fun getInitialLanguages(): String?

expect fun triggerLocaleChange(
    currentLanguage: String,
    labelDefault: String,
)

expect fun LazyListScope.appLinkItem()
