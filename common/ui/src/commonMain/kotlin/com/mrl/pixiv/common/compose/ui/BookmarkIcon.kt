package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.FavoriteDualColor

@Composable
fun BookmarkIcon(
    isBookmarked: Boolean,
    isPrivate: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    bookmarkedImageVector: ImageVector = Icons.Rounded.Favorite,
    unbookmarkedImageVector: ImageVector = Icons.Rounded.FavoriteBorder,
    tint: Color = FavoriteDualColor(isBookmarked),
    contentDescription: String? = null,
) {
    Box(modifier = modifier.size(iconSize)) {
        Icon(
            imageVector = if (isBookmarked) bookmarkedImageVector else unbookmarkedImageVector,
            contentDescription = contentDescription,
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize),
            tint = tint,
        )
        if (isBookmarked && isPrivate) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(iconSize * 0.46f)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(1.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
