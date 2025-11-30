package com.mrl.pixiv.common.compose.ui.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

internal enum class ImageLoadState {
    LOADING,
    SUCCESS,
    ERROR
}

/**
 * 加载图片
 * @param model 图片资源
 * @param errorContent 加载失败时的内容
 * @param loadingContent 加载中的内容
 */
@Composable
fun LoadingImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    errorContent: @Composable () -> Unit = {},
    loadingContent: @Composable () -> Unit = {}
) {
    var imageLoadState by remember { mutableStateOf(ImageLoadState.LOADING) }
    Box(modifier = modifier) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            onState = {
                imageLoadState = when (it) {
                    is AsyncImagePainter.State.Success -> ImageLoadState.SUCCESS
                    is AsyncImagePainter.State.Error -> ImageLoadState.ERROR
                    is AsyncImagePainter.State.Loading -> ImageLoadState.LOADING
                    else -> imageLoadState // 保持当前状态
                }
            },
            contentScale = contentScale
        )
        when (imageLoadState) {
            ImageLoadState.LOADING -> loadingContent()
            ImageLoadState.ERROR -> errorContent()
            ImageLoadState.SUCCESS -> {}
        }
    }
}