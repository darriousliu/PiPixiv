package com.mrl.pixiv.search.preview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mrl.pixiv.common.data.search.TrendingTag
import com.mrl.pixiv.common.util.throttleClick

@Composable
fun TrendingItem(
    trendingTag: TrendingTag,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val imageUrl = trendingTag.illust.imageUrls.squareMedium
    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Box(Modifier.throttleClick { onSearch(trendingTag.tag) }) {
            AsyncImage(
                model = remember(context, imageUrl) {
                    ImageRequest.Builder(context).data(imageUrl).crossfade(1000).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            val scrim = MaterialTheme.colorScheme.scrim
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.7f)
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.75f to scrim, 1f to scrim))
            )
            Column(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 8.dp)) {
                Text(
                    text = "#${trendingTag.tag}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val translation = trendingTag.translatedName
                if (!translation.isNullOrBlank()) {
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
