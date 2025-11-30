package com.mrl.pixiv.picture.components

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.util.throttleClick
import kotlinx.collections.immutable.ImmutableList

@Composable
fun UgoiraPlayer(
    images: ImmutableList<Pair<ImageBitmap, Long>>,
    placeholder: VectorPainter,
    downloadUgoira: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (images.isNotEmpty()) {
        var playUgoira by remember { mutableStateOf(false) }
        if (playUgoira) {
            val infiniteTransition = rememberInfiniteTransition(label = "ugoiraPlayerTransition")
            val currentIndex by infiniteTransition.animateValue(
                initialValue = 0,
                targetValue = images.size,
                typeConverter = Int.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = images.sumOf { it.second }.toInt()
                        images.forEachIndexed { index, pair ->
                            if (index == 0) {
                                0 at 0
                            } else {
                                index at images.subList(0, index).sumOf { it.second }.toInt()
                            }
                        }
                    }
                ),
                label = "ugoiraPlayerTransition"
            )
            Image(
                bitmap = images[currentIndex].first,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = modifier
                    .fillMaxWidth()
                    .throttleClick(
                        onLongClick = downloadUgoira
                    ) {
                        playUgoira = false
                    },
            )
        } else {
            Box(modifier = modifier) {
                Image(
                    bitmap = images[0].first,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .throttleClick(
                            onLongClick = downloadUgoira
                        ),
                )
                IconButton(
                    onClick = { playUgoira = true },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(4.dp, shape = CircleShape),
                    )
                }
            }
        }
    } else {
        Image(
            painter = placeholder,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = modifier
                .fillMaxWidth(),
        )
    }
}

