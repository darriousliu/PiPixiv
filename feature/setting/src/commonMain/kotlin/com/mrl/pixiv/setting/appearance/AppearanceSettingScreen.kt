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
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
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
import com.mrl.pixiv.common.compose.theme.baselineColorScheme
import com.mrl.pixiv.common.compose.theme.platformDynamicColorScheme
import com.mrl.pixiv.common.compose.theme.rememberAppColorSchemeState
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val preferences by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val theme = SettingTheme.entries.find { it.name == preferences.theme } ?: SettingTheme.SYSTEM
    val draft = rememberSaveable(saver = CustomColorDraft.Saver) { CustomColorDraft() }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = draft.isOpen && navigationManager.topEntryId == navigationManager.entryId,
        onBackCompleted = { draft.seed = null },
    )
    AppearanceSettingContent(
        draft = draft,
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

/**
 * 取色器里的候选色，为空表示取色器关闭。
 *
 * 拖动时只有读取 [seed] 的取色器和样机配色会更新；页面只读 [isOpen]，不随拖动重组。
 */
@Stable
internal class CustomColorDraft(initialSeed: Long? = null) {
    var seed: Long? by mutableStateOf(initialSeed)
    val isOpen: Boolean by derivedStateOf { seed != null }

    companion object {
        val Saver: Saver<CustomColorDraft, Long> = Saver(save = { it.seed }, restore = { CustomColorDraft(it) })
    }
}

private fun AppearanceSettings.withCustomColor(seed: Long, style: AppPaletteStyle) =
    copy(themeColor = ThemeColor.CUSTOM.name, customColor = seed, paletteStyle = style.name)

@Composable
internal fun AppearanceSettingContent(
    draft: CustomColorDraft,
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
    val pageScheme = MaterialTheme.colorScheme
    // 候选色只在快照中读取，拖动时页面本身不重组。
    val previewAppearance: () -> AppearanceSettings = {
        draft.seed?.let { appearance.withCustomColor(it, draftStyle) } ?: appearance
    }
    // 候选配色只用于样机和「生成」圆盘，页面保持当前主题，换配色时只有样机重组。
    val candidateScheme = if (draft.isOpen) {
        rememberAppColorSchemeState(darkTheme, placeholder = { pageScheme }, appearance = previewAppearance)
    } else {
        null
    }
    AppearanceLayout(
        pickerOpen = draft.isOpen,
        onBack = onBack,
        onClosePicker = { draft.seed = null },
        preview = { wide, previewModifier ->
            PreviewCard(
                appearance = previewAppearance,
                candidateScheme = candidateScheme,
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
                    draft.seed = appearance.customColor
                        ?: (resolved as? ResolvedThemeColor.Seeded)?.seed
                        ?: pageScheme.primary.toArgb().toUInt().toLong()
                },
            )
        },
        picker = { wide, panelModifier, close ->
            val seed = draft.seed
            if (seed != null && candidateScheme != null) {
                CustomColorPicker(
                    seed = seed,
                    style = draftStyle,
                    generatedScheme = { candidateScheme.value },
                    wide = wide,
                    onSeedChange = { draft.seed = it },
                    onUseFidelity = { useFidelity = true },
                    onCancel = close,
                    onApply = {
                        onAppearanceChange(appearance.withCustomColor(it, draftStyle))
                        close()
                    },
                    modifier = panelModifier,
                )
            }
        },
        modifier = modifier,
    )
}

/** 双栏时选项或取色器所在右栏的最大宽度。 */
private val SidePanelMaxWidth = 400.dp

/** 右栏的最小宽度，与窄屏手机上选项区和取色器的可用宽度相当。 */
private val SidePanelMinWidth = 320.dp

/** 双栏时样机卡片的最小宽度，扣除内边距后样机约 168dp。 */
private val PreviewMinWidth = 200.dp

/** 双栏的左右边距和栏间距。 */
private val WideSpacing = 24.dp

/** 单栏布局中样机卡片的高度。 */
private val PreviewCardHeight = 468.dp

@Composable
private fun AppearanceLayout(
    pickerOpen: Boolean,
    onBack: () -> Unit,
    onClosePicker: () -> Unit,
    preview: @Composable (wide: Boolean, modifier: Modifier) -> Unit,
    options: @Composable () -> Unit,
    picker: @Composable (wide: Boolean, modifier: Modifier, close: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionsScroll = rememberScrollState()
    // 窄屏弹窗显示时提供带收起动画的关闭方式，顶栏返回也走这里。
    var closeSheet by remember { mutableStateOf<(() -> Unit)?>(null) }
    Scaffold(
        modifier = modifier,
        // 取色时底部边距交给右栏或弹窗处理，弹窗可以延伸到导航栏后面。
        contentWindowInsets = if (pickerOpen) {
            ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.appearance_setting)) },
                navigationIcon = {
                    IconButton(
                        onClick = { if (pickerOpen) (closeSheet ?: onClosePicker)() else onBack() },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(RStrings.back))
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            // 先给样机卡片留出最小宽度，右栏在剩余空间内伸缩；右栏也放不下时改用单栏。
            // 选项和取色器共用同一判断，打开取色器时不切换布局。
            val sidePanelWidth = (maxWidth - WideSpacing * 3 - PreviewMinWidth).coerceAtMost(SidePanelMaxWidth)
            if (sidePanelWidth >= SidePanelMinWidth) {
                Row(
                    Modifier.fillMaxSize().navigationBarsPadding()
                        .padding(horizontal = WideSpacing, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(WideSpacing),
                ) {
                    preview(true, Modifier.weight(1f).fillMaxHeight())
                    if (pickerOpen) {
                        picker(true, Modifier.width(sidePanelWidth), onClosePicker)
                    } else {
                        Column(Modifier.width(sidePanelWidth).verticalScroll(optionsScroll).padding(bottom = 16.dp)) {
                            options()
                        }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(optionsScroll)
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                ) {
                    preview(false, Modifier.fillMaxWidth().height(PreviewCardHeight))
                    options()
                }
                if (pickerOpen) {
                    // 页面滚回顶部，让样机露在弹窗上方。
                    LaunchedEffect(Unit) { optionsScroll.animateScrollTo(0) }
                    CompactPickerSheet(
                        onClosePicker = onClosePicker,
                        onCloseSheetChange = { closeSheet = it },
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) { close ->
                        picker(false, Modifier, close)
                    }
                }
            }
        }
    }
}

/**
 * 窄屏用非模态底部弹窗承载取色器，不加遮罩，弹窗上方的页面保持可见。
 *
 * 下滑关闭或返回会丢弃候选色；`close` 先播完收起动画再关闭取色器。
 */
@Composable
private fun CompactPickerSheet(
    onClosePicker: () -> Unit,
    onCloseSheetChange: ((() -> Unit)?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onClosePicker() }
    }
    DisposableEffect(sheetState) {
        onCloseSheetChange(close)
        onDispose { onCloseSheetChange(null) }
    }
    LaunchedEffect(sheetState) { sheetState.expand() }
    BottomSheet(
        state = sheetState,
        onDismissRequest = onClosePicker,
        modifier = modifier,
        // 比后面的样机卡片高一级，没有遮罩时也能看清弹窗边缘。
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        content(close)
    }
}

@Composable
private fun PreviewCard(
    appearance: () -> AppearanceSettings,
    candidateScheme: State<ColorScheme>?,
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
                    key(dark) {
                        // 当前模式平时直接用页面配色，取色时用候选配色；另一模式按同一外观在后台生成。
                        val scheme = when {
                            dark != darkTheme -> rememberAppColorSchemeState(
                                darkTheme = dark,
                                placeholder = { baselineColorScheme(dark) },
                                appearance = appearance,
                            ).value
                            candidateScheme != null -> candidateScheme.value
                            else -> MaterialTheme.colorScheme
                        }
                        AppearancePreview(
                            colorScheme = scheme,
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
}
