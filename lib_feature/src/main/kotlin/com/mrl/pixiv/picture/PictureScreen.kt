@file:OptIn(ExperimentalPermissionsApi::class)

package com.mrl.pixiv.picture

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mrl.pixiv.common.animation.DefaultAnimationDuration
import com.mrl.pixiv.common.animation.DefaultFloatAnimationSpec
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.LocalSharedKeyPrefix
import com.mrl.pixiv.common.compose.LocalSharedTransitionScope
import com.mrl.pixiv.common.compose.deepBlue
import com.mrl.pixiv.common.compose.ui.bar.TextSnackbar
import com.mrl.pixiv.common.compose.ui.illust.SquareIllustItem
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.kts.round
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.AppUtil.getString
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.common.util.getScreenHeight
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.viewmodel.follow.FollowState
import com.mrl.pixiv.common.viewmodel.follow.isFollowing
import com.mrl.pixiv.picture.components.UgoiraPlayer
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
fun PictureDeeplinkScreen(
    modifier: Modifier = Modifier,
    illustId: Long,
    pictureViewModel: PictureViewModel = koinViewModel { parametersOf(null, illustId) },
    navigationManager: NavigationManager = koinInject(),
) {
    val state = pictureViewModel.asState()
    val illust = state.illust
    if (illust != null) {
        PictureScreen(
            illust = illust,
            onBack = navigationManager::popBackStack,
            enableTransition = false,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator()
        }
    }
}

private const val KEY_UGOIRA = "ugoira"
private const val KEY_ILLUST_TITLE = "illust_title"
private const val KEY_ILLUST_DATA = "illust_data"
private const val KEY_ILLUST_TAGS = "illust_tags"
private const val KEY_ILLUST_DIVIDER_1 = "illust_divider_1"
private const val KEY_ILLUST_AUTHOR = "illust_author"
private const val KEY_ILLUST_AUTHOR_OTHER_WORKS = "illust_author_other_works"
private const val KEY_ILLUST_RELATED_TITLE = "illust_related_title"
private const val KEY_SPACER = "spacer"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun PictureScreen(
    illust: Illust,
    onBack: () -> Unit,
    enableTransition: Boolean,
    modifier: Modifier = Modifier,
    pictureViewModel: PictureViewModel = koinViewModel { parametersOf(illust, null) },
    navigationManager: NavigationManager = koinInject(),
) {
    val sideEffect by pictureViewModel.sideEffect.filterIsInstance<SideEffect.Error>()
        .collectAsStateWithLifecycle(null)
    val exception = sideEffect?.throwable
    val relatedIllusts = pictureViewModel.relatedIllusts.collectAsLazyPagingItems()
    val navToPictureScreen = navigationManager::navigateToPictureScreen
    val dispatch = pictureViewModel::dispatch
    val navToSearchResultScreen = navigationManager::navigateToSearchResultScreen
    val popBackToHomeScreen = navigationManager::popBackToMainScreen
    val navToUserDetailScreen = navigationManager::navigateToProfileDetailScreen
    val state = pictureViewModel.asState()
    val currentWindowAdaptiveInfo = currentWindowAdaptiveInfo()
    val relatedLayoutParams = IllustGridDefaults.relatedLayoutParameters()
    val userLayoutParams = IllustGridDefaults.userLayoutParameters()
    val density = LocalDensity.current
    val userSpanCount = with(userLayoutParams.gridCells) {
        with(density) {
            density.calculateCrossAxisCellSizes(
                currentWindowAdaptiveInfo.windowSizeClass.minWidthDp.dp.roundToPx(),
                relatedLayoutParams.horizontalArrangement.spacing.roundToPx(),
            ).size
        }
    }
    val relatedSpanCount = with(relatedLayoutParams.gridCells) {
        with(density) {
            density.calculateCrossAxisCellSizes(
                currentWindowAdaptiveInfo.windowSizeClass.minWidthDp.dp.roundToPx(),
                relatedLayoutParams.horizontalArrangement.spacing.roundToPx()
            ).size
        }
    }
    val relatedRowCount = if (relatedIllusts.itemCount % relatedSpanCount == 0) {
        relatedIllusts.itemCount / relatedSpanCount
    } else {
        relatedIllusts.itemCount / relatedSpanCount + 1
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sharingSuccess = stringResource(RString.sharing_success)
    val shareLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // 处理分享结果
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch {
                    snackbarHostState.showSnackbar(sharingSuccess)
                }
            } else {
                // 分享失败或取消
            }
        }
    val lazyListState = rememberLazyListState()
    val currPage by remember {
        derivedStateOf {
            minOf(
                lazyListState.firstVisibleItemIndex,
                illust.pageCount - 1
            )
        }
    }
    val isBarVisible by remember { derivedStateOf { lazyListState.firstVisibleItemIndex <= illust.pageCount } }
    val isUserInfoFullyVisible = lazyListState.isItemFullyVisible(KEY_ILLUST_TITLE)

    val isBookmarked = illust.isBookmark
    val onBookmarkClick = { restrict: Restrict, tags: List<String>? ->
        if (isBookmarked) {
            BookmarkState.deleteBookmarkIllust(illust.id)
        } else {
            BookmarkState.bookmarkIllust(illust.id, restrict, tags)
        }
    }
    val isFollowed = illust.user.isFollowing
    val isBlocked = BlockingRepository.collectIllustBlockAsState(illustId = illust.id)
    val placeholder = rememberVectorPainter(Icons.Rounded.Refresh)
    val bottomSheetState = rememberModalBottomSheetState()
    val readMediaImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(permissions = listOf(READ_MEDIA_IMAGES))
    } else {
        rememberMultiplePermissionsState(permissions = listOf(READ_EXTERNAL_STORAGE))
    }

    LaunchedEffect(exception) {
        if (exception != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    exception.message ?: getString(RString.unknown_error)
                )
            }
        }
    }

    val prefix = LocalSharedKeyPrefix.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current
    with(sharedTransitionScope) {
        Scaffold(
            modifier = modifier
                .conditionally(enableTransition) {
                    sharedBounds(
                        rememberSharedContentState(key = "${prefix}-card-${illust.id}"),
                        animatedContentScope,
                        enter = fadeIn(DefaultFloatAnimationSpec),
                        exit = fadeOut(DefaultFloatAnimationSpec),
                        boundsTransform = { _, _ -> tween(DefaultAnimationDuration) },
//                    renderInOverlayDuringTransition = false
                    )
                },
            topBar = {
                PictureTopBar(
                    illust = illust,
                    currPage = currPage,
                    isBarVisible = isBarVisible,
                    isBlocked = isBlocked,
                    onBack = onBack,
                    popBackToHomeScreen = popBackToHomeScreen,
                    onShare = {
                        shareLauncher.launch(it)
                    },
                    navToUserDetailScreen = navToUserDetailScreen,
                    onBlock = pictureViewModel::blockIllust,
                    onRemoveBlock = pictureViewModel::removeBlockIllust
                )
            },
            floatingActionButton = {
                if (!isBlocked) {
                    IconButton(
                        onClick = throttleClick {
                            onBookmarkClick(Restrict.PUBLIC, null)
                        },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(50.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        )
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isBookmarked) Color.Red else LocalContentColor.current,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) {
                    TextSnackbar(
                        text = it.visuals.message,
                    )
                }
            },
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                with(sharedTransitionScope) {
                    if (illust.type == Type.Ugoira) {
                        item(key = KEY_UGOIRA) {
                            UgoiraPlayer(
                                images = state.ugoiraImages,
                                placeholder = placeholder
                            )
                        }
                    } else {
                        items(
                            illust.pageCount,
                            key = { "${illust.id}_$it" },
                        ) { index ->
                            val firstImageKey = "image-${illust.id}-0"
                            if (illust.pageCount > 1) {
                                illust.metaPages?.get(index)?.let {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(it.imageUrls?.medium)
                                            .placeholderMemoryCacheKey("image-${illust.id}-$index")
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .conditionally(index == 0 && enableTransition) {
                                                sharedElement(
                                                    sharedTransitionScope.rememberSharedContentState(
                                                        key = "${prefix}-$firstImageKey"
                                                    ),
                                                    animatedVisibilityScope = animatedContentScope,
                                                    placeholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                                                )
                                            }
                                            .throttleClick(
                                                onLongClick = {
                                                    dispatch(PictureAction.GetPictureInfo(index))
                                                }
                                            ),
                                        contentScale = ContentScale.FillWidth,
                                        placeholder = placeholder,
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(illust.imageUrls.medium)
                                        .placeholderMemoryCacheKey("image-${illust.id}-$index")
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .conditionally(index == 0 && enableTransition) {
                                            sharedElement(
                                                sharedTransitionScope.rememberSharedContentState(key = "${prefix}-$firstImageKey"),
                                                animatedVisibilityScope = animatedContentScope,
                                                placeholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
                                            )
                                        }
                                        .throttleClick(
                                            onLongClick = {
                                                dispatch(PictureAction.GetPictureInfo(0))
                                            }
                                        ),
                                    contentScale = ContentScale.FillWidth,
                                    placeholder = placeholder,
                                )
                            }
                        }
                    }
                }
                item(key = KEY_ILLUST_TITLE) {
                    UserInfo(
                        illust = illust,
                        navToUserDetailScreen = navToUserDetailScreen
                    )
                }
                item(key = KEY_ILLUST_DATA) {
                    Row(
                        Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            text = convertUtcStringToLocalDateTime(illust.createDate),
                            modifier = Modifier.padding(start = 20.dp),
                            style = TextStyle(fontSize = 12.sp),
                        )
                        Text(
                            text = illust.totalView.toString() + " ${stringResource(RString.viewed)}",
                            Modifier.padding(start = 10.dp),
                            style = TextStyle(fontSize = 12.sp),
                        )
                        Text(
                            text = illust.totalBookmarks.toString() + " ${stringResource(RString.liked)}",
                            Modifier.padding(start = 10.dp),
                            style = TextStyle(fontSize = 12.sp),
                        )
                    }
                }
                // tag
                item(key = KEY_ILLUST_TAGS) {
                    FlowRow(
                        modifier = Modifier.padding(start = 20.dp, top = 10.dp, end = 20.dp),
                        horizontalArrangement = 5f.spaceBy,
                        verticalArrangement = 5f.spaceBy,
                    ) {
                        illust.tags?.forEach {
                            Row(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        10f.round
                                    )
                                    .throttleClick {
                                        navToSearchResultScreen(it.name)
                                        dispatch(PictureAction.AddSearchHistory(it.name))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 2.5.dp),
                                horizontalArrangement = 5f.spaceBy,
                            ) {
                                Text(
                                    text = "#" + it.name,
                                    modifier = Modifier,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = TextStyle(fontSize = 13.sp, color = deepBlue),
                                )
                                Text(
                                    text = it.translatedName,
                                    modifier = Modifier,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(fontSize = 13.sp)
                                )
                            }
                        }
                    }
                }
                item(key = KEY_ILLUST_DIVIDER_1) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 15.dp)
                            .padding(top = 50.dp)
                    )
                }
                item(key = KEY_ILLUST_AUTHOR) {
                    //作者头像、名字、关注按钮
                    UserFollowInfo(
                        illust = illust,
                        navToUserDetailScreen = navToUserDetailScreen,
                        isFollowed = isFollowed,
                        modifier = Modifier
                            .padding(horizontal = 15.dp)
                            .padding(top = 10.dp)
                    )
                }
                item(key = KEY_ILLUST_AUTHOR_OTHER_WORKS) {
                    FlowRow(
                        modifier = Modifier
                            .padding(horizontal = 15.dp)
                            .padding(top = 10.dp),
                        horizontalArrangement = 5f.spaceBy,
                        maxLines = 1,
                    ) {
                        val otherPrefix = rememberSaveable { Uuid.random().toHexString() }
                        CompositionLocalProvider(
                            LocalSharedKeyPrefix provides otherPrefix
                        ) {
                            val illusts = state.userIllusts.take(userSpanCount)
                            illusts.forEachIndexed { index, it ->
                                val innerIsBookmarked = it.isBookmark
                                SquareIllustItem(
                                    illust = it,
                                    isBookmarked = innerIsBookmarked,
                                    onBookmarkClick = { restrict, tags, isEdit ->
                                        if (isEdit || !innerIsBookmarked) {
                                            BookmarkState.bookmarkIllust(it.id, restrict, tags)
                                        } else {
                                            BookmarkState.deleteBookmarkIllust(it.id)
                                        }
                                    },
                                    navToPictureScreen = { prefix, enableTransition ->
                                        navToPictureScreen(illusts, index, prefix, enableTransition)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                item(key = KEY_ILLUST_RELATED_TITLE) {
                    //相关作品文字，显示在中间
                    Text(
                        text = stringResource(RString.related_artworks),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp, bottom = 10.dp),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
                items(
                    relatedRowCount,
                    key = { index -> "${illust.id}_related_${index}" },
                    contentType = { "related_illusts" }
                ) { rowIndex ->
                    val illustsPair = (0..<relatedSpanCount).mapNotNull { columnIndex ->
                        val index = rowIndex * relatedSpanCount + columnIndex
                        if (index >= relatedIllusts.itemCount) return@mapNotNull null
                        val illust = relatedIllusts[index] ?: return@mapNotNull null
                        Triple(
                            illust,
                            illust.isBookmark,
                            index
                        )
                    }
                    if (illustsPair.isEmpty()) return@items
                    // 相关作品
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 5.dp),
                        horizontalArrangement = relatedLayoutParams.horizontalArrangement
                    ) {
                        illustsPair.forEach { (illust, isBookmarked, index) ->
                            SquareIllustItem(
                                illust = illust,
                                isBookmarked = isBookmarked,
                                onBookmarkClick = { restrict, tags, isEdit ->
                                    if (isEdit || !isBookmarked) {
                                        BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                                    } else {
                                        BookmarkState.deleteBookmarkIllust(illust.id)
                                    }
                                },
                                navToPictureScreen = { prefix, enableTransition ->
                                    navToPictureScreen(
                                        relatedIllusts.itemSnapshotList.items,
                                        index,
                                        prefix,
                                        enableTransition
                                    )
                                },
                                modifier = Modifier.weight(1f / relatedSpanCount.toFloat()),
                                shouldShowTip = index == 0
                            )
                        }
                        if (illustsPair.size < relatedSpanCount) {
                            Spacer(modifier = Modifier.weight((relatedSpanCount - illustsPair.size) / relatedSpanCount.toFloat()))
                        }
                    }
                }

                item(key = KEY_SPACER) {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = !isUserInfoFullyVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    UserInfo(
                        illust = illust,
                        navToUserDetailScreen = navToUserDetailScreen,
                    )
                }
            }
            if (state.bottomSheetState != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        pictureViewModel.closeBottomSheet()
                    },
                    modifier = Modifier.heightIn(getScreenHeight() / 2),
                    sheetState = bottomSheetState,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .throttleClick {
                                    // 下载原始图片
                                    dispatch(
                                        PictureAction.DownloadIllust(
                                            illust.id,
                                            state.bottomSheetState.index,
                                            state.bottomSheetState.downloadUrl
                                        )
                                    )
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(
                                    RString.download_with_size,
                                    state.bottomSheetState.downloadSize
                                ),
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .throttleClick {
                                    readMediaImagePermission.launchMultiplePermissionRequest()
                                    if (readMediaImagePermission.allPermissionsGranted) {
                                        pictureViewModel.shareImage(
                                            state.bottomSheetState.index,
                                            state.bottomSheetState.downloadUrl,
                                            illust,
                                            shareLauncher
                                        )
                                    }
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
                            Text(
                                text = stringResource(RString.share),
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }
            if (state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .throttleClick {},
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            if (isBlocked) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            10.dp,
                            Alignment.CenterVertically
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HideImage,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                        )
                        Text(
                            text = stringResource(RString.illust_hidden),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(
                            onClick = pictureViewModel::removeBlockIllust
                        ) {
                            Text(
                                text = stringResource(RString.show_illust)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserFollowInfo(
    illust: Illust,
    navToUserDetailScreen: (Long) -> Unit,
    isFollowed: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
    ) {
        UserAvatar(
            url = illust.user.profileImageUrls.medium,
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.CenterVertically),
            onClick = {
                navToUserDetailScreen(illust.user.id)
            },
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = illust.user.name,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = "ID: ${illust.user.id}",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isFollowed) {
            OutlinedButton(
                onClick = {
                    FollowState.unFollowUser(illust.user.id)
                }
            ) {
                Text(
                    text = stringResource(RString.followed),
                )
            }
        } else {
            Button(
                onClick = {
                    FollowState.followUser(illust.user.id)
                }
            ) {
                Text(
                    text = stringResource(RString.follow),
                )
            }
        }
    }
}

@Composable
private fun PictureTopBar(
    illust: Illust,
    currPage: Int,
    isBarVisible: Boolean,
    isBlocked: Boolean,
    onBack: () -> Unit,
    popBackToHomeScreen: () -> Unit,
    onShare: (Intent) -> Unit,
    navToUserDetailScreen: (Long) -> Unit,
    onBlock: () -> Unit,
    onRemoveBlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBottomMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {},
        modifier = modifier,
        actions = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.throttleClick { onBack() },
                    )
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 15.dp)
                            .throttleClick { popBackToHomeScreen() }
                    )
                }
                // 分享按钮
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .throttleClick {
                            showBottomMenu = true
                        },
                )
                this@TopAppBar.AnimatedVisibility(
                    modifier = Modifier.align(Alignment.Center),
                    visible = isBarVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = "${currPage + 1}/${illust.pageCount}",
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        )
    )
    if (showBottomMenu) {
        ModalBottomSheet(
            onDismissRequest = { showBottomMenu = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
            ) {
                UserFollowInfo(
                    illust = illust,
                    navToUserDetailScreen = navToUserDetailScreen,
                    isFollowed = illust.user.isFollowing
                )
                BottomMenuItem(
                    onClick = {
                        val shareIntent = ShareUtil.createShareIntent(
                            "${illust.title} | ${illust.user.name} #pixiv https://www.pixiv.net/artworks/${illust.id}"
                        )
                        onShare(shareIntent)
                        showBottomMenu = false
                    },
                    text = stringResource(RString.share),
                    modifier = Modifier.padding(vertical = 15.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                        )
                    }
                )
                BottomMenuItem(
                    onClick = {
                        if (isBlocked) onRemoveBlock() else onBlock()
                        showBottomMenu = false
                    },
                    text = stringResource(if (isBlocked) RString.show_illust else RString.block_illust),
                    modifier = Modifier.padding(vertical = 15.dp),
                    icon = {
                        Icon(
                            imageVector = if (isBlocked) Icons.Rounded.Refresh else Icons.Rounded.Block,
                            contentDescription = null,
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomMenuItem(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .throttleClick(
                indication = ripple(),
                onClick = onClick
            )
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = 10f.spaceBy,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            text = text,
            style = TextStyle(fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UserInfo(
    illust: Illust,
    navToUserDetailScreen: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, top = 10.dp, bottom = 10.dp),
    ) {
        UserAvatar(
            url = illust.user.profileImageUrls.medium,
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.CenterVertically),
            onClick = {
                navToUserDetailScreen(illust.user.id)
            },
        )
        Column(
            modifier = Modifier.padding(start = 10.dp)
        ) {
            Text(
                text = illust.title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
            )
            Text(
                text = illust.user.name,
                modifier = Modifier,
                style = TextStyle(
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
    }
}


@Composable
private fun LazyListState.isItemFullyVisible(key: String): Boolean {
    val fullyVisible by remember(key) {
        derivedStateOf {
            val item = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
                ?: return@derivedStateOf false

            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val itemStart = item.offset
            val itemEnd = item.offset + item.size

            itemStart >= viewportStart && itemEnd <= viewportEnd
        }
    }
    return fullyVisible
}