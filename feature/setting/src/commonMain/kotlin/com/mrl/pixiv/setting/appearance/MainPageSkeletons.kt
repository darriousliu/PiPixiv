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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 对应 feature/main/.../home/HomeScreen.kt 的 HomeScaffold；修改其框架时同步此骨架。 */
@Composable
internal fun HomeSkeleton(spanCount: Int, navigationInset: Dp) {
    PageWithFab(navigationInset) {
        Column {
            SkeletonTopBar(Icons.Rounded.PersonSearch, Icons.Rounded.Refresh)
            SkeletonTabs(2)
            ArtworkGrid(spanCount)
        }
    }
}

/** 对应 feature/main/.../ranking/RankingScreen.kt 的 RankingScreen；修改其框架时同步此骨架。 */
@Composable
internal fun RankingSkeleton(spanCount: Int, navigationInset: Dp) {
    PageWithFab(navigationInset) {
        Column {
            SkeletonTopBar()
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonTabs(4, Modifier.weight(1f))
                ActionIcon(Icons.Rounded.EditCalendar)
            }
            ArtworkGrid(spanCount)
        }
    }
}

/** 对应 feature/main/.../latest/LatestScreen.kt 的 LatestScreen；修改其框架时同步此骨架。 */
@Composable
internal fun LatestSkeleton(spanCount: Int, navigationInset: Dp) {
    PageWithFab(navigationInset) {
        Column {
            SkeletonTabs(3, Modifier.padding(horizontal = 16.dp))
            Box {
                ArtworkGrid(spanCount, Modifier.padding(top = 40.dp))
                Row(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(3) { ChipSkeleton(selected = it == 0) }
                }
            }
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
private fun SkeletonTabs(count: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(48.dp)) {
        HorizontalDivider(Modifier.align(Alignment.BottomCenter))
        Row(Modifier.fillMaxSize()) {
            repeat(count) { index ->
                Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                    PlaceholderBar(36.dp, 12.dp, if (index == 0) colors.primary else colors.onSurfaceVariant)
                    if (index == 0) {
                        Spacer(
                            Modifier.align(Alignment.BottomCenter).size(36.dp, 3.dp)
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
private fun ArtworkGrid(spanCount: Int, modifier: Modifier = Modifier) {
    val columns = if (spanCount == -1) 2 else spanCount.coerceIn(1, 4)
    Row(
        modifier = modifier.fillMaxWidth().clipToBounds().padding(start = 16.dp, top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(columns) { column ->
            Column(
                modifier = Modifier.weight(1f).wrapContentHeight(Alignment.Top, unbounded = true),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(5) { row ->
                    val height = cardHeights[(row * columns + column) % cardHeights.size]
                    ArtworkCard(height)
                }
            }
        }
    }
}

@Composable
private fun ArtworkCard(height: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(
            Modifier.fillMaxWidth().height(height)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
        )
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlaceholderBar(64.dp, 10.dp)
            PlaceholderBar(40.dp, 8.dp)
        }
    }
}

@Composable
private fun ChipSkeleton(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    val modifier = if (selected) {
        Modifier.background(colors.secondaryContainer, shape)
    } else {
        Modifier.background(colors.surface, shape).border(1.dp, colors.outline, shape)
    }
    Box(modifier.size(76.dp, 32.dp), contentAlignment = Alignment.Center) {
        PlaceholderBar(36.dp, 8.dp, if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .background(colors.surfaceContainerHigh, MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                    PlaceholderBar(72.dp, 12.dp)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaceholderBar(72.dp, 16.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { ChipSkeleton(selected = false) }
                }
            }
            PlaceholderBar(90.dp, 18.dp)
            repeat(5) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Spacer(
                            Modifier.weight(1f).aspectRatio(1f)
                                .background(colors.surfaceContainerHigh, MaterialTheme.shapes.medium)
                        )
                    }
                }
            }
        }
    }
}

/** 对应 feature/main/.../profile/ProfileScreen.kt 的 ProfileContent；修改其框架时同步此骨架。 */
@Composable
internal fun ProfileSkeleton() {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().clipToBounds().wrapContentHeight(Alignment.Top, unbounded = true)) {
        SkeletonTopBar(Icons.Rounded.Palette)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(Modifier.size(64.dp).background(colors.surfaceContainerHigh, CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlaceholderBar(76.dp, 16.dp)
                        PlaceholderBar(56.dp, 8.dp)
                        PlaceholderBar(68.dp, 8.dp, colors.primary)
                    }
                    ProfileArrow()
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
    Column(Modifier.fillMaxWidth().background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)) {
        icons.forEachIndexed { index, icon ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = 56.dp, end = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(16.dp))
                PlaceholderBar(64.dp, 12.dp)
                Spacer(Modifier.weight(1f))
                ProfileArrow()
            }
        }
    }
}

@Composable
private fun ProfileArrow() {
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
