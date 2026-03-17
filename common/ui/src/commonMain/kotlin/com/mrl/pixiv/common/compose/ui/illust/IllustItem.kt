package com.mrl.pixiv.common.compose.ui.illust

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mrl.pixiv.common.animation.DefaultAnimationDuration
import com.mrl.pixiv.common.animation.DefaultFloatAnimationSpec
import com.mrl.pixiv.common.compose.FavoriteDualColor
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastExpanded
import com.mrl.pixiv.common.compose.lightBlue
import com.mrl.pixiv.common.compose.ui.IllustBottomBookmarkSheet
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.round
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.allowRgb565
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.long_click_to_edit_favorite
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Composable
fun SquareIllustItem(
    illust: Illust,
    isBookmarked: Boolean,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
    navToPictureScreen: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = 5.dp,
    shouldShowTip: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium,
    enableTransition: Boolean = !currentWindowAdaptiveInfo().isWidthAtLeastExpanded,
) {
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showPopupTip by rememberSaveable { mutableStateOf(false) }
    val prefix = rememberSaveable(enableTransition) { Uuid.random().toHexString() }
    val isIllustBlocked = BlockingRepositoryV2.collectIllustBlockAsState(illust.id)
    val isUserBlocked = BlockingRepositoryV2.collectUserBlockAsState(illust.user.id)
    val enableTransition = enableTransition && !isIllustBlocked && !isUserBlocked
    val onClick = {
        navToPictureScreen(prefix, enableTransition)
    }
    LaunchedEffect(Unit) {
        showPopupTip =
            shouldShowTip && !SettingRepository.userPreferenceFlow.value.hasShowBookmarkTip
    }
    val animatedContentScope = LocalNavAnimatedContentScope.current

    with(LocalSharedTransitionScope.current) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .sharedBounds(
                    rememberSharedContentState(key = "${prefix}-card-${illust.id}"),
                    animatedContentScope,
                    enter = fadeIn(DefaultFloatAnimationSpec),
                    exit = fadeOut(DefaultFloatAnimationSpec),
                    boundsTransform = { _, _ -> tween(DefaultAnimationDuration) },
//                    renderInOverlayDuringTransition = false
                )
                .shadow(elevation, shape)
                .background(MaterialTheme.colorScheme.background)
                .throttleClick { onClick() }
        ) {
            val imageKey = illust.imageUrls.squareMedium
            AsyncImage(
                modifier = Modifier
                    .matchParentSize()
                    .sharedElement(
                        rememberSharedContentState(key = "${prefix}-$imageKey"),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        placeholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                    )
                    .conditionally(isIllustBlocked || isUserBlocked) {
                        blur(50.dp, BlurredEdgeTreatment(shape))
                    },
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(illust.imageUrls.squareMedium)
                    .crossfade(1.seconds.inWholeMilliseconds.toInt())
                    .allowRgb565(true)
                    .placeholderMemoryCacheKey(imageKey)
                    .memoryCacheKey(imageKey)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (illust.illustAIType == AiType.AiGeneratedWorks) {
                    AIBadge()
                }
                if (illust.type == Type.Ugoira) {
                    GifBadge()
                }
                if (illust.pageCount > 1) {
                    PageBadge(
                        pageCount = illust.pageCount,
                    )
                }
            }
            if (!isUserBlocked && !isIllustBlocked) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    IconButton(
                        onClick = throttleClick {
                            val restrict =
                                if (requireUserPreferenceValue.defaultPrivateBookmark) Restrict.PRIVATE else Restrict.PUBLIC
                            onBookmarkClick(restrict, null, false)
                        },
                        onLongClick = { showBottomSheet = true },
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "",
                            modifier = Modifier.size(24.dp),
                            tint = FavoriteDualColor(isBookmarked)
                        )
                    }
                    if (showPopupTip) {
                        LaunchedEffect(Unit) {
                            SettingRepository.setHasShowBookmarkTip(true)
                            delay(3000)
                            showPopupTip = false
                        }
                        Popup(
                            alignment = Alignment.TopCenter,
                            offset = IntOffset(x = 0, y = -100)
                        ) {
                            Text(
                                text = stringResource(RStrings.long_click_to_edit_favorite),
                                modifier = Modifier
                                    .background(lightBlue, MaterialTheme.shapes.small)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    if (showBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState(true)
        IllustBottomBookmarkSheet(
            hideBottomSheet = { showBottomSheet = false },
            illust = illust,
            bottomSheetState = bottomSheetState,
            onBookmarkClick = onBookmarkClick,
        )
    }
}

@Composable
fun RectangleIllustItem(
    navToPictureScreen: (String, Boolean) -> Unit,
    illust: Illust,
    isBookmarked: Boolean,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enableTransition: Boolean = !currentWindowAdaptiveInfo().isWidthAtLeastExpanded,
) {
    val scale = illust.width * 1.0f / illust.height
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current
    val prefix = rememberSaveable(enableTransition) { Uuid.random().toHexString() }
    val isIllustBlocked = BlockingRepositoryV2.collectIllustBlockAsState(illust.id)
    val isUserBlocked = BlockingRepositoryV2.collectUserBlockAsState(illust.user.id)
    val enableTransition = enableTransition && !isIllustBlocked && !isUserBlocked
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val onBookmarkLongClick = {
        showBottomSheet = true
    }
    val context = LocalPlatformContext.current

    with(sharedTransitionScope) {
        val shape = 10f.round
        Box(
            modifier = modifier
                .sharedBounds(
                    rememberSharedContentState(key = "${prefix}-card-${illust.id}"),
                    animatedContentScope,
                    enter = fadeIn(DefaultFloatAnimationSpec),
                    exit = fadeOut(DefaultFloatAnimationSpec),
                    boundsTransform = { _, _ -> tween(DefaultAnimationDuration) },
//                    renderInOverlayDuringTransition = false
                )
                .padding(horizontal = 5.dp)
                .padding(bottom = 5.dp)
                .throttleClick {
                    navToPictureScreen(prefix, enableTransition)
                }
                .shadow(4.dp, shape, clip = false)
                .background(color = MaterialTheme.colorScheme.surface, shape = shape)
                .clip(shape),
        ) {
            Column {
                val imageKey = illust.imageUrls.medium
                val imageShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                AsyncImage(
                    model = remember {
                        ImageRequest.Builder(context)
                            .data(illust.imageUrls.medium)
                            .allowRgb565(true)
                            .crossfade(1.seconds.inWholeMilliseconds.toInt())
                            .placeholderMemoryCacheKey(imageKey)
                            .memoryCacheKey(imageKey)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(scale)
                        .conditionally(isIllustBlocked || isUserBlocked) {
                            blur(50.dp, BlurredEdgeTreatment(imageShape))
                        }
                        .clip(imageShape)
                        .sharedElement(
                            sharedTransitionScope.rememberSharedContentState(key = "${prefix}-$imageKey"),
                            animatedVisibilityScope = animatedContentScope,
                            placeholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                        ),
                    alignment = Alignment.TopCenter,
                )
                Row(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = illust.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = illust.user.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = throttleClick {
                            val restrict =
                                if (requireUserPreferenceValue.defaultPrivateBookmark) Restrict.PRIVATE else Restrict.PUBLIC
                            onBookmarkClick(restrict, null, false)
                        },
                        onLongClick = onBookmarkLongClick,
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "",
                            modifier = Modifier.size(24.dp),
                            tint = FavoriteDualColor(isBookmarked)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (illust.illustAIType == AiType.AiGeneratedWorks) {
                    AIBadge()
                }
                if (illust.type == Type.Ugoira) {
                    GifBadge()
                }
                if (illust.pageCount > 1) {
                    PageBadge(
                        pageCount = illust.pageCount,
                    )
                }
            }
        }
    }
    if (showBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState(true)
        IllustBottomBookmarkSheet(
            hideBottomSheet = { showBottomSheet = false },
            illust = illust,
            bottomSheetState = bottomSheetState,
            onBookmarkClick = onBookmarkClick,
        )
    }
}


@Composable
internal fun AIBadge(
    modifier: Modifier = Modifier
) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = "AI",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun GifBadge(
    modifier: Modifier = Modifier
) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = "GIF",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PageBadge(
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Badge(
        modifier = modifier,
        containerColor = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        5f.HSpacer
        Icon(
            imageVector = Icons.Rounded.FileCopy,
            contentDescription = null,
            modifier = Modifier.size(10.dp)
        )
        2f.HSpacer
        Text(
            text = "$pageCount",
            modifier = Modifier.padding(vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
        5f.HSpacer
    }
}
