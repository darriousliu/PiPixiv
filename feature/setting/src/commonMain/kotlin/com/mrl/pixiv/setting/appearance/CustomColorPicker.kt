package com.mrl.pixiv.setting.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.mrl.pixiv.common.data.setting.AppPaletteStyle
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.apply
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.custom_color_brightness
import com.mrl.pixiv.strings.custom_color_generated
import com.mrl.pixiv.strings.custom_color_hex
import com.mrl.pixiv.strings.custom_color_selected
import com.mrl.pixiv.strings.custom_color_style_hint
import com.mrl.pixiv.strings.custom_color_title
import com.mrl.pixiv.strings.custom_color_use_fidelity
import com.mrl.pixiv.strings.custom_color_wheel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CustomColorPicker(
    seed: Long,
    style: AppPaletteStyle,
    wide: Boolean,
    onSeedChange: (Long) -> Unit,
    onUseFidelity: () -> Unit,
    onCancel: () -> Unit,
    onApply: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberColorPickerController()
    val hexField = rememberTextFieldState(seed.toColorHex())
    var synchronizedSeed by remember { mutableLongStateOf(seed) }
    val updateSeed by rememberUpdatedState(onSeedChange)
    val selectedColor = Color(seed)

    LaunchedEffect(hexField, controller) {
        snapshotFlow { hexField.text.toString() }.collect { text ->
            val parsed = text.parseColorHex()
            if (parsed != null && parsed != synchronizedSeed) {
                synchronizedSeed = parsed
                controller.selectByColor(Color(parsed), fromUser = false)
                updateSeed(parsed)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = if (wide) MaterialTheme.shapes.extraLarge else BottomSheetDefaults.ExpandedShape,
        color = if (wide) MaterialTheme.colorScheme.surfaceContainerLow else BottomSheetDefaults.ContainerColor,
    ) {
        Column(
            modifier = Modifier
                .then(if (wide) Modifier else Modifier.navigationBarsPadding())
                .padding(
                    horizontal = if (wide) 20.dp else 24.dp,
                    vertical = if (wide) 20.dp else 0.dp,
                ),
        ) {
            if (!wide) BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(RStrings.custom_color_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                ColorInputs(selectedColor, controller, hexField) { color ->
                    val newSeed = color.toArgb().toUInt().toLong()
                    synchronizedSeed = newSeed
                    onSeedChange(newSeed)
                    hexField.setTextAndPlaceCursorAtEnd(newSeed.toColorHex())
                }
                ColorBrightness(selectedColor, controller)
                if (style != AppPaletteStyle.FIDELITY) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(RStrings.custom_color_style_hint, style.label()),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onUseFidelity, modifier = Modifier.widthIn(max = 160.dp)) {
                            Text(stringResource(RStrings.custom_color_use_fidelity), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            val compactButtonPadding = if (LocalDensity.current.fontScale > 1.2f) {
                PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            } else null
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = if (wide) 0.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    contentPadding = compactButtonPadding ?: ButtonDefaults.TextButtonContentPadding,
                ) {
                    Text(stringResource(RStrings.cancel))
                }
                Button(
                    onClick = { hexField.text.toString().parseColorHex()?.let(onApply) },
                    enabled = hexField.text.toString().parseColorHex() != null,
                    contentPadding = compactButtonPadding ?: ButtonDefaults.ContentPadding,
                ) {
                    Text(stringResource(RStrings.apply))
                }
            }
        }
    }
}

@Composable
private fun ColorInputs(
    selectedColor: Color,
    controller: ColorPickerController,
    hexField: TextFieldState,
    onWheelColorChange: (Color) -> Unit,
) {
    val wheelDescription = stringResource(RStrings.custom_color_wheel)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            HsvColorPicker(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .semantics { contentDescription = wheelDescription },
                controller = controller,
                initialColor = selectedColor,
                onColorChanged = { color ->
                    if (color.fromUser) onWheelColorChange(color.color)
                },
            )
        }
        val sampleWidth = (128.dp * LocalDensity.current.fontScale).coerceIn(128.dp, 176.dp)
        Column(
            modifier = Modifier.width(sampleWidth),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val circle = Modifier.size(56.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clip(CircleShape)
                ColorSample(stringResource(RStrings.custom_color_selected), Modifier.weight(1f)) {
                    Box(circle.background(selectedColor))
                }
                ColorSample(stringResource(RStrings.custom_color_generated), Modifier.weight(1f)) {
                    PaletteDisc(MaterialTheme.colorScheme, circle)
                }
            }
            OutlinedTextField(
                state = hexField,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(RStrings.custom_color_hex)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ColorBrightness(selectedColor: Color, controller: ColorPickerController) {
    val brightnessDescription = stringResource(RStrings.custom_color_brightness)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.BrightnessMedium, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        BrightnessSlider(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .semantics {
                    contentDescription = brightnessDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        maxOf(selectedColor.red, selectedColor.green, selectedColor.blue),
                        0f..1f,
                    )
                    setProgress { value ->
                        controller.setBrightness(value.coerceIn(0f, 1f), fromUser = true)
                        true
                    }
                },
            controller = controller,
            borderRadius = 16.dp,
            borderSize = 1.dp,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            initialColor = selectedColor,
        )
    }
}

@Composable
private fun ColorSample(label: String, modifier: Modifier = Modifier, sample: @Composable () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        sample()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(hyphens = Hyphens.Auto),
            textAlign = TextAlign.Center,
        )
    }
}

private fun Long.toColorHex(): String = "#" + (this and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

private val ColorHexPattern = Regex("#[0-9a-fA-F]{6}")

private fun String.parseColorHex(): Long? =
    if (matches(ColorHexPattern)) 0xFF000000 or substring(1).toLong(16) else null
