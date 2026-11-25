package com.mrl.pixiv.common.compose.ui.illust

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastExpanded
import com.mrl.pixiv.common.compose.navigationSharedImage
import com.mrl.pixiv.common.compose.theme.tabularNumbers
import com.mrl.pixiv.common.compose.ui.BookmarkIcon
import com.mrl.pixiv.common.compose.ui.IllustBottomBookmarkSheet
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isPrivateBookmark
import com.mrl.pixiv.common.router.LocalNavigationManager
import com.mrl.pixiv.common.router.PictureCoverSource
import com.mrl.pixiv.common.router.PictureImageKey
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.allowRgb565
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.cancel_favorite
import com.mrl.pixiv.strings.liked
import com.mrl.pixiv.strings.long_click_to_edit_favorite
import com.mrl.pixiv.strings.manga
import com.mrl.pixiv.strings.series
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun SquareIllustItem(
    illust: Illust,
    isBookmarked: Boolean,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
    navToPictureScreen: (PictureCoverSource?) -> Unit,
    sharedElementSlot: String,
    modifier: Modifier = Modifier,
    shouldShowTip: Boolean = false,
    shape: Shape = MaterialTheme.shapes.large,
    enableTransition: Boolean = !currentWindowAdaptiveInfoV2().isWidthAtLeastExpanded,
    showLabel: Boolean = false,
) {
    IllustItem(
        illust = illust,
        isBookmarked = isBookmarked,
        onBookmarkClick = onBookmarkClick,
        navToPictureScreen = navToPictureScreen,
        sharedElementSlot = sharedElementSlot,
        modifier = modifier,
        square = true,
        showLabel = showLabel,
        rank = null,
        shape = shape,
        enableTransition = enableTransition,
        shouldShowTip = shouldShowTip,
    )
}

@Composable
fun RectangleIllustItem(
    navToPictureScreen: (PictureCoverSource?) -> Unit,
    illust: Illust,
    isBookmarked: Boolean,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
    sharedElementSlot: String,
    modifier: Modifier = Modifier,
    enableTransition: Boolean = !currentWindowAdaptiveInfoV2().isWidthAtLeastExpanded,
    shouldShowTip: Boolean = false,
    rank: Int? = null,
) {
    IllustItem(
        illust = illust,
        isBookmarked = isBookmarked,
        onBookmarkClick = onBookmarkClick,
        navToPictureScreen = navToPictureScreen,
        sharedElementSlot = sharedElementSlot,
        modifier = modifier,
        square = false,
        showLabel = true,
        rank = rank,
        shape = MaterialTheme.shapes.large,
        enableTransition = enableTransition,
        shouldShowTip = shouldShowTip,
    )
}

@Composable
private fun IllustItem(
    illust: Illust,
    isBookmarked: Boolean,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
    navToPictureScreen: (PictureCoverSource?) -> Unit,
    sharedElementSlot: String,
    modifier: Modifier,
    square: Boolean,
    showLabel: Boolean,
    rank: Int?,
    shape: Shape,
    enableTransition: Boolean,
    shouldShowTip: Boolean,
) {
    val isIllustBlocked = BlockingRepositoryV2.collectIllustBlockAsState(illust.id)
    val isUserBlocked = BlockingRepositoryV2.collectUserBlockAsState(illust.user.id)
    val isBlocked = isIllustBlocked || isUserBlocked
    val imageUrl = illust.frameCoverUrl(square, showLabel)
    val cover = pictureCoverSource(illust, sharedElementSlot, imageUrl, enableTransition && !isBlocked)
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    IllustFrame(
        illust = illust,
        square = square,
        isBlocked = isBlocked,
        isBookmarked = isBookmarked,
        isPrivate = illust.isPrivateBookmark,
        onOpen = { navToPictureScreen(cover) },
        onBookmarkClick = {
            val restrict = if (requireUserPreferenceValue.defaultPrivateBookmark) Restrict.PRIVATE else Restrict.PUBLIC
            onBookmarkClick(restrict, null, false)
        },
        onBookmarkLongClick = { showBottomSheet = true },
        modifier = modifier,
        showLabel = showLabel,
        rank = rank,
        shape = shape,
        sharedImage = cover?.key,
        shouldShowTip = shouldShowTip,
    )

    if (showBottomSheet) {
        IllustBottomBookmarkSheet(
            hideBottomSheet = { showBottomSheet = false },
            illust = illust,
            bottomSheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
            onBookmarkClick = onBookmarkClick,
        )
    }
}

@Composable
internal fun IllustFrame(
    illust: Illust,
    square: Boolean,
    isBlocked: Boolean,
    isBookmarked: Boolean,
    isPrivate: Boolean,
    onOpen: () -> Unit,
    onBookmarkClick: () -> Unit,
    onBookmarkLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = !square,
    rank: Int? = null,
    shape: Shape = MaterialTheme.shapes.large,
    sharedImage: PictureImageKey? = null,
    shouldShowTip: Boolean = false,
) {
    val imageUrl = illust.frameCoverUrl(square, showLabel)
    val context = LocalPlatformContext.current
    Column(
        modifier = modifier.throttleClick(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .aspectRatio(if (square) 1f else calculateIllustAspectRatio(illust.width, illust.height))
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = remember(context, imageUrl) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowRgb565(true)
                        .crossfade(1.seconds.inWholeMilliseconds.toInt())
                        .placeholderMemoryCacheKey(imageUrl)
                        .memoryCacheKey(imageUrl)
                        .build()
                },
                contentDescription = null,
                modifier = Modifier.matchParentSize()
                    .navigationSharedImage(sharedImage)
                    .clip(shape)
                    .conditionally(isBlocked) { blur(50.dp, BlurredEdgeTreatment(shape)) },
                contentScale = if (square) ContentScale.Crop else ContentScale.Fit,
                alignment = if (square) Alignment.Center else Alignment.TopCenter,
            )
            IllustInfoPill(illust, Modifier.align(Alignment.TopStart).padding(8.dp))
            if (!isBlocked) {
                BookmarkTooltipBox(shouldShowTip, Modifier.align(Alignment.BottomEnd)) {
                    IllustBookmarkButton(
                        isBookmarked = isBookmarked,
                        isPrivate = isPrivate,
                        onClick = throttleClick(onClick = onBookmarkClick),
                        onLongClick = onBookmarkLongClick,
                    )
                }
            }
        }
        if (showLabel) {
            IllustLabel(illust = illust, rank = rank)
        }
    }
}

// 相关作品将原比例封面裁成方图，打开详情时仍可复用该封面作为首图占位。
private fun Illust.frameCoverUrl(square: Boolean, showLabel: Boolean): String =
    if (square && !showLabel) imageUrls.squareMedium else imageUrls.medium

@Composable
private fun IllustInfoPill(illust: Illust, modifier: Modifier = Modifier) {
    val hasPages = illust.pageCount > 1
    val parts = buildList {
        if (hasPages) add(illust.pageCount.toString())
        if (illust.illustAIType == AiType.AiGeneratedWorks) add("AI")
        if (illust.type == Type.Ugoira) add("GIF")
        if (illust.type == Type.Manga) add(stringResource(RStrings.manga))
        if (illust.series != null) add(stringResource(RStrings.series))
    }
    if (parts.isEmpty()) return

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasPages) {
                Icon(Icons.Rounded.FileCopy, contentDescription = null, modifier = Modifier.size(12.dp))
            }
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall.tabularNumbers,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun IllustBookmarkButton(
    isBookmarked: Boolean,
    isPrivate: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, onLongClick = onLongClick, modifier = modifier.size(48.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BookmarkIcon(
                isBookmarked = isBookmarked,
                isPrivate = isPrivate,
                iconSize = 18.dp,
                contentDescription = stringResource(if (isBookmarked) RStrings.cancel_favorite else RStrings.liked),
            )
        }
    }
}

@Composable
private fun IllustLabel(illust: Illust, rank: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleLargeEmphasized.tabularNumbers,
                color = if (rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = illust.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = illust.user.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun pictureCoverSource(
    illust: Illust,
    slot: String,
    thumbnailUrl: String,
    enabled: Boolean,
): PictureCoverSource? {
    if (!enabled || illust.type == Type.Ugoira || thumbnailUrl.isBlank()) return null
    val entryId = LocalNavigationManager.current?.entryId ?: return null
    return remember(entryId, slot, illust.id, thumbnailUrl) {
        PictureCoverSource(PictureImageKey(entryId, slot, illust.id), thumbnailUrl)
    }
}

internal fun calculateIllustAspectRatio(width: Int, height: Int): Float {
    return width.toFloat() / height
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkTooltipBox(
    shouldShowTip: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)

    LaunchedEffect(shouldShowTip) {
        if (shouldShowTip && !SettingRepository.userPreferenceFlow.value.hasShowBookmarkTip) {
            SettingRepository.setHasShowBookmarkTip(true)
            launch { tooltipState.show() }
            delay(3000.milliseconds)
            tooltipState.dismiss()
        }
    }

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
            ),
            tooltip = {
                PlainTooltip {
                    Text(text = stringResource(RStrings.long_click_to_edit_favorite))
                }
            },
            state = tooltipState,
            enableUserInput = false,
            content = content,
        )
    }
}
