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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.TextFieldValue
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
import com.mrl.pixiv.common.compose.transparentIndicatorColors
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.IllustAiType
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.illust.BookmarkDetailTag
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.round
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.allowRgb565
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.add_tags
import com.mrl.pixiv.strings.add_to_favorite
import com.mrl.pixiv.strings.bookmark_tags
import com.mrl.pixiv.strings.cancel_favorite
import com.mrl.pixiv.strings.edit_favorite
import com.mrl.pixiv.strings.long_click_to_edit_favorite
import com.mrl.pixiv.strings.max_bookmark_tags_reached
import com.mrl.pixiv.strings.word_private
import com.mrl.pixiv.strings.word_public
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
                if (illust.illustAIType == IllustAiType.AiGeneratedWorks) {
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
                            onBookmarkClick(Restrict.PUBLIC, null, false)
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
        BottomBookmarkSheet(
            hideBottomSheet = { showBottomSheet = false },
            illust = illust,
            bottomSheetState = bottomSheetState,
            onBookmarkClick = onBookmarkClick,
            isBookmarked = isBookmarked
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
                            onBookmarkClick(Restrict.PUBLIC, null, false)
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
                if (illust.illustAIType == IllustAiType.AiGeneratedWorks) {
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
        BottomBookmarkSheet(
            hideBottomSheet = { showBottomSheet = false },
            illust = illust,
            bottomSheetState = bottomSheetState,
            onBookmarkClick = onBookmarkClick,
            isBookmarked = isBookmarked
        )
    }
}

private const val MAX_BOOKMARK_TAGS = 10

@Composable
fun BottomBookmarkSheet(
    hideBottomSheet: () -> Unit,
    illust: Illust,
    bottomSheetState: SheetState,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
    isBookmarked: Boolean,
) {
    var publicSwitch by remember { mutableStateOf(true) }
    val illustBookmarkDetailTags = remember { mutableStateListOf<BookmarkDetailTag>() }
    LaunchedEffect(Unit) {
        val resp = withIOContext { PixivRepository.getIllustBookmarkDetail(illust.id) }
        publicSwitch = resp.bookmarkDetail.restrict == Restrict.PUBLIC.value
        illustBookmarkDetailTags.clear()
        illustBookmarkDetailTags.addAll(resp.bookmarkDetail.tags)
    }
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        modifier = Modifier.imePadding(),
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val allTags = remember(illustBookmarkDetailTags.size) {
            illustBookmarkDetailTags.map { it.name to it.isRegistered }.toMutableStateList()
        }
        val selectedTagsIndex = allTags.indices.filter { allTags[it].second }
        var inputTag by remember { mutableStateOf(TextFieldValue()) }

        Text(
            text = if (isBookmarked) stringResource(RStrings.edit_favorite) else stringResource(
                RStrings.add_to_favorite
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (publicSwitch) stringResource(RStrings.word_public)
                else stringResource(RStrings.word_private)
            )
            Switch(checked = publicSwitch, onCheckedChange = { publicSwitch = it })
        }
        Row(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
//                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(RStrings.bookmark_tags),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedTagsIndex.size} / $MAX_BOOKMARK_TAGS",
                    style = MaterialTheme.typography.labelMedium
                )
                Checkbox(
                    checked = allTags.count { it.second }.let {
                        it == MAX_BOOKMARK_TAGS || it == allTags.size
                    },
                    onCheckedChange = { checked ->
                        if (checked) {
                            (0..<minOf(allTags.size, MAX_BOOKMARK_TAGS)).forEach { index ->
                                allTags[index] = allTags[index].first to true
                            }
                        } else {
                            allTags.indices.forEach { index ->
                                allTags[index] = allTags[index].first to false
                            }
                        }
                    }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputTag,
                onValueChange = { inputTag = it },
                modifier = Modifier.weight(1f),
                enabled = selectedTagsIndex.size < MAX_BOOKMARK_TAGS,
                placeholder = { Text(text = stringResource(RStrings.add_tags)) },
                shape = MaterialTheme.shapes.small,
                colors = transparentIndicatorColors
            )
            IconButton(
                onClick = throttleClick {
                    handleInputTag(inputTag, allTags)
                    inputTag = inputTag.copy(text = "")
                },
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            }
        }
        LazyColumn(
            modifier = Modifier
                .height(LocalWindowInfo.current.containerDpSize.height / 3)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            itemsIndexed(
                items = allTags,
                key = { index, item -> "${index}_${item.first}" }
            ) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .throttleClick(indication = ripple()) {
                            if (item.second) {
                                allTags[index] = item.first to false
                            } else {
                                if (selectedTagsIndex.size < MAX_BOOKMARK_TAGS) {
                                    allTags[index] = item.first to true
                                } else {
                                    ToastUtil.safeShortToast(
                                        RStrings.max_bookmark_tags_reached,
                                        MAX_BOOKMARK_TAGS
                                    )
                                }
                            }
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.first,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Checkbox(
                        checked = item.second,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (selectedTagsIndex.size < MAX_BOOKMARK_TAGS) {
                                    allTags[index] = item.first to true
                                } else {
                                    ToastUtil.safeShortToast(
                                        RStrings.max_bookmark_tags_reached,
                                        MAX_BOOKMARK_TAGS
                                    )
                                }
                            } else {
                                allTags[index] = item.first to false
                            }
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(8.dp),
            horizontalArrangement = 8f.spaceBy,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = throttleClick {
                    onBookmarkClick(
                        if (publicSwitch) Restrict.PUBLIC else Restrict.PRIVATE,
                        selectedTagsIndex.map { allTags[it].first },
                        isBookmarked
                    )
                    hideBottomSheet()
                },
                modifier = Modifier
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = FavoriteDualColor(isBookmarked)
                )
                8f.HSpacer
                Text(
                    text = stringResource(if (isBookmarked) RStrings.edit_favorite else RStrings.add_to_favorite),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (isBookmarked) {
                OutlinedButton(
                    onClick = throttleClick {
                        onBookmarkClick(Restrict.PUBLIC, null, false)
                        hideBottomSheet()
                    },
                ) {
                    Text(
                        text = stringResource(RStrings.cancel_favorite),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun AIBadge(
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

private fun handleInputTag(
    inputTag: TextFieldValue,
    allTags: SnapshotStateList<Pair<String, Boolean>>,
) {
    val tagText = inputTag.text.trim()
    if (tagText.isNotEmpty()) {
        // 检查是否已存在于illustBookmarkDetailTags中
        val existingTagIndex = allTags.indexOfFirst { it.first == tagText }
        if (existingTagIndex != -1) {
            // 如果存在，移动到首位
            val existingTag = allTags[existingTagIndex]
            allTags.remove(existingTag)
            allTags.add(0, existingTag)
        } else {
            // 如果不存在，添加到首位
            allTags.add(0, tagText to true)
        }
    }
}