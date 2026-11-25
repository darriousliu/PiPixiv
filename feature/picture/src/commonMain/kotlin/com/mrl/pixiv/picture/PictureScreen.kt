package com.mrl.pixiv.picture

import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.mrl.pixiv.common.compose.IllustGridDefaults
import com.mrl.pixiv.common.compose.IllustGridSpacing
import com.mrl.pixiv.common.compose.layout.ResizableSplitLayout
import com.mrl.pixiv.common.compose.layout.currentPaneLayoutInfo
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastExpanded
import com.mrl.pixiv.common.compose.layout.rememberSplitPaneState
import com.mrl.pixiv.common.compose.navigationSharedImage
import com.mrl.pixiv.common.compose.ui.BlockSurface
import com.mrl.pixiv.common.compose.ui.IllustBottomBookmarkSheet
import com.mrl.pixiv.common.compose.ui.SectionHeader
import com.mrl.pixiv.common.compose.ui.illust.SquareIllustItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.setting.PreviewImageQuality
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isPrivateBookmark
import com.mrl.pixiv.common.repository.viewmodel.follow.FollowState
import com.mrl.pixiv.common.repository.viewmodel.follow.isFollowing
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.PictureCoverSource
import com.mrl.pixiv.common.router.PictureImageKey
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.adaptiveFileSize1
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.util.getScreenHeight
import com.mrl.pixiv.common.util.isDesktop
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.common.util.selectSaveFile
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.manga.MangaSeriesContextCard
import com.mrl.pixiv.picture.components.UgoiraPlayer
import com.mrl.pixiv.picture.components.rememberArtworkPagePlaceholder
import com.mrl.pixiv.strings.cancel_user_blocked
import com.mrl.pixiv.strings.copy_link
import com.mrl.pixiv.strings.download
import com.mrl.pixiv.strings.download_with_size
import com.mrl.pixiv.strings.export_failed
import com.mrl.pixiv.strings.hide_illust
import com.mrl.pixiv.strings.illust_hidden
import com.mrl.pixiv.strings.related_artworks
import com.mrl.pixiv.strings.save_as
import com.mrl.pixiv.strings.share
import com.mrl.pixiv.strings.show_illust
import com.mrl.pixiv.strings.user_blocked
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.seconds

@Composable
fun PictureDeeplinkScreen(
    modifier: Modifier = Modifier,
    illustId: Long,
    pictureViewModel: PictureViewModel = koinViewModel(key = "picture-$illustId") {
        parametersOf(null, illustId)
    },
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val state = pictureViewModel.asState()
    val illust = state.illust
    if (illust != null) {
        PictureScreen(
            illust = illust,
            onBack = navigationManager::popBackStack,
            pictureIndex = 0,
            modifier = modifier,
            pictureViewModel = pictureViewModel,
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            pictureViewModel.addHistory()
        }
    }
}

private const val KEY_UGOIRA = "ugoira"
private const val KEY_ILLUST_TITLE = "illust_title"
private const val KEY_ILLUST_DATA = "illust_data"
private const val KEY_ILLUST_TAGS = "illust_tags"
private const val KEY_ILLUST_AUTHOR = "illust_author"
private const val KEY_ILLUST_AUTHOR_OTHER_WORKS = "illust_author_other_works"
private const val KEY_VIEW_COMMENTS = "view_comments"
private const val KEY_ILLUST_RELATED_TITLE = "illust_related_title"

@Composable
internal fun rememberPictureViewModel(illust: Illust): PictureViewModel =
    koinViewModel(key = "picture-${illust.id}") { parametersOf(illust, null) }

@Composable
internal fun PictureScreen(
    illust: Illust,
    onBack: () -> Unit,
    pictureIndex: Int,
    modifier: Modifier = Modifier,
    cover: PictureCoverSource? = null,
    previewEntryId: String? = null,
    isActivePage: () -> Boolean = { true },
    pictureViewModel: PictureViewModel = rememberPictureViewModel(illust),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val relatedIllusts = pictureViewModel.relatedIllusts.collectAsLazyPagingItems()
    val navToPictureScreen = navigationManager::navigateToPictureScreen
    val dispatch = pictureViewModel::dispatch
    val navToSearchResultScreen = navigationManager::navigateToSearchResultScreen
    val popBackToHomeScreen = navigationManager::popBackToMainScreen
    val navToUserDetailScreen = navigationManager::navigateToProfileDetailScreen
    val state = pictureViewModel.asState()
    val paneLayoutInfo = currentPaneLayoutInfo()
    // Keep both image and details panes usable; medium widths need the single-column layout.
    val useTwoPaneLayout = paneLayoutInfo.sizeClass.isWidthAtLeastExpanded
    val relatedLayoutParams = IllustGridDefaults.relatedLayoutParameters(
        if (useTwoPaneLayout) paneLayoutInfo.copy(isSplit = true) else paneLayoutInfo,
    )
    val userLayoutParams = IllustGridDefaults.userLayoutParameters(paneLayoutInfo)
    val density = LocalDensity.current
    val userSpanCount = with(userLayoutParams.gridCells) {
        with(density) {
            density.calculateCrossAxisCellSizes(
                paneLayoutInfo.size.width.roundToPx(),
                relatedLayoutParams.horizontalArrangement.spacing.roundToPx(),
            ).size
        }
    }
    val relatedSpanCount = with(relatedLayoutParams.gridCells) {
        with(density) {
            density.calculateCrossAxisCellSizes(
                (paneLayoutInfo.size.width - relatedLayoutParams.edgePadding * 2).roundToPx(),
                IllustGridSpacing.LabeledColumn.roundToPx(),
            ).size
        }
    }
    val relatedRowCount = if (relatedIllusts.itemCount % relatedSpanCount == 0) {
        relatedIllusts.itemCount / relatedSpanCount
    } else {
        relatedIllusts.itemCount / relatedSpanCount + 1
    }

    val lazyListState = rememberLazyListState()
    val pictureSplitState = rememberSplitPaneState(key = illust.id, initialFraction = 0.5f)
    val rightListState = rememberLazyListState()
    val currPage by remember {
        computedStateOf {
            minOf(
                lazyListState.firstVisibleItemIndex,
                illust.pageCount - 1
            )
        }
    }
    val isBarVisible by remember { computedStateOf { lazyListState.firstVisibleItemIndex <= illust.pageCount } }

    val isBookmarked = illust.isBookmark
    val browsingSettings by requireUserPreferenceFlow.collectAsStateWithLifecycle {
        browsingSettings
    }
    var arePreviewControlsVisible by remember { mutableStateOf(true) }
    var previewControlsInteractionVersion by remember { mutableIntStateOf(0) }
    val onBookmarkClick = { restrict: Restrict, tags: List<String>? ->
        if (isBookmarked) {
            BookmarkState.deleteBookmarkIllust(illust.id)
        } else {
            BookmarkState.bookmarkIllust(illust.id, restrict, tags)
        }
    }
    val isFollowed = illust.user.isFollowing
    val isIllustBlocked = BlockingRepositoryV2.collectIllustBlockAsState(illustId = illust.id)
    val isUserBlocked = BlockingRepositoryV2.collectUserBlockAsState(userId = illust.user.id)
    val isAnyBlocked = isIllustBlocked || isUserBlocked
    val errorImage = rememberVectorPainter(Icons.Rounded.ErrorOutline)

    var showAdvancedBookmark by rememberSaveable { mutableStateOf(false) }
    var showDownloadPicker by rememberSaveable(illust.id) { mutableStateOf(false) }
    val bottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var contextMenuImageIndex by remember { mutableStateOf<Int?>(null) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    val showPreviewControls = !browsingSettings.autoHidePreviewControls ||
            !isBarVisible ||
            arePreviewControlsVisible

    fun revealPreviewControls() {
        if (browsingSettings.autoHidePreviewControls) {
            arePreviewControlsVisible = true
            previewControlsInteractionVersion++
        }
    }
    LaunchedEffect(
        browsingSettings.autoHidePreviewControls,
        isBarVisible,
        arePreviewControlsVisible,
        currPage,
        previewControlsInteractionVersion,
    ) {
        if (!browsingSettings.autoHidePreviewControls) {
            arePreviewControlsVisible = true
            return@LaunchedEffect
        }
        if (!isBarVisible) {
            arePreviewControlsVisible = true
            return@LaunchedEffect
        }
        if (arePreviewControlsVisible) {
            delay(PREVIEW_CONTROLS_HIDE_DELAY_MS)
            arePreviewControlsVisible = false
        }
    }
    val getOriginalUrl: (Int) -> String? = { index ->
        if (illust.pageCount > 1) {
            illust.metaPages?.getOrNull(index)?.imageUrls?.original
        } else {
            illust.metaSinglePage.originalImageURL
        }
    }
    val getPreviewUrl: (Int) -> String? = { index ->
        val imageUrls = if (illust.pageCount > 1) {
            illust.metaPages?.getOrNull(index)?.imageUrls
        } else {
            illust.imageUrls
        }
        val originalUrl = getOriginalUrl(index)

        when (browsingSettings.previewImageQuality) {
            PreviewImageQuality.MEDIUM -> listOf(
                imageUrls?.medium,
                imageUrls?.large,
                originalUrl,
            )

            PreviewImageQuality.HIGH -> listOf(
                imageUrls?.large,
                imageUrls?.medium,
                originalUrl,
            )

            PreviewImageQuality.ORIGINAL -> listOf(
                originalUrl,
                imageUrls?.large,
                imageUrls?.medium,
            )
        }.firstOrNull { !it.isNullOrBlank() }
    }
    val openOriginalPreview: (Int, PictureImageKey?) -> Unit =
        openOriginalPreview@{ index, sharedImage ->
            if (browsingSettings.autoHidePreviewControls && !arePreviewControlsVisible) {
                revealPreviewControls()
                return@openOriginalPreview
            }
            if (!browsingSettings.tapImageToOpenFullResolutionPreview) return@openOriginalPreview
            val selectedUrl = getOriginalUrl(index) ?: return@openOriginalPreview
            val imageUrls = (0 until illust.pageCount.coerceAtLeast(1))
                .mapNotNull(getOriginalUrl)
            if (imageUrls.isNotEmpty()) {
                navigationManager.navigateToImagePreviewScreen(
                    imageUrls = imageUrls,
                    initialIndex = imageUrls.indexOf(selectedUrl).coerceAtLeast(0),
                    sharedImage = sharedImage,
                )
            }
        }
    // Mobile keeps FileKit's Compose ActivityResult integration; desktop borrows
    // a native Tao parent for the lifetime of each file dialog.
    var pendingMobileSaveAsUrl by remember { mutableStateOf<String?>(null) }
    val mobileSaveAsLauncher = if (platform !is Platform.Desktop) {
        rememberFileSaverLauncher(dialogSettings = FileKitDialogSettings.createDefault()) { file ->
            val url = pendingMobileSaveAsUrl
            pendingMobileSaveAsUrl = null
            if (file != null && url != null) pictureViewModel.saveAsImage(url, file)
        }
    } else {
        null
    }
    val coroutineScope = rememberCoroutineScope()
    val downloadPages = remember(illust.metaPages, illust.pageCount) {
        if (illust.pageCount < 2) {
            emptyList()
        } else {
            illust.metaPages.orEmpty().take(illust.pageCount).mapIndexedNotNull { index, page ->
                val urls = page.imageUrls ?: return@mapIndexedNotNull null
                if (urls.original.isBlank()) return@mapIndexedNotNull null
                ArtworkDownloadPage(
                    index = index,
                    thumbnailUrl = urls.preferredThumbnailUrl,
                )
            }
        }
    }
    val artworkActions: @Composable (Modifier) -> Unit = { toolbarModifier ->
        ArtworkActionToolbar(
            isBookmarked = isBookmarked,
            isPrivateBookmark = illust.isPrivateBookmark,
            canDownload = !state.loading && when {
                illust.type == Type.Ugoira -> true
                illust.pageCount > 1 -> downloadPages.isNotEmpty()
                else -> !getOriginalUrl(0).isNullOrBlank()
            },
            onDownload = throttleClick {
                when {
                    illust.type == Type.Ugoira -> pictureViewModel.downloadUgoiraAsGIF()
                    illust.pageCount > 1 -> showDownloadPicker = true
                    else -> pictureViewModel.downloadOriginalPages(illust.id, listOf(0))
                }
            },
            onBookmark = throttleClick {
                val restrict = if (requireUserPreferenceValue.defaultPrivateBookmark) {
                    Restrict.PRIVATE
                } else {
                    Restrict.PUBLIC
                }
                onBookmarkClick(restrict, null)
            },
            onEditBookmark = { showAdvancedBookmark = true },
            onShare = throttleClick {
                coroutineScope.launch {
                    ShareUtil.shareText(
                        "${illust.title} | ${illust.user.name} #pixiv https://www.pixiv.net/artworks/${illust.id}",
                    )
                }
            },
            modifier = toolbarModifier.navigationBarsPadding(),
        )
    }
    fun saveAsImage(url: String) {
        val (fileName, extension) = extractFileNameAndExtension(url)
        if (mobileSaveAsLauncher != null) {
            pendingMobileSaveAsUrl = url
            mobileSaveAsLauncher.launch(suggestedName = fileName, defaultExtension = extension)
            return
        }
        coroutineScope.launch {
            val file = selectSaveFile(fileName, extension, RStrings.export_failed)
            if (file != null) pictureViewModel.saveAsImage(url, file)
        }
    }

    fun LazyListScope.illustImageItems() {
        if (illust.type == Type.Ugoira) {
            item(key = KEY_UGOIRA) {
                UgoiraPlayer(
                    initialImage = illust.imageUrls.medium,
                    images = state.ugoiraState.ugoiraImages,
                    loading = state.ugoiraState.loading,
                    playUgoira = state.ugoiraState.isPlaying,
                    loadingUgoira = { dispatch(PictureAction.DownloadUgoira(illust.id)) },
                    downloadUgoira = pictureViewModel::getUgoiraInfo,
                    onToggleUgoira = pictureViewModel::toggleUgoiraPlayState,
                )
            }
        } else {
            items(illust.pageCount, key = { "${illust.id}_$it" }) { index ->
                if (illust.pageCount > 1 && illust.metaPages?.getOrNull(index) == null) return@items
                val source = cover?.takeIf { index == 0 && it.key.illustId == illust.id }
                val sharedImage = source?.key?.takeIf { isActivePage() } ?: previewEntryId?.let { entryId ->
                    PictureImageKey(entryId, "picture:$pictureIndex", illust.id, page = index)
                }
                val placeholder = rememberArtworkPagePlaceholder(index + 1, illust.pageCount)
                val thumbnail = source?.thumbnailUrl?.takeIf { it == illust.imageUrls.medium }
                val context = LocalPlatformContext.current
                val url = getPreviewUrl(index)
                val request = remember(context, url, thumbnail) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .placeholderMemoryCacheKey(thumbnail)
                        .build()
                }
                Box {
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        modifier = Modifier
                            .navigationSharedImage(sharedImage)
                            .fillMaxWidth()
                            .conditionally(source != null && illust.width > 0 && illust.height > 0) {
                                aspectRatio(illust.width.toFloat() / illust.height)
                            }
                            .throttleClick(
                                onClick = { openOriginalPreview(index, sharedImage) },
                                onLongClick = { dispatch(PictureAction.GetPictureInfo(index)) },
                            )
                            .conditionally(platform.isDesktop()) {
                                onRightClickDetect(index) { idx, offset ->
                                    contextMenuImageIndex = idx
                                    contextMenuOffset = offset
                                }
                            },
                        contentScale = ContentScale.FillWidth,
                        placeholder = placeholder,
                        error = errorImage,
                    )
                    if (platform.isDesktop()) {
                        ImageContextMenuDropdown(
                            expanded = contextMenuImageIndex == index,
                            offset = contextMenuOffset,
                            originalUrl = getOriginalUrl(index),
                            onDismiss = { contextMenuImageIndex = null },
                            onDownload = { originalUrl ->
                                pictureViewModel.downloadIllust(illust.id, index, originalUrl)
                            },
                            onSaveAs = ::saveAsImage,
                            onCopyLink = { originalUrl ->
                                coroutineScope.launch { copyToClipboard(originalUrl) }
                            },
                        )
                    }
                }
            }
        }
    }

    fun LazyListScope.illustDetailItems(
        margin: Dp,
        currentUserSpanCount: Int,
        currentRelatedSpanCount: Int,
        currentRelatedRowCount: Int,
    ) {
        val inset = Modifier.padding(horizontal = margin)
        item(key = KEY_ILLUST_TITLE) {
            ArtworkTitleSection(illust, modifier = inset.padding(top = 20.dp))
        }
        if (illust.caption.isNotBlank()) {
            item(key = KEY_ILLUST_DATA) {
                ArtworkCaption(illust.caption, modifier = inset.padding(top = 16.dp))
            }
        }
        if (illust.series?.id?.let { it > 0 } == true) {
            item(key = "manga_series_context") {
                MangaSeriesContextCard(
                    illust = illust,
                    onSeriesClick = navigationManager::navigateToMangaSeriesScreen,
                    onEpisodeClick = navigationManager::navigateToSinglePictureScreen,
                )
            }
        }
        val tags = illust.tags.orEmpty()
        if (tags.isNotEmpty()) {
            item(key = KEY_ILLUST_TAGS) {
                ArtworkTags(
                    tags = tags,
                    onSearch = { tag ->
                        navToSearchResultScreen(tag.name, false, AppViewMode.ILLUST)
                        dispatch(PictureAction.AddSearchHistory(tag.name))
                    },
                    modifier = inset.padding(top = 16.dp),
                )
            }
        }
        item(key = KEY_ILLUST_AUTHOR) {
            UserFollowInfo(
                illust = illust,
                navToUserDetailScreen = navToUserDetailScreen,
                isFollowed = isFollowed,
                modifier = inset.padding(top = 24.dp),
            )
        }
        if (state.userIllusts.isNotEmpty()) {
            item(key = KEY_ILLUST_AUTHOR_OTHER_WORKS) {
                val works = state.userIllusts.take(currentUserSpanCount)
                ArtworkWorkRow(
                    columns = currentUserSpanCount,
                    itemCount = works.size,
                    spacing = IllustGridSpacing.Thumbnail,
                    modifier = inset.padding(top = 12.dp),
                ) { index, cell ->
                    val work = works[index]
                    DetailWorkItem(
                        work = work,
                        onOpen = { source -> navToPictureScreen(works, index, source) },
                        sharedElementSlot = "picture:$pictureIndex:other-works:$index",
                        modifier = cell,
                    )
                }
            }
        }
        item(key = KEY_VIEW_COMMENTS) {
            ArtworkComments(
                count = illust.totalComments,
                onClick = { navigationManager.navigateToCommentScreen(illust.id, CommentType.ILLUST) },
                modifier = inset.padding(top = 24.dp),
            )
        }
        item(key = KEY_ILLUST_RELATED_TITLE) {
            SectionHeader(text = stringResource(RStrings.related_artworks), modifier = inset.padding(top = 24.dp))
        }
        items(
            count = currentRelatedRowCount,
            key = { index -> "${illust.id}_related_${index}" },
            contentType = { "related_illusts" },
        ) { rowIndex ->
            val firstIndex = rowIndex * currentRelatedSpanCount
            val count = (relatedIllusts.itemCount - firstIndex).coerceIn(0, currentRelatedSpanCount)
            ArtworkWorkRow(
                columns = currentRelatedSpanCount,
                itemCount = count,
                spacing = IllustGridSpacing.LabeledColumn,
                modifier = inset.padding(bottom = IllustGridSpacing.LabeledRow),
            ) { column, cell ->
                val index = firstIndex + column
                val work = relatedIllusts[index]
                if (work == null) {
                    Spacer(cell)
                } else {
                    DetailWorkItem(
                        work = work,
                        onOpen = { source -> navToPictureScreen(relatedIllusts.itemSnapshotList.items, index, source) },
                        sharedElementSlot = "picture:$pictureIndex:related:$index",
                        modifier = cell,
                        showLabel = true,
                        shouldShowTip = index == 0,
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (!useTwoPaneLayout || isAnyBlocked) {
                CapturedAnimatedVisibility(
                    visible = isAnyBlocked || showPreviewControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    PictureTopBar(
                        illust = illust,
                        currPage = currPage,
                        isBarVisible = isBarVisible,
                        isIllustBlocked = isIllustBlocked,
                        isUserBlocked = isUserBlocked,
                        onBack = onBack,
                        popBackToHomeScreen = popBackToHomeScreen,
                        navToUserDetailScreen = navToUserDetailScreen,
                        onBlock = pictureViewModel::blockIllust,
                        onRemoveBlock = pictureViewModel::removeBlockIllust
                    )
                }
            }
        },
    ) {
        if (isAnyBlocked) {
            BlockSurface(
                modifier = Modifier.fillMaxSize(),
                icon = {
                    Icon(
                        imageVector = if (isIllustBlocked) Icons.Rounded.HideImage else Icons.Rounded.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                    )
                },
                title = {
                    Text(
                        text = stringResource(if (isIllustBlocked) RStrings.illust_hidden else RStrings.user_blocked),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                button = {
                    Button(
                        onClick = if (isIllustBlocked) {
                            pictureViewModel::removeBlockIllust
                        } else {
                            pictureViewModel::removeBlockUser
                        }
                    ) {
                        Text(
                            text = stringResource(if (isIllustBlocked) RStrings.show_illust else RStrings.cancel_user_blocked)
                        )
                    }
                }
            )
        } else if (useTwoPaneLayout) {
            ResizableSplitLayout(
                state = pictureSplitState,
                minSourceWidth = 320.dp,
                minDetailWidth = 360.dp,
                source = {
                    // Left pane: image list with PictureTopBar overlaid
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            illustImageItems()
                        }
                        CapturedAnimatedVisibility(
                            visible = showPreviewControls,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            PictureTopBar(
                                illust = illust,
                                currPage = currPage,
                                isBarVisible = isBarVisible,
                                isIllustBlocked = isIllustBlocked,
                                isUserBlocked = isUserBlocked,
                                onBack = onBack,
                                popBackToHomeScreen = popBackToHomeScreen,
                                navToUserDetailScreen = navToUserDetailScreen,
                                onBlock = pictureViewModel::blockIllust,
                                onRemoveBlock = pictureViewModel::removeBlockIllust
                            )
                        }
                    }
                },
                detail = {
                    // Right pane: details and related works
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        var controlsHeight by remember { mutableIntStateOf(0) }
                        val rightPaneWidth = maxWidth
                        val rightRelatedSpanCount = with(relatedLayoutParams.gridCells) {
                            with(density) {
                                calculateCrossAxisCellSizes(
                                    (rightPaneWidth - relatedLayoutParams.edgePadding * 2).roundToPx(),
                                    IllustGridSpacing.LabeledColumn.roundToPx(),
                                ).size
                            }
                        }
                        val rightUserSpanCount = with(userLayoutParams.gridCells) {
                            with(density) {
                                calculateCrossAxisCellSizes(
                                    rightPaneWidth.roundToPx(),
                                    relatedLayoutParams.horizontalArrangement.spacing.roundToPx()
                                ).size
                            }
                        }
                        val rightRelatedRowCount =
                            if (relatedIllusts.itemCount % rightRelatedSpanCount == 0) {
                                relatedIllusts.itemCount / rightRelatedSpanCount
                            } else {
                                relatedIllusts.itemCount / rightRelatedSpanCount + 1
                            }
                        LazyColumn(
                            state = rightListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = with(density) { controlsHeight.toDp() }),
                        ) {
                            illustDetailItems(
                                margin = 24.dp,
                                currentUserSpanCount = rightUserSpanCount,
                                currentRelatedSpanCount = rightRelatedSpanCount,
                                currentRelatedRowCount = rightRelatedRowCount,
                            )
                        }
                        artworkActions(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { controlsHeight = it.height },
                        )
                    }
                },
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                var controlsHeight by remember { mutableIntStateOf(0) }
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = with(density) { controlsHeight.toDp() }),
                ) {
                    illustImageItems()
                    illustDetailItems(
                        margin = relatedLayoutParams.edgePadding,
                        currentUserSpanCount = userSpanCount,
                        currentRelatedSpanCount = relatedSpanCount,
                        currentRelatedRowCount = relatedRowCount,
                    )
                }
                artworkActions(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { controlsHeight = it.height },
                )
            }
        }
        if (state.bottomSheetState != null) {
            BottomMenu(
                onDismissRequest = {
                    pictureViewModel.closeBottomSheet()
                },
                downloadSize = state.bottomSheetState.downloadSize.adaptiveFileSize1(),
                onDownload = {
                    // 下载原始图片
                    if (illust.type == Type.Ugoira) {
                        pictureViewModel.downloadUgoiraAsGIF()
                    } else {
                        pictureViewModel.downloadIllust(
                            illust.id,
                            state.bottomSheetState.index,
                            state.bottomSheetState.downloadUrl
                        )
                    }
                },
                onShare = {
                    pictureViewModel.shareImage(
                        state.bottomSheetState.index,
                        state.bottomSheetState.downloadUrl,
                        illust
                    )
                }
            )
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
    }

    if (showDownloadPicker && !isAnyBlocked && isActivePage()) {
        ArtworkDownloadPagePicker(
            pages = downloadPages,
            onDismissRequest = { showDownloadPicker = false },
            onDownload = { pageIndices ->
                if (showDownloadPicker) {
                    showDownloadPicker = false
                    pictureViewModel.downloadOriginalPages(illust.id, pageIndices)
                }
            },
        )
    }

    if (showAdvancedBookmark) {
        IllustBottomBookmarkSheet(
            hideBottomSheet = { showAdvancedBookmark = false },
            illust = illust,
            bottomSheetState = bottomSheetState,
            onBookmarkClick = { restrict, tags, isEdit ->
                if (isEdit || !isBookmarked) {
                    BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                } else {
                    BookmarkState.deleteBookmarkIllust(illust.id)
                }
            },
        )
    }
}

private fun Modifier.onRightClickDetect(
    index: Int,
    onRightClick: (index: Int, offset: Offset) -> Unit
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                onRightClick(index, event.changes.firstOrNull()?.position ?: Offset.Zero)
            }
        }
    }
}

@Composable
private fun ImageContextMenuDropdown(
    expanded: Boolean,
    offset: Offset,
    originalUrl: String?,
    onDismiss: () -> Unit,
    onDownload: (url: String) -> Unit,
    onSaveAs: (url: String) -> Unit,
    onCopyLink: (url: String) -> Unit,
) {
    val density = LocalDensity.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = with(density) {
            DpOffset(offset.x.toDp(), offset.y.toDp())
        }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(RStrings.download)) },
            onClick = {
                onDismiss()
                originalUrl?.let { onDownload(it) }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(RStrings.save_as)) },
            onClick = {
                onDismiss()
                originalUrl?.let { url -> onSaveAs(url) }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(RStrings.copy_link)) },
            onClick = {
                onDismiss()
                originalUrl?.let { onCopyLink(it) }
            }
        )
    }
}

private fun extractFileNameAndExtension(url: String): Pair<String, String> {
    val lastSegment = url.substringAfterLast("/").ifEmpty { url }
    val dotIndex = lastSegment.lastIndexOf('.')
    return if (dotIndex > 0) {
        lastSegment.substring(0, dotIndex) to lastSegment.substring(dotIndex + 1)
    } else {
        lastSegment to "jpg"
    }
}

private val PREVIEW_CONTROLS_HIDE_DELAY_MS = 3.seconds

@Composable
internal expect fun Modifier.clickWithPermission(
    onClick: () -> Unit
): Modifier

@Composable
private fun BottomMenu(
    onDismissRequest: () -> Unit,
    downloadSize: String,
    modifier: Modifier = Modifier,
    onDownload: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    val bottomSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.heightIn(getScreenHeight() / 2),
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
                    .throttleClick(onClick = onDownload)
                    .padding(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null
                )
                Text(
                    text = stringResource(RStrings.download_with_size, downloadSize),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickWithPermission {
                        onShare()
                    }
                    .padding(vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
                Text(
                    text = stringResource(RStrings.share),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailWorkItem(
    work: Illust,
    onOpen: (PictureCoverSource?) -> Unit,
    sharedElementSlot: String,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    shouldShowTip: Boolean = false,
) {
    val isBookmarked = work.isBookmark
    SquareIllustItem(
        illust = work,
        isBookmarked = isBookmarked,
        onBookmarkClick = { restrict, tags, isEdit ->
            if (isEdit || !isBookmarked) {
                BookmarkState.bookmarkIllust(work.id, restrict, tags)
            } else {
                BookmarkState.deleteBookmarkIllust(work.id)
            }
        },
        navToPictureScreen = onOpen,
        sharedElementSlot = sharedElementSlot,
        modifier = modifier,
        showLabel = showLabel,
        shouldShowTip = shouldShowTip,
    )
}

@Composable
private fun UserFollowInfo(
    illust: Illust,
    navToUserDetailScreen: (Long) -> Unit,
    isFollowed: Boolean,
    modifier: Modifier = Modifier,
) {
    val user = illust.user
    ArtworkAuthor(
        user = user,
        isFollowed = isFollowed,
        isUpdating = FollowState.isUpdating(user.id),
        onOpen = { navToUserDetailScreen(user.id) },
        onFollow = {
            if (isFollowed) FollowState.unFollowUser(user.id) else FollowState.followUser(user.id)
        },
        modifier = modifier,
    )
}

@Composable
private fun PictureTopBar(
    illust: Illust,
    currPage: Int,
    isBarVisible: Boolean,
    isIllustBlocked: Boolean,
    isUserBlocked: Boolean,
    onBack: () -> Unit,
    popBackToHomeScreen: () -> Unit,
    navToUserDetailScreen: (Long) -> Unit,
    onBlock: () -> Unit,
    onRemoveBlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBottomMenu by rememberSaveable { mutableStateOf(false) }
    ArtworkTopBar(
        currentPage = currPage + 1,
        pageCount = illust.pageCount,
        showPage = isBarVisible,
        isBlocked = isIllustBlocked || isUserBlocked,
        onBack = onBack,
        onHome = popBackToHomeScreen,
        onMore = { showBottomMenu = true },
        modifier = modifier,
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
                        if (isIllustBlocked) onRemoveBlock() else onBlock()
                        showBottomMenu = false
                    },
                    text = stringResource(if (isIllustBlocked) RStrings.show_illust else RStrings.hide_illust),
                    modifier = Modifier.padding(vertical = 15.dp),
                    icon = {
                        Icon(
                            imageVector = if (isIllustBlocked) Icons.Rounded.Image else Icons.Rounded.HideImage,
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
