package com.mrl.pixiv.image.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import kotlin.math.abs

@Composable
fun ImagePreviewScreen(
    imageUrls: List<String>,
    initialIndex: Int,
    sharedElementKey: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.isEmpty()) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex),
        pageCount = { imageUrls.size }
    )
    val errorImage = rememberVectorPainter(Icons.Rounded.ErrorOutline)
    val pageLoadingStates = remember(imageUrls) { mutableStateMapOf<Int, Boolean>() }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current
    val safeInitialIndex = initialIndex.coerceIn(0, imageUrls.lastIndex)
    val dismissDistance = with(LocalDensity.current) { 120.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val backgroundAlpha = (1f - abs(dragOffsetY) / (dismissDistance * 3f))
        .coerceIn(0.45f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .pointerInput(onBack, dismissDistance) {
                detectDragGestures(
                    onDragCancel = {
                        dragOffsetY = 0f
                    },
                    onDragEnd = {
                        if (abs(dragOffsetY) > dismissDistance) {
                            onBack()
                        }
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        if (abs(dragAmount.y) > abs(dragAmount.x)) {
                            change.consume()
                            dragOffsetY += dragAmount.y
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffsetY
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val isPageLoading = pageLoadingStates[page] ?: true
                val imageModifier = with(sharedTransitionScope) {
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (sharedElementKey != null && page == safeInitialIndex) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(sharedElementKey),
                                    animatedVisibilityScope = animatedContentScope,
                                )
                            } else {
                                Modifier
                            }
                        )
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    CoilZoomAsyncImage(
                        model = imageUrls[page],
                        contentDescription = null,
                        modifier = imageModifier,
                        contentScale = ContentScale.Fit,
                        error = errorImage,
                        onLoading = {
                            pageLoadingStates[page] = true
                        },
                        onSuccess = {
                            pageLoadingStates[page] = false
                        },
                        onError = {
                            pageLoadingStates[page] = false
                        },
                    )
                    if (isPageLoading) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (imageUrls.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}
