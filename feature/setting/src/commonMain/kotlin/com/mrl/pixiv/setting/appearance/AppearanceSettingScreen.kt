package com.mrl.pixiv.setting.appearance

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mrl.pixiv.common.compose.theme.platformDynamicColorScheme
import com.mrl.pixiv.common.compose.theme.rememberAppColorScheme
import com.mrl.pixiv.common.data.setting.AppPaletteStyle
import com.mrl.pixiv.common.data.setting.AppearanceSettings
import com.mrl.pixiv.common.data.setting.ResolvedThemeColor
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.ThemeColor
import com.mrl.pixiv.common.data.setting.resolve
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.setAppCompatDelegateThemeMode
import com.mrl.pixiv.strings.appearance_setting
import com.mrl.pixiv.strings.back
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val preferences by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val theme = SettingTheme.entries.find { it.name == preferences.theme } ?: SettingTheme.SYSTEM
    var pickerSeed by rememberSaveable { mutableStateOf<Long?>(null) }
    if (pickerSeed != null && navigationManager.topEntryId == navigationManager.entryId) {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = { pickerSeed = null },
        )
    }
    AppearanceSettingContent(
        pickerSeed = pickerSeed,
        onPickerSeedChange = { pickerSeed = it },
        appearance = preferences.appearance,
        theme = theme,
        darkTheme = when (theme) {
            SettingTheme.SYSTEM -> isSystemInDarkTheme()
            SettingTheme.LIGHT -> false
            SettingTheme.DARK -> true
        },
        useLiquidGlassNavigation = preferences.useLiquidGlassNavigation,
        spanCount = preferences.spanCountPortrait,
        onAppearanceChange = SettingRepository::setAppearanceSettings,
        onThemeChange = {
            SettingRepository.setSettingTheme(it)
            setAppCompatDelegateThemeMode(it)
        },
        onGlassChange = { enabled ->
            SettingRepository.updateSettings { copy(useLiquidGlassNavigation = enabled) }
        },
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    )
}

@Composable
internal fun AppearanceSettingContent(
    pickerSeed: Long?,
    onPickerSeedChange: (Long?) -> Unit,
    appearance: AppearanceSettings,
    theme: SettingTheme,
    darkTheme: Boolean,
    useLiquidGlassNavigation: Boolean,
    spanCount: Int,
    onAppearanceChange: (AppearanceSettings) -> Unit,
    onThemeChange: (SettingTheme) -> Unit,
    onGlassChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    var useFidelity by rememberSaveable { mutableStateOf(false) }
    val draftStyle = if (useFidelity) AppPaletteStyle.FIDELITY else appearance.resolvedPaletteStyle
    val resolved = appearance.resolve(platformDynamicColorScheme(darkTheme) != null)
    val currentPrimary = MaterialTheme.colorScheme.primary.toArgb().toUInt().toLong()
    val previewAppearance = if (pickerSeed == null) appearance else appearance.copy(
        themeColor = ThemeColor.CUSTOM.name,
        customColor = pickerSeed,
        paletteStyle = draftStyle.name,
    )
    val scheme = if (pickerSeed == null) {
        MaterialTheme.colorScheme
    } else {
        rememberAppColorScheme(previewAppearance, darkTheme)
    }
    MaterialTheme(colorScheme = scheme) {
        AppearanceLayout(
            pickerOpen = pickerSeed != null,
            onBack = { if (pickerSeed == null) onBack() else onPickerSeedChange(null) },
            preview = { wide, previewModifier ->
                PreviewCard(
                    appearance = previewAppearance,
                    darkTheme = darkTheme,
                    wide = wide,
                    useLiquidGlassNavigation = useLiquidGlassNavigation,
                    spanCount = spanCount,
                    selectedPage = previewPages[pageIndex],
                    onSelectPage = { pageIndex = previewPages.indexOf(it) },
                    modifier = previewModifier,
                )
            },
            options = {
                AppearanceOptions(
                    appearance = appearance,
                    theme = theme,
                    darkTheme = darkTheme,
                    useLiquidGlassNavigation = useLiquidGlassNavigation,
                    onAppearanceChange = onAppearanceChange,
                    onThemeChange = onThemeChange,
                    onGlassChange = onGlassChange,
                    onCustomColor = {
                        useFidelity = false
                        val seed = appearance.customColor ?: (resolved as? ResolvedThemeColor.Seeded)?.seed
                        onPickerSeedChange(seed ?: currentPrimary)
                    },
                )
            },
            picker = { wide, panelModifier ->
                if (pickerSeed != null) {
                    CustomColorPicker(
                        seed = pickerSeed,
                        style = draftStyle,
                        wide = wide,
                        onSeedChange = onPickerSeedChange,
                        onUseFidelity = { useFidelity = true },
                        onCancel = { onPickerSeedChange(null) },
                        onApply = { seed ->
                            onAppearanceChange(previewAppearance.copy(customColor = seed))
                            onPickerSeedChange(null)
                        },
                        modifier = panelModifier,
                    )
                }
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun AppearanceLayout(
    pickerOpen: Boolean,
    onBack: () -> Unit,
    preview: @Composable (wide: Boolean, modifier: Modifier) -> Unit,
    options: @Composable () -> Unit,
    picker: @Composable (wide: Boolean, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionsScroll = rememberScrollState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = if (pickerOpen) {
            ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.appearance_setting)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.back))
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (maxWidth >= 600.dp) {
                Row(
                    Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    preview(true, Modifier.weight(1f).fillMaxHeight())
                    if (pickerOpen) {
                        picker(true, Modifier.width(400.dp))
                    } else {
                        Column(Modifier.width(400.dp).verticalScroll(optionsScroll).padding(bottom = 16.dp)) {
                            options()
                        }
                    }
                }
            } else if (pickerOpen) {
                val maxPanelHeight = maxHeight * 0.76f
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    preview(false, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp))
                    picker(false, Modifier.heightIn(max = maxPanelHeight))
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(optionsScroll)
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                ) {
                    preview(false, Modifier.fillMaxWidth().height(468.dp))
                    options()
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    appearance: AppearanceSettings,
    darkTheme: Boolean,
    wide: Boolean,
    useLiquidGlassNavigation: Boolean,
    spanCount: Int,
    selectedPage: MainPage,
    onSelectPage: (MainPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(if (wide) 16.dp else 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val gap = 12.dp
            // 两台样机各至少留出 144dp；较窄的详情栏只展示当前模式。
            val paired = wide && maxWidth >= 144.dp * 2 + gap
            val width = minOf(if (paired) (maxWidth - gap) / 2 else maxWidth, minOf(maxHeight, 640.dp) / 2)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                val modes = if (paired) listOf(false, true) else listOf(darkTheme)
                for (dark in modes) {
                    AppearancePreview(
                        colorScheme = rememberAppColorScheme(appearance, dark),
                        useLiquidGlassNavigation = useLiquidGlassNavigation,
                        spanCount = spanCount,
                        selectedPage = selectedPage,
                        onSelectPage = onSelectPage,
                        modifier = Modifier.size(width, width * 2),
                    )
                }
            }
        }
    }
}
