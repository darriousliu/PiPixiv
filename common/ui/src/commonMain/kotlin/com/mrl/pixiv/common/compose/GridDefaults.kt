package com.mrl.pixiv.common.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.layout.PaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastExpanded
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.layout.isWidthCompact
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow

object IllustGridSpacing {
    val LabeledColumn = 12.dp
    val LabeledRow = 16.dp
    val Thumbnail = 8.dp

    fun edgePadding(paneInfo: PaneLayoutInfo): Dp =
        if (paneInfo.sizeClass.isWidthAtLeastMedium) 24.dp else 16.dp
}

@Stable
data class GridLayoutParams(
    val gridCells: GridCells,
    val horizontalArrangement: Arrangement.Horizontal,
    val verticalArrangement: Arrangement.Vertical,
    val edgePadding: Dp,
)

@Stable
data class StaggeredGridLayoutParams(
    val gridCells: StaggeredGridCells,
    val horizontalArrangement: Arrangement.Horizontal,
    val verticalArrangement: Arrangement.Vertical,
    val edgePadding: Dp,
)

object RecommendGridDefaults {
    @Composable
    fun coverLayoutParameters(paneInfo: PaneLayoutInfo = currentPaneLayoutInfo()): StaggeredGridLayoutParams {
        val spanCountPortrait by requireUserPreferenceFlow.collectAsStateWithLifecycle { spanCountPortrait }
        val spanCountLandscape by requireUserPreferenceFlow.collectAsStateWithLifecycle { spanCountLandscape }
        return coverLayoutParameters(paneInfo, spanCountPortrait, spanCountLandscape)
    }

    internal fun coverLayoutParameters(
        paneInfo: PaneLayoutInfo,
        spanCountPortrait: Int,
        spanCountLandscape: Int,
    ): StaggeredGridLayoutParams {
        val isPortrait = paneInfo.size.height >= paneInfo.size.width
        val spanCount = if (isPortrait) spanCountPortrait else spanCountLandscape

        return StaggeredGridLayoutParams(
            gridCells = when {
                spanCount < 0 -> StaggeredGridCells.Adaptive(minSize = 150.dp)
                paneInfo.isSplit -> ConstrainedStaggeredGridCells(preferredCount = spanCount, minSize = 150.dp)
                else -> StaggeredGridCells.Fixed(spanCount.coerceAtLeast(1))
            },
            horizontalArrangement = Arrangement.spacedBy(IllustGridSpacing.LabeledColumn),
            verticalArrangement = Arrangement.spacedBy(IllustGridSpacing.LabeledRow),
            edgePadding = IllustGridSpacing.edgePadding(paneInfo),
        )
    }
}

object IllustGridDefaults {
    @Composable
    fun relatedLayoutParameters(paneInfo: PaneLayoutInfo = currentPaneLayoutInfo()): GridLayoutParams {
        val spanCountPortrait by requireUserPreferenceFlow.collectAsStateWithLifecycle { spanCountPortrait }
        val spanCountLandscape by requireUserPreferenceFlow.collectAsStateWithLifecycle { spanCountLandscape }
        return relatedLayoutParameters(paneInfo, spanCountPortrait, spanCountLandscape)
    }

    internal fun relatedLayoutParameters(
        paneInfo: PaneLayoutInfo,
        spanCountPortrait: Int,
        spanCountLandscape: Int,
    ): GridLayoutParams {
        val isPortrait = paneInfo.size.height >= paneInfo.size.width
        val spanCount = if (isPortrait) spanCountPortrait else spanCountLandscape

        return GridLayoutParams(
            gridCells = when {
                spanCount < 0 -> GridCells.Adaptive(minSize = 150.dp)
                paneInfo.isSplit -> ConstrainedGridCells(preferredCount = spanCount, minSize = 150.dp)
                else -> GridCells.Fixed(spanCount.coerceAtLeast(1))
            },
            horizontalArrangement = Arrangement.spacedBy(IllustGridSpacing.Thumbnail),
            verticalArrangement = Arrangement.spacedBy(IllustGridSpacing.Thumbnail),
            edgePadding = IllustGridSpacing.edgePadding(paneInfo),
        )
    }

    fun userWorkCells(paneWidth: Dp, isSplit: Boolean): GridCells =
        if (paneWidth < 600.dp) {
            ConstrainedGridCells(preferredCount = 3, minSize = if (isSplit) 120.dp else 96.dp)
        } else {
            GridCells.Adaptive(minSize = 120.dp)
        }

    @Composable
    fun userFollowingParameters(paneInfo: PaneLayoutInfo = currentPaneLayoutInfo()): GridLayoutParams {
        val windowSizeClass = paneInfo.sizeClass
        val horizontalArrangement = when {
            windowSizeClass.isWidthAtLeastExpanded -> 7f.spaceBy
            windowSizeClass.isWidthAtLeastMedium -> 5f.spaceBy
            else -> 5f.spaceBy
        }
        return GridLayoutParams(
            gridCells = when {
                windowSizeClass.isWidthAtLeastExpanded -> ConstrainedGridCells(3, minSize = 260.dp)
                windowSizeClass.isWidthAtLeastMedium -> ConstrainedGridCells(2, minSize = 260.dp)
                windowSizeClass.isWidthCompact -> GridCells.Fixed(1)
                else -> GridCells.Fixed(1)
            },
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = horizontalArrangement,
            edgePadding = IllustGridSpacing.edgePadding(paneInfo),
        )
    }
}

object BlockingGridDefaults {
    @Composable
    fun blockingLayoutParameters(paneInfo: PaneLayoutInfo = currentPaneLayoutInfo()): GridLayoutParams {
        val windowSizeClass = paneInfo.sizeClass

        val horizontalArrangement = when {
            windowSizeClass.isWidthAtLeastExpanded -> 7f.spaceBy
            windowSizeClass.isWidthAtLeastMedium -> 5f.spaceBy
            else -> 5f.spaceBy
        }
        return GridLayoutParams(
            gridCells = when {
                windowSizeClass.isWidthAtLeastExpanded -> ConstrainedGridCells(4, minSize = 220.dp)
                windowSizeClass.isWidthAtLeastMedium -> ConstrainedGridCells(2, minSize = 260.dp)
                windowSizeClass.isWidthCompact -> GridCells.Fixed(1)
                else -> GridCells.Fixed(1)
            },
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = horizontalArrangement,
            edgePadding = IllustGridSpacing.edgePadding(paneInfo),
        )
    }
}

/** Treat a fixed-column preference as an upper bound when a grid is in a narrower pane. */
private data class ConstrainedGridCells(
    val preferredCount: Int,
    val minSize: Dp,
) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val count = constrainedColumnCount(availableSize, spacing, minSize.roundToPx(), preferredCount)
        return with(GridCells.Fixed(count)) { calculateCrossAxisCellSizes(availableSize, spacing) }
    }
}

private data class ConstrainedStaggeredGridCells(
    val preferredCount: Int,
    val minSize: Dp,
) : StaggeredGridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): IntArray {
        val count = constrainedColumnCount(availableSize, spacing, minSize.roundToPx(), preferredCount)
        return with(StaggeredGridCells.Fixed(count)) {
            calculateCrossAxisCellSizes(availableSize, spacing)
        }
    }
}

private fun constrainedColumnCount(
    availableSize: Int,
    spacing: Int,
    minSize: Int,
    preferredCount: Int,
): Int = ((availableSize + spacing) / (minSize + spacing).coerceAtLeast(1))
    .coerceAtLeast(1)
    .coerceAtMost(preferredCount.coerceAtLeast(1))
