package com.mrl.pixiv.setting.appearance

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.router.MainPage
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.appearance_preview
import com.mrl.pixiv.strings.home
import com.mrl.pixiv.strings.my
import com.mrl.pixiv.strings.new_artworks
import com.mrl.pixiv.strings.ranking
import com.mrl.pixiv.strings.search
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val previewPageLabels = mapOf<MainPage, StringResource>(
    MainPage.Home to RStrings.home,
    MainPage.Ranking to RStrings.ranking,
    MainPage.Latest to RStrings.new_artworks,
    MainPage.Search to RStrings.search,
    MainPage.Profile to RStrings.my,
)

internal val previewPages: List<MainPage> = previewPageLabels.keys.toList()

@Composable
internal fun AppearancePreview(
    colorScheme: ColorScheme,
    useLiquidGlassNavigation: Boolean,
    spanCount: Int,
    selectedPage: MainPage,
    onSelectPage: (MainPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(RStrings.appearance_preview)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
    ) {
        Surface(
            modifier = modifier.aspectRatio(0.5f).semantics { contentDescription = description },
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant),
        ) {
            Box(Modifier.scaledPhoneCanvas()) {
                val navigationInset = if (useLiquidGlassNavigation) 76.dp else 80.dp
                Crossfade(
                    targetState = selectedPage,
                    modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
                ) { page ->
                    when (page) {
                        MainPage.Home -> HomeSkeleton(spanCount, navigationInset)
                        MainPage.Ranking -> RankingSkeleton(spanCount, navigationInset)
                        MainPage.Latest -> LatestSkeleton(spanCount, navigationInset)
                        MainPage.Search -> SearchSkeleton(navigationInset)
                        MainPage.Profile -> ProfileSkeleton()
                    }
                }
                PreviewNavigation(
                    floating = useLiquidGlassNavigation,
                    selectedPage = selectedPage,
                    onSelectPage = onSelectPage,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/** 固定骨架的测量尺寸，再整体缩放绘制和点击坐标。 */
private fun Modifier.scaledPhoneCanvas() = layout { measurable, constraints ->
    val width = 360.dp.roundToPx()
    val height = 720.dp.roundToPx()
    val child = measurable.measure(Constraints.fixed(width, height))
    val scale = minOf(constraints.maxWidth.toFloat() / width, constraints.maxHeight.toFloat() / height)
    layout((width * scale).roundToInt(), (height * scale).roundToInt()) {
        child.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

@Composable
private fun PreviewNavigation(
    floating: Boolean,
    selectedPage: MainPage,
    onSelectPage: (MainPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val bar = if (floating) {
        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .height(64.dp).background(colors.surfaceContainer, CircleShape)
            .padding(horizontal = 6.dp)
    } else {
        Modifier.height(80.dp).background(colors.surfaceContainer)
    }
    Row(modifier.fillMaxWidth().then(bar).selectableGroup()) {
        previewPages.forEach { page ->
            val selected = page == selectedPage
            val tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = { onSelectPage(page) },
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier.size(48.dp, 32.dp).then(
                        if (selected) Modifier.background(colors.secondaryContainer, CircleShape) else Modifier
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, contentDescription = stringResource(previewPageLabels.getValue(page)), tint = tint)
                }
                Spacer(Modifier.size(22.dp, 8.dp).background(tint, CircleShape))
            }
        }
    }
}
