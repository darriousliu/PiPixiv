// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted liquid optics from the Miuix 0.9.4 example and Kyant0/AndroidLiquidGlass (Apache-2.0).
// Modified for PiPixiv's Material theme, navigation, accessibility, and inset handling.
// https://github.com/compose-miuix-ui/miuix/blob/v0.9.4/example/shared/src/commonMain/kotlin/component/liquid/LiquidGlassNavigationBar.kt
package com.mrl.pixiv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.glass.lens
import com.mrl.pixiv.glass.rememberCombinedBackdrop
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.roundToInt

private val NavigationGlassHighlight = Highlight(
    width = 1.dp,
    alpha = 0.75f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(LightPosition(0.5f, -0.3f, -0.05f), Color.White, 1f),
        secondaryLight = LightSource(LightPosition(0.5f, 0.8f, -0.5f), Color.White, 0.4f),
        dualPeak = true,
    ),
)

private fun BackdropEffectScope.navigationShellOptics() {
    // Reserve the refraction/press reach before blur allocates the backdrop texture.
    padding = maxOf(padding, 40.dp.toPx())
    colorControls(brightness = 0f, contrast = 0.88f, saturation = 1.25f)
    blur(2.5.dp.toPx(), 2.5.dp.toPx())
    lens(refractionHeight = 24.dp.toPx(), refractionAmount = 20.dp.toPx())
}

@Composable
internal fun FloatingGlassNavigationBar(
    selectedPage: MainPage,
    hasNewVersion: Boolean,
    backdrop: LayerBackdrop?,
    onSelectPage: (MainPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val shape = CircleShape
    val selectedIndex =
        mainNavigationItems.indexOfFirst { it.first == selectedPage }.coerceAtLeast(0)
    val scope = rememberCoroutineScope()
    // Keep visual position independent of page recomposition and animate from its current value.
    val motion = remember { LiquidNavigationMotion(selectedIndex, mainNavigationItems.size, scope) }
    LaunchedEffect(selectedIndex) { motion.synchronize(selectedIndex) }
    val pressScale = animateFloatAsState(
        if (motion.isPressed) 78f / 56f else 1f,
        spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "navigationCapsuleScale",
    )
    val haptics = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val tabsBackdrop = if (backdrop != null) rememberLayerBackdrop() else null
    val capsuleBackdrop = if (backdrop != null && tabsBackdrop != null) {
        rememberCombinedBackdrop(backdrop, tabsBackdrop)
    } else null
    val captureLayer = rememberGraphicsLayer()
    val interactionSources = remember {
        List(mainNavigationItems.size) { MutableInteractionSource() }
    }
    val focusedIndex = interactionSources.map { it.collectIsFocusedAsState().value }
        .indexOfFirst { it }
    val surfaceTint = colors.surface.copy(alpha = if (dark) 0.30f else 0.24f)
    val labels = mainNavigationItems.map { (_, title) -> stringResource(title) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier.graphicsLayer {
            translationX = 4.dp.toPx() * motion.panelOffsetFraction
            clip = false
        }.testTag("floating-navigation-shell"),
    ) {
        val slotWidth = (maxWidth - 12.dp) / mainNavigationItems.size
        val labelWidthPx = with(density) { (slotWidth - 4.dp).roundToPx().coerceAtLeast(1) }
        val showLabels = labels.all { label ->
            !textMeasurer.measure(
                text = label,
                style = labelStyle,
                constraints = Constraints(maxWidth = labelWidthPx),
                maxLines = 2,
            ).hasVisualOverflow
        }
        Box(
            Modifier.matchParentSize()
                .shadow(6.dp, shape, clip = false)
                .then(
                    if (backdrop != null) Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = { navigationShellOptics() },
                        highlight = { NavigationGlassHighlight },
                        layerBlock = {
                            val bloom = 1f + 16.dp.toPx() / size.width.coerceAtLeast(1f) *
                                    motion.pressProgress
                            scaleX = bloom
                            scaleY = bloom
                        },
                        onDrawSurface = { drawRect(surfaceTint) },
                    ) else Modifier.background(colors.surface, shape)
                ),
        )
        // The shell stays 64dp at normal font size; the 56dp lens blooms outside it on press.
        Box(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Box(
                Modifier.fillMaxWidth()
                    .testTag("floating-navigation")
                    .liquidNavigationGestures(
                        motion = motion,
                        selectedIndex = selectedIndex,
                        itemCount = mainNavigationItems.size,
                        isRtl = isRtl,
                        onSelect = { onSelectPage(mainNavigationItems[it].first) },
                        onStep = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
                    ),
            ) {
                NavigationTabRow(
                    labels = labels,
                    selectedIndex = selectedIndex,
                    hasNewVersion = hasNewVersion,
                    showLabels = showLabels,
                    tint = colors.onSurface,
                    selectedTint = if (backdrop == null) colors.primary else colors.onSurface,
                    interactionSources = interactionSources,
                    onSelect = { index ->
                        motion.select(index)
                        if (index != selectedIndex) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelectPage(mainNavigationItems[index].first)
                        }
                    },
                )
                if (backdrop != null && tabsBackdrop != null) {
                    // Record the selected-colour row without replaying it. alpha=0 can skip
                    // capture on Skiko, so the source and the consuming lens stay separate.
                    NavigationTabRow(
                        labels = labels,
                        selectedIndex = selectedIndex,
                        hasNewVersion = hasNewVersion,
                        showLabels = showLabels,
                        tint = colors.primary,
                        interactionSources = interactionSources,
                        onSelect = null,
                        modifier = Modifier.matchParentSize()
                            .clearAndSetSemantics {}
                            .drawWithContent {
                                captureLayer.record { this@drawWithContent.drawContent() }
                            }
                            .layerBackdrop(tabsBackdrop)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { shape },
                                effects = { navigationShellOptics() },
                                onDrawSurface = { drawRect(surfaceTint) },
                            ),
                    )
                }
                Box(Modifier.matchParentSize().clearAndSetSemantics {}) {
                    val capsuleTransform: GraphicsLayerScope.() -> Unit = {
                        val stretch = (abs(motion.velocity) / 18f).coerceIn(0f, 1f)
                        scaleX = pressScale.value + 0.10f * stretch
                        scaleY = pressScale.value - 0.045f * stretch
                        clip = false
                    }
                    Box(
                        Modifier.width(slotWidth).fillMaxHeight()
                            .graphicsLayer {
                                // Draw-phase translation retains the live spring position during a page switch.
                                translationX =
                                    slotWidth.toPx() * motion.position * if (isRtl) -1f else 1f
                            }
                            .then(
                                if (capsuleBackdrop != null) Modifier.drawBackdrop(
                                    backdrop = capsuleBackdrop,
                                    shape = { shape },
                                    effects = {
                                        lens(
                                            refractionHeight = 10.dp.toPx() * motion.pressProgress,
                                            refractionAmount = 14.dp.toPx() * motion.pressProgress,
                                            depthEffect = true,
                                        )
                                    },
                                    highlight = {
                                        NavigationGlassHighlight.copy(alpha = motion.pressProgress)
                                    },
                                    layerBlock = capsuleTransform,
                                    onDrawSurface = {
                                        drawRect(colors.primary.copy(alpha = 0.08f * (1f - motion.pressProgress)))
                                    },
                                ) else Modifier.graphicsLayer(capsuleTransform)
                                    .background(colors.primary.copy(alpha = 0.14f), shape)
                            )
                            .testTag("navigation-glass-capsule"),
                    )
                    // Focus is native foreground UI, outside the refracted source.
                    if (focusedIndex >= 0) {
                        Box(
                            Modifier.offset {
                                IntOffset((slotWidth.toPx() * focusedIndex).roundToInt(), 0)
                            }.width(slotWidth).fillMaxHeight()
                                .border(2.dp, colors.primary, shape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationTabRow(
    labels: List<String>,
    selectedIndex: Int,
    hasNewVersion: Boolean,
    showLabels: Boolean,
    tint: Color,
    interactionSources: List<MutableInteractionSource>,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    selectedTint: Color = tint,
) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .navigationContentOutline(MaterialTheme.colorScheme.surface)
            .then(if (onSelect != null) Modifier.selectableGroup() else Modifier),
    ) {
        mainNavigationItems.forEachIndexed { index, (page, _) ->
            val selected = index == selectedIndex
            val contentTint = if (selected) selectedTint else tint
            val label = labels[index]
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val content: @Composable () -> Unit = {
                    Column(
                        modifier = Modifier.fillMaxSize().heightIn(min = 56.dp)
                            .then(
                                if (onSelect == null) Modifier else Modifier
                                    .selectable(
                                        selected = selected,
                                        interactionSource = interactionSources[index],
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { onSelect(index) },
                                    )
                                    .semantics { if (!showLabels) contentDescription = label })
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            2.dp,
                            Alignment.CenterVertically
                        ),
                    ) {
                        MainNavigationIcon(page, hasNewVersion, tint = contentTint)
                        if (showLabels) Text(
                            text = label,
                            color = contentTint,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
                if (onSelect != null) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Above,
                        ),
                        tooltip = { PlainTooltip { Text(label) } },
                        state = rememberTooltipState(),
                        enableUserInput = !showLabels && !selected,
                        content = content,
                    )
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
private fun Modifier.navigationContentOutline(color: Color): Modifier {
    val contentLayer = rememberGraphicsLayer()
    val outlineLayer = rememberGraphicsLayer()
    val outlineFilter = remember(color) { ColorFilter.tint(color) }
    return drawWithContent {
        contentLayer.record { this@drawWithContent.drawContent() }
        outlineLayer.colorFilter = outlineFilter
        outlineLayer.record { drawLayer(contentLayer) }
        // Separate the glyphs from refracted artwork and body text without obscuring the backdrop.
        val width = 1.dp.toPx()
        translate(left = -width) { drawLayer(outlineLayer) }
        translate(left = width) { drawLayer(outlineLayer) }
        translate(top = -width) { drawLayer(outlineLayer) }
        translate(top = width) { drawLayer(outlineLayer) }
        drawLayer(contentLayer)
    }
}
