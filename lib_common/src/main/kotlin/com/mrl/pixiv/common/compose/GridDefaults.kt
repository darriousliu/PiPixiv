package com.mrl.pixiv.common.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastExpanded
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.layout.isWidthCompact
import com.mrl.pixiv.common.kts.spaceBy

@Stable
data class GridLayoutParams(
    val gridCells: GridCells,
    val horizontalArrangement: Arrangement.Horizontal,
    val verticalArrangement: Arrangement.Vertical,
    val cardShape: Shape,
)

@Stable
data class StaggeredGridLayoutParams(
    val gridCells: StaggeredGridCells,
    val horizontalArrangement: Arrangement.Horizontal,
    val verticalArrangement: Arrangement.Vertical,
    val cardShape: Shape,
)

object RecommendGridDefaults {
    @Composable
    fun coverLayoutParameters(windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo()): StaggeredGridLayoutParams {
        val windowSizeClass = windowAdaptiveInfo.windowSizeClass

        val horizontalArrangement = when {
            windowSizeClass.isWidthAtLeastExpanded -> 9f.spaceBy
            windowSizeClass.isWidthAtLeastMedium -> 7f.spaceBy
            else -> 5f.spaceBy
        }

        return StaggeredGridLayoutParams(
            gridCells = when {
                windowSizeClass.isWidthCompact -> StaggeredGridCells.Fixed(2)
                else -> StaggeredGridCells.Adaptive(minSize = 150.dp)
            },
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = horizontalArrangement,
            cardShape = MaterialTheme.shapes.medium
        )
    }
}