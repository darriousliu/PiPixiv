package com.mrl.pixiv.picture

import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.theme.tabularNumbers
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.home
import com.mrl.pixiv.strings.more
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ArtworkTopBar(
    currentPage: Int,
    pageCount: Int,
    showPage: Boolean,
    isBlocked: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Box(
        modifier = modifier.fillMaxWidth()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(onClick = throttleClick(onClick = onBack), colors = buttonColors) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(RStrings.back))
            }
            FilledTonalIconButton(onClick = throttleClick(onClick = onHome), colors = buttonColors) {
                Icon(Icons.Rounded.Home, contentDescription = stringResource(RStrings.home))
            }
        }
        if (!isBlocked) {
            CapturedAnimatedVisibility(
                visible = showPage,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Text(
                        text = "$currentPage/$pageCount",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.tabularNumbers,
                    )
                }
            }
            FilledTonalIconButton(
                onClick = throttleClick(onClick = onMore),
                modifier = Modifier.align(Alignment.CenterEnd),
                colors = buttonColors,
            ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(RStrings.more))
            }
        }
    }
}
