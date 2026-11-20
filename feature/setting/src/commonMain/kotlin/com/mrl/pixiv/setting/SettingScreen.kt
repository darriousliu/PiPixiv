package com.mrl.pixiv.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.VersionManager
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.SettingChoiceValue
import com.mrl.pixiv.setting.components.SettingRow
import com.mrl.pixiv.setting.components.SettingSectionTitle
import com.mrl.pixiv.setting.components.SettingSwitchRow
import com.mrl.pixiv.strings.about
import com.mrl.pixiv.strings.ai_translation_setting
import com.mrl.pixiv.strings.allow_open_link
import com.mrl.pixiv.strings.app_language
import com.mrl.pixiv.strings.block_settings
import com.mrl.pixiv.strings.browsing_setting
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.data_storage_title
import com.mrl.pixiv.strings.default_open
import com.mrl.pixiv.strings.download_settings_title
import com.mrl.pixiv.strings.export_token
import com.mrl.pixiv.strings.history_setting
import com.mrl.pixiv.strings.label_default
import com.mrl.pixiv.strings.liquid_glass_navigation
import com.mrl.pixiv.strings.liquid_glass_navigation_desc
import com.mrl.pixiv.strings.network_setting
import com.mrl.pixiv.strings.new_version_available
import com.mrl.pixiv.strings.privacy_setting
import com.mrl.pixiv.strings.search_setting
import com.mrl.pixiv.strings.setting
import com.mrl.pixiv.strings.settings_section_account
import com.mrl.pixiv.strings.settings_section_app
import com.mrl.pixiv.strings.settings_section_content
import com.mrl.pixiv.strings.settings_section_general
import com.mrl.pixiv.strings.sign_out
import com.mrl.pixiv.strings.sign_out_confirmation
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

const val KEY_DEFAULT_OPEN_LINK = "default_open_link"

internal enum class SettingPage {
    Network, Browsing, Search, History, Privacy, Download, AiTranslation, Block, AppData, About
}

@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
    viewModel: SettingViewModel = koinViewModel(),
) {
    val labelDefault = stringResource(RStrings.label_default)
    val languages = remember { getLanguages() }
    val hasNewVersion by VersionManager.hasNewVersion.collectAsStateWithLifecycle()
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    var currentLanguage by remember(labelDefault) {
        mutableStateOf(getInitialLanguages() ?: labelDefault)
    }
    LaunchedEffect(currentLanguage, labelDefault) {
        triggerLocaleChange(currentLanguage, labelDefault)
    }
    val topEntryId = navigationManager.topEntryId
    SettingContent(
        currentLanguage = currentLanguage,
        languages = languages,
        hasNewVersion = hasNewVersion,
        useLiquidGlassNavigation = userPreference.useLiquidGlassNavigation,
        onLanguageChange = { currentLanguage = it },
        onUseLiquidGlassNavigationChange = { enabled ->
            SettingRepository.updateSettings { copy(useLiquidGlassNavigation = enabled) }
        },
        onBack = navigationManager::popBackStack,
        onNavigate = { page ->
            when (page) {
                SettingPage.Network -> navigationManager.navigateToNetworkSettingScreen()
                SettingPage.Browsing -> navigationManager.navigateToBrowsingSettingScreen()
                SettingPage.Search -> navigationManager.navigateToSearchSettingScreen()
                SettingPage.History -> navigationManager.navigateToHistorySettingScreen()
                SettingPage.Privacy -> navigationManager.navigateToPrivacySettingScreen()
                SettingPage.Download -> navigationManager.navigateToFileNameFormatScreen()
                SettingPage.AiTranslation -> navigationManager.navigateToAiTranslationSettingScreen()
                SettingPage.Block -> navigationManager.navigateToBlockSettings()
                SettingPage.AppData -> navigationManager.navigateToAppDataScreen()
                SettingPage.About -> navigationManager.navigateToAboutScreen()
            }
        },
        onExportToken = { viewModel.dispatch(SettingAction.ExportToken) },
        onSignOut = {
            if (topEntryId != null && navigationManager.topEntryId == topEntryId) {
                viewModel.logout()
            }
        },
        appLinks = { appLinkItem() },
        modifier = modifier,
        topEntryId = topEntryId,
    )
}

@Composable
internal fun SettingContent(
    currentLanguage: String,
    languages: List<Language>,
    hasNewVersion: Boolean,
    useLiquidGlassNavigation: Boolean,
    onLanguageChange: (String) -> Unit,
    onUseLiquidGlassNavigationChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigate: (SettingPage) -> Unit,
    onExportToken: () -> Unit,
    onSignOut: () -> Unit,
    topEntryId: String?,
    modifier: Modifier = Modifier,
    appLinks: LazyListScope.() -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    // 设置页在分栏导航中可能一直可见，弹窗只属于打开时的导航位置。
    var showSignOutConfirmation by key(topEntryId) {
        rememberSaveable { mutableStateOf(false) }
    }
    if (showSignOutConfirmation) {
        SignOutConfirmationDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            onConfirm = {
                if (showSignOutConfirmation) {
                    showSignOutConfirmation = false
                    onSignOut()
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.setting)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item("general_header") { SettingSectionTitle(stringResource(RStrings.settings_section_general)) }
            item("language") {
                LanguageSettingItem(
                    currentLanguage = currentLanguage,
                    languages = languages,
                    onLanguageChange = onLanguageChange,
                )
            }
            item("liquid_glass_navigation") {
                LiquidGlassNavigationSetting(
                    checked = useLiquidGlassNavigation,
                    onCheckedChange = onUseLiquidGlassNavigationChange,
                )
            }
            item("network") {
                SettingDestinationItem(
                    stringResource(RStrings.network_setting),
                    Icons.Rounded.NetworkWifi
                ) {
                    onNavigate(SettingPage.Network)
                }
            }
            appLinks()
            item("content_header") { SettingSectionTitle(stringResource(RStrings.settings_section_content)) }
            item("browsing") {
                SettingDestinationItem(
                    stringResource(RStrings.browsing_setting),
                    Icons.Rounded.Image
                ) {
                    onNavigate(SettingPage.Browsing)
                }
            }
            item("search") {
                SettingDestinationItem(
                    stringResource(RStrings.search_setting),
                    Icons.Rounded.Search
                ) {
                    onNavigate(SettingPage.Search)
                }
            }
            item("history") {
                SettingDestinationItem(
                    stringResource(RStrings.history_setting),
                    Icons.Rounded.History
                ) {
                    onNavigate(SettingPage.History)
                }
            }
            item("privacy") {
                SettingDestinationItem(
                    stringResource(RStrings.privacy_setting),
                    Icons.Rounded.Lock
                ) {
                    onNavigate(SettingPage.Privacy)
                }
            }
            item("blocking") {
                SettingDestinationItem(
                    stringResource(RStrings.block_settings),
                    Icons.Rounded.Block
                ) {
                    onNavigate(SettingPage.Block)
                }
            }
            item("ai_translation") {
                SettingDestinationItem(
                    stringResource(RStrings.ai_translation_setting),
                    Icons.Rounded.Translate
                ) {
                    onNavigate(SettingPage.AiTranslation)
                }
            }
            item("app_header") { SettingSectionTitle(stringResource(RStrings.settings_section_app)) }
            item("downloads") {
                SettingDestinationItem(
                    stringResource(RStrings.download_settings_title),
                    Icons.Rounded.Save
                ) {
                    onNavigate(SettingPage.Download)
                }
            }
            item("data") {
                SettingDestinationItem(
                    stringResource(RStrings.data_storage_title),
                    Icons.Rounded.Storage
                ) {
                    onNavigate(SettingPage.AppData)
                }
            }
            item("about") {
                SettingDestinationItem(
                    title = stringResource(RStrings.about),
                    icon = Icons.Rounded.Info,
                    badge = if (hasNewVersion) stringResource(RStrings.new_version_available) else null,
                ) { onNavigate(SettingPage.About) }
            }
            item("account_header") { SettingSectionTitle(stringResource(RStrings.settings_section_account)) }
            item("export_token") {
                SettingDestinationItem(
                    stringResource(RStrings.export_token),
                    Icons.Rounded.ImportExport,
                    showArrow = false
                ) {
                    onExportToken()
                }
            }
            item("sign_out") {
                SettingDestinationItem(
                    stringResource(RStrings.sign_out),
                    Icons.AutoMirrored.Rounded.Logout,
                    showArrow = false
                ) {
                    showSignOutConfirmation = true
                }
            }
            item("bottom_spacing") { Column(Modifier.padding(bottom = 16.dp)) {} }
        }
    }
}

@Composable
internal fun SignOutConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.key != Key.Escape) return@onPreviewKeyEvent false
            // 在 Desktop 的全局返回处理器收到 Escape 之前关闭弹窗。
            if (event.type == KeyEventType.KeyUp) onDismissRequest()
            true
        },
        title = { Text(stringResource(RStrings.sign_out_confirmation)) },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.focusRequester(cancelFocusRequester),
            ) {
                Text(stringResource(RStrings.cancel))
            }
            LaunchedEffect(Unit) { cancelFocusRequester.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(RStrings.sign_out))
            }
        },
    )
}

@Composable
internal fun LiquidGlassNavigationSetting(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingSwitchRow(
        title = stringResource(RStrings.liquid_glass_navigation),
        icon = Icons.Rounded.BlurOn,
        checked = checked,
        onCheckedChange = onCheckedChange,
        supportingText = stringResource(RStrings.liquid_glass_navigation_desc),
    )
}

@Composable
private fun LanguageSettingItem(
    currentLanguage: String,
    languages: List<Language>,
    onLanguageChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDisplayName = languages.firstOrNull { it.langTag == currentLanguage }?.displayName
        ?: currentLanguage
    SettingDestinationItem(
        title = stringResource(RStrings.app_language),
        icon = Icons.Rounded.Translate,
        trailingContent = {
            Box {
                SettingChoiceValue(value = currentDisplayName, maxLines = 1)
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.displayName) },
                            trailingIcon = if (language.langTag == currentLanguage) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else null,
                            onClick = {
                                expanded = false
                                onLanguageChange(language.langTag)
                            },
                        )
                    }
                }
            }
        },
        onClick = { expanded = !expanded },
    )
}

@Composable
private fun SettingDestinationItem(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    supportingText: String? = null,
    showArrow: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    SettingRow(
        title = title,
        icon = icon,
        onClick = rememberThrottleClick(onClick = onClick),
        supportingContent = when {
            badge != null -> {
                { Badge { Text(badge, Modifier.padding(horizontal = 4.dp)) } }
            }

            supportingText != null -> {
                { Text(supportingText) }
            }

            else -> null
        },
        trailingContent = trailingContent ?: if (!showArrow) null else {
            { Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null) }
        },
    )
}

@Composable
internal fun AppLinkSettingItem(onClick: () -> Unit) {
    SettingDestinationItem(
        title = stringResource(RStrings.default_open),
        icon = Icons.Rounded.AddLink,
        supportingText = stringResource(RStrings.allow_open_link),
        onClick = onClick,
    )
}

expect fun getInitialLanguages(): String?
expect fun triggerLocaleChange(currentLanguage: String, labelDefault: String)
expect fun LazyListScope.appLinkItem()
