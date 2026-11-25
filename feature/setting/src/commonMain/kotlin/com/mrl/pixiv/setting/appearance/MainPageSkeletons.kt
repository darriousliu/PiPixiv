package com.mrl.pixiv.setting.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 对应 feature/main/.../home/HomeScreen.kt 的 HomeScaffold；修改其框架时同步此骨架。 */
@Composable
internal fun HomeSkeleton(spanCount: Int, navigationInset: Dp) {
    PageWithFab(navigationInset) {
        Column {
            SkeletonTopBar(Icons.Rounded.PersonSearch, Icons.Rounded.Refresh)
            SkeletonTabs(listOf(36.dp, 36.dp))
            ArtworkGrid(spanCount)
        }
    }
}

/** 对应 feature/main/.../ranking/RankingScreen.kt 的 RankingScreen；修改其框架时同步此骨架。 */
@Composable
internal fun RankingSkeleton(spanCount: Int, navigationInset: Dp) {
    PageWithFab(navigationInset) {
        Column {
            SkeletonTopBar(Icons.Rounded.EditCalendar)
            SkeletonTabs(listOf(36.dp, 36.dp, 36.dp, 52.dp), scrollable = true)
            ArtworkGrid(spanCount, showRank = true)
        }
    }
}

/** 对应 feature/main/.../latest/LatestScreen.kt 的 LatestScreen；修改其框架时同步此骨架。 */
@Composable
internal fun LatestSkeleton(spanCount: Int, navigationInset: Dp) {
    PageWithFab(navigationInset) {
        Column {
            SkeletonTopBar()
            SkeletonTabs(listOf(36.dp, 60.dp, 36.dp, 36.dp), scrollable = true)
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { ChipSkeleton(selected = it == 0, shape = CircleShape) }
            }
            ArtworkGrid(spanCount, topPadding = 4.dp)
        }
    }
}

@Composable
private fun PageWithFab(navigationInset: Dp, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().clipToBounds()) {
        content()
        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navigationInset + 16.dp)
                .size(56.dp).background(colors.primaryContainer, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Book, contentDescription = null, tint = colors.onPrimaryContainer)
        }
    }
}

@Composable
private fun SkeletonTopBar(vararg actions: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBar(60.dp, 18.dp)
        Spacer(Modifier.weight(1f))
        actions.forEach { ActionIcon(it) }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SkeletonTabs(labelWidths: List<Dp>, scrollable: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(48.dp).clipToBounds()) {
        HorizontalDivider(Modifier.align(Alignment.BottomCenter))
        Row(Modifier.fillMaxSize().padding(horizontal = if (scrollable) 16.dp else 0.dp)) {
            labelWidths.forEachIndexed { index, width ->
                val tabWidth = if (scrollable) {
                    Modifier.width((width + 32.dp).coerceAtLeast(48.dp))
                } else {
                    Modifier.weight(1f)
                }
                Box(tabWidth.height(48.dp), contentAlignment = Alignment.Center) {
                    PlaceholderBar(width, 12.dp, if (index == 0) colors.primary else colors.onSurfaceVariant)
                    if (index == 0) {
                        Spacer(
                            Modifier.align(Alignment.BottomCenter).size(width, 3.dp)
                                .background(colors.primary, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

private val cardHeights = listOf(180.dp, 240.dp, 200.dp, 160.dp, 220.dp, 260.dp)

@Composable
private fun ArtworkGrid(spanCount: Int, showRank: Boolean = false, topPadding: Dp = 12.dp) {
    val columns = if (spanCount == -1) 2 else spanCount.coerceIn(1, 4)
    Row(
        modifier = Modifier.fillMaxWidth().clipToBounds()
            .padding(start = 16.dp, top = topPadding, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(columns) { column ->
            Column(
                modifier = Modifier.weight(1f).wrapContentHeight(Alignment.Top, unbounded = true),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(5) { row ->
                    val index = row * columns + column
                    ArtworkCard(cardHeights[index % cardHeights.size], rank = if (showRank) index + 1 else null)
                }
            }
        }
    }
}

@Composable
private fun ArtworkCard(height: Dp, rank: Int?) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Column {
            Spacer(Modifier.fillMaxWidth().height(height).background(colors.surfaceContainerHigh))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 6.dp)
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (rank != null) {
                    PlaceholderBar(16.dp, 20.dp, if (rank <= 3) colors.primary else colors.onSurfaceVariant)
                }
                LabelSkeleton(Modifier.weight(1f))
                Spacer(Modifier.size(18.dp).background(colors.onSurfaceVariant, CircleShape))
            }
        }
    }
}

@Composable
private fun LabelSkeleton(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlaceholderBar(64.dp, 10.dp, color)
        PlaceholderBar(40.dp, 8.dp, color)
    }
}

@Composable
private fun ChipSkeleton(
    selected: Boolean,
    shape: Shape,
    labelColor: Color = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    },
) {
    val colors = MaterialTheme.colorScheme
    val modifier = if (selected) {
        Modifier.background(colors.secondaryContainer, shape)
    } else {
        Modifier.background(colors.surface, shape).border(1.dp, colors.outline, shape)
    }
    Box(modifier.size(76.dp, 32.dp), contentAlignment = Alignment.Center) {
        PlaceholderBar(36.dp, 8.dp, labelColor)
    }
}

@Composable
private fun PlaceholderBar(width: Dp, height: Dp, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Spacer(Modifier.size(width, height).background(color, CircleShape))
}

/** 对应 feature/main/.../search/preview/SearchPreviewScreen.kt 的 SearchPreviewContent；修改其框架时同步此骨架。 */
@Composable
internal fun SearchSkeleton(navigationInset: Dp) {
    val colors = MaterialTheme.colorScheme
    PageWithFab(navigationInset) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .wrapContentHeight(Alignment.Top, unbounded = true),
        ) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .background(colors.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                    PlaceholderBar(72.dp, 12.dp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                PlaceholderBar(72.dp, 16.dp)
                Spacer(Modifier.weight(1f))
                PlaceholderBar(36.dp, 10.dp, colors.primary)
            }
            Row(
                modifier = Modifier.heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) {
                    ChipSkeleton(selected = false, shape = MaterialTheme.shapes.small, labelColor = colors.primary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.height(48.dp), contentAlignment = Alignment.CenterStart) {
                PlaceholderBar(90.dp, 16.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(2) {
                            Surface(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                shape = MaterialTheme.shapes.large,
                                color = colors.surfaceContainerHigh,
                                shadowElevation = 1.dp,
                            ) {
                                Box(
                                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                                    contentAlignment = Alignment.BottomStart,
                                ) {
                                    LabelSkeleton(color = colors.surfaceContainerLowest)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 对应 feature/main/.../profile/ProfileScreen.kt 的 ProfileContent；修改其框架时同步此骨架。 */
@Composable
internal fun ProfileSkeleton(navigationInset: Dp) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(bottom = navigationInset).clipToBounds()
            .wrapContentHeight(Alignment.Top, unbounded = true)
    ) {
        SkeletonTopBar(Icons.Rounded.Palette)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.surfaceContainerLow,
                    shadowElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Spacer(Modifier.size(64.dp).background(colors.surfaceContainerHigh, CircleShape))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlaceholderBar(76.dp, 16.dp)
                            PlaceholderBar(56.dp, 8.dp)
                            PlaceholderBar(68.dp, 8.dp, colors.primary)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Icons.Rounded.HowToReg, Icons.Rounded.People).forEach { icon ->
                        Column(
                            modifier = Modifier.weight(1f).height(64.dp)
                                .background(colors.secondaryContainer, CircleShape),
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colors.onSecondaryContainer,
                            )
                            PlaceholderBar(48.dp, 8.dp, colors.onSecondaryContainer)
                        }
                    }
                }
            }
            ProfileMenu(
                Icons.Rounded.Bookmarks,
                Icons.AutoMirrored.Rounded.MenuBook,
                Icons.Rounded.History,
                Icons.Rounded.Download,
            )
            ProfileMenu(Icons.Rounded.Notifications, Icons.Rounded.Settings)
        }
    }
}

@Composable
private fun ProfileMenu(vararg icons: ImageVector) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        icons.forEachIndexed { index, icon ->
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index, icons.size),
                colors = ListItemDefaults.segmentedColors(containerColor = colors.surfaceContainerLow),
                leadingContent = {
                    Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant)
                },
            ) {
                PlaceholderBar(64.dp, 12.dp)
            }
        }
    }
}
