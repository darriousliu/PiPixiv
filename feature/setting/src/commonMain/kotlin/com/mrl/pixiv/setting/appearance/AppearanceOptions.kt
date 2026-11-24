package com.mrl.pixiv.setting.appearance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.theme.platformDynamicColorScheme
import com.mrl.pixiv.common.compose.theme.previewColorScheme
import com.mrl.pixiv.common.data.setting.AppPaletteStyle
import com.mrl.pixiv.common.data.setting.AppearanceSettings
import com.mrl.pixiv.common.data.setting.ResolvedThemeColor
import com.mrl.pixiv.common.data.setting.SettingTheme
import com.mrl.pixiv.common.data.setting.ThemeColor
import com.mrl.pixiv.common.data.setting.resolve
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.setting.LiquidGlassNavigationSetting
import com.mrl.pixiv.setting.components.SettingSectionTitle
import com.mrl.pixiv.setting.components.SettingSwitchRow
import com.mrl.pixiv.strings.amoled_dark
import com.mrl.pixiv.strings.amoled_dark_desc
import com.mrl.pixiv.strings.appearance_section_dark_mode
import com.mrl.pixiv.strings.appearance_section_display
import com.mrl.pixiv.strings.appearance_section_palette_style
import com.mrl.pixiv.strings.appearance_section_theme_color
import com.mrl.pixiv.strings.palette_style_disabled_default
import com.mrl.pixiv.strings.palette_style_disabled_dynamic
import com.mrl.pixiv.strings.palette_style_disabled_ink
import com.mrl.pixiv.strings.palette_style_fidelity
import com.mrl.pixiv.strings.palette_style_neutral
import com.mrl.pixiv.strings.palette_style_tonal_spot
import com.mrl.pixiv.strings.theme_color_amber
import com.mrl.pixiv.strings.theme_color_coral
import com.mrl.pixiv.strings.theme_color_custom
import com.mrl.pixiv.strings.theme_color_default
import com.mrl.pixiv.strings.theme_color_dynamic
import com.mrl.pixiv.strings.theme_color_dynamic_requirement
import com.mrl.pixiv.strings.theme_color_indigo
import com.mrl.pixiv.strings.theme_color_ink
import com.mrl.pixiv.strings.theme_color_jade
import com.mrl.pixiv.strings.theme_color_matcha
import com.mrl.pixiv.strings.theme_color_sakura
import com.mrl.pixiv.strings.theme_color_sky
import com.mrl.pixiv.strings.theme_color_wisteria
import com.mrl.pixiv.strings.theme_dark
import com.mrl.pixiv.strings.theme_light
import com.mrl.pixiv.strings.theme_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val ThemeColor.labelResource: StringResource
    get() = when (this) {
        ThemeColor.DYNAMIC -> RStrings.theme_color_dynamic
        ThemeColor.DEFAULT -> RStrings.theme_color_default
        ThemeColor.SKY -> RStrings.theme_color_sky
        ThemeColor.INDIGO -> RStrings.theme_color_indigo
        ThemeColor.WISTERIA -> RStrings.theme_color_wisteria
        ThemeColor.SAKURA -> RStrings.theme_color_sakura
        ThemeColor.CORAL -> RStrings.theme_color_coral
        ThemeColor.AMBER -> RStrings.theme_color_amber
        ThemeColor.MATCHA -> RStrings.theme_color_matcha
        ThemeColor.JADE -> RStrings.theme_color_jade
        ThemeColor.INK -> RStrings.theme_color_ink
        ThemeColor.CUSTOM -> RStrings.theme_color_custom
    }

@Composable
internal fun AppPaletteStyle.label(): String = stringResource(
    when (this) {
        AppPaletteStyle.TONAL_SPOT -> RStrings.palette_style_tonal_spot
        AppPaletteStyle.NEUTRAL -> RStrings.palette_style_neutral
        AppPaletteStyle.FIDELITY -> RStrings.palette_style_fidelity
    }
)

@Composable
internal fun AppearanceOptions(
    appearance: AppearanceSettings,
    theme: SettingTheme,
    darkTheme: Boolean,
    useLiquidGlassNavigation: Boolean,
    onAppearanceChange: (AppearanceSettings) -> Unit,
    onThemeChange: (SettingTheme) -> Unit,
    onGlassChange: (Boolean) -> Unit,
    onCustomColor: () -> Unit,
) {
    val wallpaper = platformDynamicColorScheme(darkTheme)
    val baseline = remember(darkTheme) { if (darkTheme) darkColorScheme() else expressiveLightColorScheme() }
    val resolved = appearance.resolve(wallpaper != null)
    SettingSectionTitle(stringResource(RStrings.appearance_section_theme_color))
    FlowRow(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (color in ThemeColor.entries) {
            if (color == ThemeColor.DYNAMIC && wallpaper == null) continue
            val source = appearance.copy(themeColor = color.name).resolve(wallpaper != null)
            val scheme = when (source) {
                ResolvedThemeColor.Baseline -> baseline
                ResolvedThemeColor.Dynamic -> requireNotNull(wallpaper)
                is ResolvedThemeColor.Seeded -> remember(source, darkTheme) {
                    previewColorScheme(source.seed, source.style, darkTheme)
                }
            }
            ThemeSwatch(
                color = color,
                selected = resolved.color == color,
                scheme = scheme,
                unusedCustom = color == ThemeColor.CUSTOM && appearance.customColor == null,
                onClick = {
                    if (color == ThemeColor.CUSTOM) onCustomColor()
                    else onAppearanceChange(appearance.copy(themeColor = color.name))
                },
            )
        }
    }
    val android = platform as? Platform.Android
    if (android != null && android.version < 31) {
        OptionDescription(stringResource(RStrings.theme_color_dynamic_requirement))
    }
    SettingSectionTitle(stringResource(RStrings.appearance_section_palette_style))
    val styleEnabled = resolved is ResolvedThemeColor.Seeded && resolved.style != null
    ConnectedChoices(
        options = AppPaletteStyle.entries,
        selected = appearance.resolvedPaletteStyle,
        enabled = styleEnabled,
        label = { it.label() },
        onSelect = { onAppearanceChange(appearance.copy(paletteStyle = it.name)) },
    )
    val styleHint = when (resolved.color) {
        ThemeColor.DYNAMIC -> RStrings.palette_style_disabled_dynamic
        ThemeColor.DEFAULT -> RStrings.palette_style_disabled_default
        ThemeColor.INK -> RStrings.palette_style_disabled_ink
        else -> null
    }
    if (styleHint != null) OptionDescription(stringResource(styleHint))
    SettingSectionTitle(stringResource(RStrings.appearance_section_dark_mode))
    ConnectedChoices(
        options = listOf(SettingTheme.SYSTEM, SettingTheme.LIGHT, SettingTheme.DARK),
        selected = theme,
        enabled = true,
        label = {
            stringResource(when (it) {
                SettingTheme.SYSTEM -> RStrings.theme_system
                SettingTheme.LIGHT -> RStrings.theme_light
                SettingTheme.DARK -> RStrings.theme_dark
            })
        },
        onSelect = onThemeChange,
    )
    SettingSectionTitle(stringResource(RStrings.appearance_section_display))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingSwitchRow(
            title = stringResource(RStrings.amoled_dark),
            icon = Icons.Rounded.BrightnessMedium,
            checked = appearance.amoledDark,
            onCheckedChange = { onAppearanceChange(appearance.copy(amoledDark = it)) },
            supportingText = stringResource(RStrings.amoled_dark_desc),
        )
        LiquidGlassNavigationSetting(useLiquidGlassNavigation, onGlassChange)
    }
}

@Composable
private fun ThemeSwatch(
    color: ThemeColor,
    selected: Boolean,
    scheme: ColorScheme,
    unusedCustom: Boolean,
    onClick: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val labelWidth = 64.dp + 44.dp * (fontScale - 1f).coerceAtLeast(0f)
    val labelStyle = if (selected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge
    Column(
        Modifier.width(labelWidth).clip(MaterialTheme.shapes.medium)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(if (selected) MaterialShapes.Cookie9Sided.toShape() else CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (unusedCustom) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        Brush.sweepGradient(
                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                        )
                    )
                }
            } else {
                PaletteDisc(scheme, Modifier.fillMaxSize())
            }
            val badge: ImageVector? = when {
                selected -> Icons.Rounded.Check
                unusedCustom -> Icons.Rounded.Add
                color == ThemeColor.CUSTOM -> Icons.Rounded.Colorize
                color == ThemeColor.DYNAMIC -> Icons.Rounded.Wallpaper
                else -> null
            }
            if (badge != null) {
                Box(
                    Modifier.size(28.dp).background(scheme.surfaceContainerLowest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(badge, null, Modifier.size(18.dp), tint = scheme.onSurface)
                }
            }
        }
        Text(
            stringResource(color.labelResource),
            style = labelStyle.copy(hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun PaletteDisc(scheme: ColorScheme, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val half = Size(size.width / 2, size.height / 2)
        drawRect(scheme.primary)
        drawRect(scheme.secondaryContainer, Offset(0f, half.height), half)
        drawRect(scheme.tertiary, Offset(half.width, half.height), half)
    }
}

@Composable
private fun OptionDescription(text: String) {
    Text(
        text,
        Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun <T> ConnectedChoices(
    options: List<T>,
    selected: T,
    enabled: Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, choice ->
            ToggleButton(
                checked = selected == choice,
                onCheckedChange = { onSelect(choice) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enabled = enabled,
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(label(choice), textAlign = TextAlign.Center)
            }
        }
    }
}
