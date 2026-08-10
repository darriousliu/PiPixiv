package com.mrl.pixiv.novel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.compose.ui.BlockSurface
import com.mrl.pixiv.common.compose.ui.BookmarkIcon
import com.mrl.pixiv.common.compose.ui.NovelBottomBookmarkSheet
import com.mrl.pixiv.common.compose.ui.novel.NovelReadLaterButton
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isPrivateBookmark
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.StatusBarVisibilityEffect
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.ai_translation_setting
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.chapter_next
import com.mrl.pixiv.strings.chapter_previous
import com.mrl.pixiv.strings.delete_translation
import com.mrl.pixiv.strings.export_txt_button
import com.mrl.pixiv.strings.font_size_value
import com.mrl.pixiv.strings.hide_novel
import com.mrl.pixiv.strings.line_spacing_value
import com.mrl.pixiv.strings.more
import com.mrl.pixiv.strings.novel_collection
import com.mrl.pixiv.strings.novel_hidden
import com.mrl.pixiv.strings.novel_marker
import com.mrl.pixiv.strings.novel_marker_page
import com.mrl.pixiv.strings.novel_work_information
import com.mrl.pixiv.strings.read_later
import com.mrl.pixiv.strings.regenerate_translation
import com.mrl.pixiv.strings.share_link
import com.mrl.pixiv.strings.show_novel
import com.mrl.pixiv.strings.show_original_text
import com.mrl.pixiv.strings.show_translated_text
import com.mrl.pixiv.strings.translate_novel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal data class NovelTranslationListAnchor(
    val novelId: Long,
    val itemIndex: Int,
    val scrollOffset: Int,
)

internal fun shouldRestoreNovelTranslationListAnchor(
    wasTranslating: Boolean,
    isTranslating: Boolean,
    isTranslated: Boolean,
): Boolean = wasTranslating && !isTranslating && !isTranslated

internal fun resolveNovelTranslationListAnchorItemIndex(
    requestedItemIndex: Int,
    paragraphStartItemIndex: Int,
    paragraphCount: Int,
): Int = requestedItemIndex.coerceIn(
    minimumValue = 0,
    maximumValue = paragraphStartItemIndex + paragraphCount.coerceAtLeast(0),
)

@Composable
fun NovelScreen(
    novelId: Long,
    markerPage: Int? = null,
    readLaterTargetLanguage: String? = null,
    modifier: Modifier = Modifier,
    viewModel: NovelViewModel = koinViewModel {
        parametersOf(novelId, markerPage ?: 0)
    },
    navigationManager: NavigationManager = koinInject(),
) {
    val uriHandler = LocalUriHandler.current
    val state = viewModel.asState()
    val chapterStateKey = novelChapterStateKey(
        entryNovelId = novelId,
        loadedNovelId = state.novel?.id,
    )
    val currentNovelId = chapterStateKey
    val isNovelBlocked = BlockingRepositoryV2.collectNovelBlockAsState(currentNovelId)
    val listState = key(chapterStateKey) {
        rememberLazyListState()
    }
    val paragraphLayoutCacheKey = state.paragraphLayoutCacheKey()
    val paragraphLayouts = remember(paragraphLayoutCacheKey) {
        mutableStateMapOf<Int, TextLayoutResult>()
    }
    val cumulativeParagraphLengths = remember(state.paragraphs) {
        buildCumulativeParagraphLengths(state.paragraphs)
    }
    val markerPages = remember(state.paragraphSpans) {
        markerPagesForSpans(state.paragraphSpans)
    }
    var showBookmarkBottomSheet by remember { mutableStateOf(false) }
    var showMetadataBottomSheet by remember(state.novel?.id) { mutableStateOf(false) }
    var translationListAnchor by remember(state.novel?.id) {
        mutableStateOf<NovelTranslationListAnchor?>(null)
    }
    var wasTranslating by remember(state.novel?.id) { mutableStateOf(false) }

    LaunchedEffect(state.loading, state.novel?.id, readLaterTargetLanguage) {
        if (!state.loading &&
            state.novel?.id == novelId &&
            !readLaterTargetLanguage.isNullOrBlank()
        ) {
            viewModel.dispatch(
                NovelIntent.ApplyReadLaterTranslation(readLaterTargetLanguage)
            )
        }
    }

    // 沉浸逻辑: 滚动到正文区域时隐藏TopBar和FAB
    val isContentVisible by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key is Int // index
        }
    }
    var manuallyShowTopBar by remember { mutableStateOf(false) }
    val showBar = !isContentVisible || manuallyShowTopBar
    val readingProgressFraction by remember(
        state.novel?.id,
        state.paragraphs,
        state.isTranslating,
        listState,
    ) {
        derivedStateOf {
            if (state.isTranslating) return@derivedStateOf 0f
            val novel = state.novel ?: return@derivedStateOf 0f
            val paragraphStartIndex =
                paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())
            buildBottomReadingProgressFraction(
                listState = listState,
                paragraphStartIndex = paragraphStartIndex,
                paragraphCount = state.paragraphs.size,
                paragraphLayouts = paragraphLayouts,
                paragraphs = state.paragraphs,
                cumulativeParagraphLengths = cumulativeParagraphLengths,
            )
        }
    }
    val currentMarkerPage by remember(state.novel?.id, state.paragraphSpans, listState) {
        derivedStateOf {
            val novel = state.novel ?: return@derivedStateOf 1
            val paragraphStartIndex =
                paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())
            val paragraphItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
                itemInfo.index in paragraphStartIndex until
                        (paragraphStartIndex + state.paragraphSpans.size)
            }
            val paragraphIndex = paragraphItem
                ?.let { it.index - paragraphStartIndex }
                ?: 0
            markerPages.getOrElse(paragraphIndex) { 1 }
        }
    }

    StatusBarVisibilityEffect(hidden = state.novel != null && !isNovelBlocked && !showBar)

    LaunchedEffect(manuallyShowTopBar) {
        if (manuallyShowTopBar) {
            delay(3000.milliseconds) // 3秒后自动隐藏
            manuallyShowTopBar = false
        }
    }

    val latestState = rememberUpdatedState(state)
    val latestParagraphLayouts = rememberUpdatedState(paragraphLayouts)
    val saveReadingProgress = remember(listState, viewModel) {
        {
            val currentState = latestState.value
            val novel = currentState.novel ?: return@remember
            if (currentState.isTranslating || currentState.paragraphs.isEmpty()) {
                return@remember
            }
            val paragraphStartIndex =
                paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())
            val firstVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: return@remember
            val contentRange =
                paragraphStartIndex until (paragraphStartIndex + currentState.paragraphs.size)
            if (firstVisibleItemIndex !in contentRange) {
                viewModel.clearProgress(novelId = novel.id)
                return@remember
            }
            val progress = buildVisibleReadingProgress(
                listState = listState,
                paragraphStartIndex = paragraphStartIndex,
                paragraphCount = currentState.paragraphs.size,
                paragraphLayouts = latestParagraphLayouts.value,
                paragraphs = currentState.paragraphs
            ) ?: return@remember
            viewModel.saveProgress(novelId = novel.id, progress = progress)
        }
    }

    LaunchedEffect(state.novel?.id, listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .drop(1)
            .filter { !it }
            .collect {
                saveReadingProgress()
            }
    }

    var handledRestoreVersion by remember(state.novel?.id) { mutableStateOf(-1L) }
    LaunchedEffect(state.restoreVersion, state.novel?.id, state.isTranslating) {
        if (handledRestoreVersion == state.restoreVersion) return@LaunchedEffect
        handledRestoreVersion = state.restoreVersion
        if (state.isTranslating) return@LaunchedEffect
        val novel = state.novel ?: return@LaunchedEffect
        val resolvedProgress = state.restoreProgress ?: return@LaunchedEffect
        if (state.paragraphs.isEmpty()) return@LaunchedEffect
        val paragraphStartIndex =
            paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())

        val targetItemIndex = paragraphStartIndex + resolvedProgress.paragraphIndex
        Logger.d(tag = "NovelScreen") { "Restore: paragraphStartIndex=$paragraphStartIndex, targetItemIndex=$targetItemIndex" }

        // 先滚动到目标段落；布局缓存已按正文和排版参数隔离，只会包含当前布局结果。
        listState.scrollToItem(targetItemIndex, 0)

        // 等待目标段落的布局完成。包含图片标记的段落可能没有文本布局，这里做超时兜底。
        val layout = withTimeoutOrNull(500L.milliseconds) {
            while (paragraphLayouts[resolvedProgress.paragraphIndex] == null) {
                delay(16.milliseconds)
            }
            paragraphLayouts[resolvedProgress.paragraphIndex]
        } ?: run {
            Logger.d(tag = "NovelScreen") {
                "Restore: paragraphIndex=${resolvedProgress.paragraphIndex} has no text layout, keep item-top restore."
            }
            return@LaunchedEffect
        }

        val targetParagraph = state.paragraphs[resolvedProgress.paragraphIndex]
        val targetCharIndex = resolvedProgress.charIndex.coerceIn(0, targetParagraph.length)

        // 根据字符位置计算所在行数
        val lineIndex = layout.getLineForOffset(targetCharIndex)

        // 获取该行顶部的Y坐标
        val lineTop = layout.getLineTop(lineIndex)

        // 补偿LazyColumn的内边距（如果有的话）
        val beforeContentPaddingCompensation =
            (-listState.layoutInfo.viewportStartOffset).coerceAtLeast(0)

        // 计算最终偏移量：将该行的顶部与视口顶部对齐
        val offset = (lineTop + beforeContentPaddingCompensation).toInt().coerceAtLeast(0)

        Logger.d(tag = "NovelScreen") {
            "Restore: paragraphIndex=${resolvedProgress.paragraphIndex}, " +
                    "charIndex=$targetCharIndex, lineIndex=$lineIndex, " +
                    "lineTop=$lineTop, offset=$offset"
        }

        // 执行滚动，将目标行的顶部与视口顶部对齐
        listState.scrollToItem(targetItemIndex, offset)
    }

    LaunchedEffect(state.novel?.id, state.isTranslating) {
        val novel = state.novel ?: return@LaunchedEffect
        val previouslyTranslating = wasTranslating
        wasTranslating = state.isTranslating
        if (!previouslyTranslating && state.isTranslating) {
            val paragraphStartIndex =
                paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())
            paragraphLayouts.clear()
            listState.scrollToItem(paragraphStartIndex, 0)
        } else if (
            shouldRestoreNovelTranslationListAnchor(
                wasTranslating = previouslyTranslating,
                isTranslating = state.isTranslating,
                isTranslated = state.isTranslated,
            )
        ) {
            val anchorToRestore = translationListAnchor
            translationListAnchor = null
            anchorToRestore
                ?.takeIf { it.novelId == novel.id }
                ?.let { anchor ->
                    val paragraphStartIndex =
                        paragraphStartItemIndex(
                            novel.series.title != null,
                            novel.caption.isNotEmpty(),
                        )
                    val resolvedItemIndex = resolveNovelTranslationListAnchorItemIndex(
                        requestedItemIndex = anchor.itemIndex,
                        paragraphStartItemIndex = paragraphStartIndex,
                        paragraphCount = state.paragraphSpans.size,
                    )
                    listState.scrollToItem(
                        index = resolvedItemIndex,
                        scrollOffset = if (resolvedItemIndex == anchor.itemIndex) {
                            anchor.scrollOffset
                        } else {
                            0
                        },
                    )
                }
        }

        if (previouslyTranslating && !state.isTranslating) {
            translationListAnchor = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.addHistory()
        }
    }

    val requestTranslation: () -> Unit = request@{
        val novel = state.novel ?: return@request
        translationListAnchor = NovelTranslationListAnchor(
            novelId = novel.id,
            itemIndex = listState.firstVisibleItemIndex,
            scrollOffset = listState.firstVisibleItemScrollOffset,
        )
        saveReadingProgress()
        viewModel.dispatch(
            NovelIntent.TranslateNovel(forceRefresh = state.isTranslated)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Row(
                    horizontalArrangement = 8.spaceBy
                ) {
                    // 上一章按钮
                    if (state.prevNovelId != null) {
                        FloatingActionButton(
                            onClick = {
                                saveReadingProgress()
                                viewModel.dispatch(NovelIntent.NavigateToChapter(state.prevNovelId))
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(RStrings.chapter_previous)
                            )
                        }
                    }

                    // 下一章按钮
                    if (state.nextNovelId != null) {
                        FloatingActionButton(
                            onClick = {
                                saveReadingProgress()
                                viewModel.dispatch(NovelIntent.NavigateToChapter(state.nextNovelId))
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = stringResource(RStrings.chapter_next)
                            )
                        }
                    }
                }
            }
        },
        contentWindowInsets = if (platform is Platform.Apple.IPhoneOS) {
            // 适配横屏灵动岛
            ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal)
        } else {
            ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.systemBars)
        }
    ) { paddingValues ->
        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }

            state.novel != null -> {
                Box(
                    modifier = Modifier.padding(paddingValues),
                ) {
                    if (isNovelBlocked) {
                        BlockSurface(
                            modifier = Modifier.fillMaxSize(),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.HideImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                )
                            },
                            title = {
                                Text(
                                    text = stringResource(RStrings.novel_hidden),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            button = {
                                Button(
                                    onClick = viewModel::removeBlockNovel
                                ) {
                                    Text(text = stringResource(RStrings.show_novel))
                                }
                            }
                        )
                    } else {
                        NovelReaderContent(
                            state = state,
                            listState = listState,
                            readingProgressFraction = readingProgressFraction,
                            onParagraphTextLayout = { paragraphIndex, layout ->
                                paragraphLayouts[paragraphIndex] = layout
                            },
                            onContentClick = {
                                manuallyShowTopBar = !manuallyShowTopBar
                            },
                            onTagClick = { tag ->
                                navigationManager.navigateToSearchResultScreen(
                                    searchWord = tag,
                                    isIdSearch = false,
                                    searchMode = AppViewMode.NOVEL
                                )
                            },
                            onPixivImageClick = { illustId ->
                                navigationManager.navigateToSinglePictureScreen(illustId)
                            },
                            onAuthorClick = { userId ->
                                navigationManager.navigateToProfileDetailScreen(userId)
                            },
                            onSeriesClick = { seriesId ->
                                navigationManager.navigateToNovelSeriesScreen(seriesId)
                            },
                            onCaptionLinkClick = { url ->
                                when (val target = resolveNovelCaptionLink(url)) {
                                    is NovelCaptionLinkTarget.Illust ->
                                        navigationManager.navigateToSinglePictureScreen(target.id)

                                    is NovelCaptionLinkTarget.Novel ->
                                        navigationManager.navigateToNovelDetailScreen(target.id)

                                    is NovelCaptionLinkTarget.User ->
                                        navigationManager.navigateToProfileDetailScreen(target.id)

                                    is NovelCaptionLinkTarget.External ->
                                        runCatching { uriHandler.openUri(target.url) }

                                    null -> Unit
                                }
                            },
                            onCommentClick = {
                                navigationManager.navigateToCommentScreen(
                                    state.novel.id,
                                    CommentType.NOVEL
                                )
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = showBar,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it })
                    ) {
                        val topBarColor = MaterialTheme.colorScheme.surface
                        TopAppBar(
                            title = {},
                            modifier = Modifier.dropShadow(RectangleShape) {
                                radius = 2f
                                color = topBarColor
                                val isExit = transition.currentState == EnterExitState.Visible &&
                                        transition.targetState == EnterExitState.PostExit
                                alpha = if (isContentVisible && !isExit) 1f else 0f
                            },
                            navigationIcon = {
                                IconButton(onClick = navigationManager::popBackStack) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(RStrings.back)
                                    )
                                }
                            },
                            actions = {
                                if (!isNovelBlocked) {
                                    if (state.isTranslating) {
                                        IconButton(
                                            onClick = {
                                                viewModel.dispatch(NovelIntent.CancelTranslation)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(RStrings.cancel)
                                            )
                                        }
                                    } else if (!state.isTranslated) {
                                        IconButton(onClick = requestTranslation) {
                                            Icon(
                                                imageVector = Icons.Rounded.Translate,
                                                contentDescription = stringResource(
                                                    RStrings.translate_novel
                                                )
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.dispatch(NovelIntent.ToggleBookmark) },
                                        onLongClick = { showBookmarkBottomSheet = true }
                                    ) {
                                        val isBookmark = state.novel.isBookmark
                                        BookmarkIcon(
                                            isBookmarked = isBookmark,
                                            isPrivate = state.novel.isPrivateBookmark,
                                            bookmarkedImageVector = Icons.Rounded.Favorite,
                                            unbookmarkedImageVector = Icons.Rounded.FavoriteBorder,
                                            tint = LocalContentColor.current,
                                            contentDescription = stringResource(RStrings.novel_collection),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.dispatch(
                                                NovelIntent.ToggleMarker(currentMarkerPage)
                                            )
                                        },
                                        enabled = !state.markerUpdating && !state.isTranslating,
                                    ) {
                                        if (state.markerUpdating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (
                                                    state.markerPage == currentMarkerPage
                                                ) {
                                                    Icons.Rounded.Bookmark
                                                } else {
                                                    Icons.Rounded.BookmarkBorder
                                                },
                                                contentDescription = if (state.markerPage != null) {
                                                    stringResource(
                                                        RStrings.novel_marker_page,
                                                        state.markerPage,
                                                    )
                                                } else {
                                                    stringResource(RStrings.novel_marker)
                                                },
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { showMetadataBottomSheet = true }
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = stringResource(
                                                RStrings.novel_work_information
                                            )
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dispatch(NovelIntent.ToggleBottomSheet) }
                                    ) {
                                        Icon(
                                            Icons.Rounded.MoreVert,
                                            contentDescription = stringResource(RStrings.more)
                                        )
                                    }
                                }
                            },
                            windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Top),
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    if (showBookmarkBottomSheet && state.novel != null) {
        val bottomSheetState = rememberModalBottomSheetState(true)
        NovelBottomBookmarkSheet(
            hideBottomSheet = { showBookmarkBottomSheet = false },
            novel = state.novel,
            bottomSheetState = bottomSheetState,
            onBookmarkClick = { restrict, tags, isEdit ->
                if (isEdit || !state.novel.isBookmark) {
                    BookmarkState.bookmarkNovel(state.novel.id, restrict, tags)
                } else {
                    BookmarkState.deleteBookmarkNovel(state.novel.id)
                }
            }
        )
    }

    if (showMetadataBottomSheet && state.novel != null && !isNovelBlocked) {
        NovelMetadataBottomSheet(
            novel = state.novel,
            onDismissRequest = { showMetadataBottomSheet = false },
            onAuthorClick = { userId ->
                showMetadataBottomSheet = false
                navigationManager.navigateToProfileDetailScreen(userId)
            },
            onSeriesClick = { seriesId ->
                showMetadataBottomSheet = false
                navigationManager.navigateToNovelSeriesScreen(seriesId)
            },
            onTagClick = { tag ->
                showMetadataBottomSheet = false
                navigationManager.navigateToSearchResultScreen(
                    searchWord = tag,
                    isIdSearch = false,
                    searchMode = AppViewMode.NOVEL
                )
            },
            onCaptionLinkClick = { url ->
                showMetadataBottomSheet = false
                when (val target = resolveNovelCaptionLink(url)) {
                    is NovelCaptionLinkTarget.Illust ->
                        navigationManager.navigateToSinglePictureScreen(target.id)

                    is NovelCaptionLinkTarget.Novel ->
                        navigationManager.navigateToNovelDetailScreen(target.id)

                    is NovelCaptionLinkTarget.User ->
                        navigationManager.navigateToProfileDetailScreen(target.id)

                    is NovelCaptionLinkTarget.External ->
                        runCatching { uriHandler.openUri(target.url) }

                    null -> Unit
                }
            },
            onCommentClick = {
                showMetadataBottomSheet = false
                navigationManager.navigateToCommentScreen(
                    state.novel.id,
                    CommentType.NOVEL
                )
            },
        )
    }

    // BottomSheet
    if (state.showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dispatch(NovelIntent.ToggleBottomSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            NovelBottomSheetContent(
                state = state,
                onFontSizeChange = {
                    saveReadingProgress()
                    viewModel.dispatch(NovelIntent.UpdateFontSize(it))
                },
                onLineSpacingChange = {
                    saveReadingProgress()
                    viewModel.dispatch(NovelIntent.UpdateLineSpacing(it))
                },
                onExport = { viewModel.dispatch(NovelIntent.ExportToTxt) },
                onShare = { viewModel.dispatch(NovelIntent.ShareNovel) },
                onToggleDisplayedText = {
                    viewModel.dispatch(NovelIntent.ToggleDisplayOriginalText)
                },
                onDeleteTranslation = {
                    viewModel.dispatch(NovelIntent.DeleteNovelTranslation)
                },
                onRegenerateTranslation = {
                    viewModel.dispatch(NovelIntent.ToggleBottomSheet)
                    requestTranslation()
                },
                onAiSetting = {
                    viewModel.dispatch(NovelIntent.ToggleBottomSheet)
                    navigationManager.navigateToAiTranslationSettingScreen()
                },
                isNovelBlocked = isNovelBlocked,
                onBlockNovel = {
                    if (isNovelBlocked) {
                        viewModel.removeBlockNovel()
                    } else {
                        viewModel.blockNovel()
                    }
                    viewModel.dispatch(NovelIntent.ToggleBottomSheet)
                },
            )
        }
    }
}

@Composable
private fun NovelBottomSheetContent(
    state: NovelState,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Int) -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onToggleDisplayedText: () -> Unit,
    onDeleteTranslation: () -> Unit,
    onRegenerateTranslation: () -> Unit,
    onAiSetting: () -> Unit,
    isNovelBlocked: Boolean,
    onBlockNovel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        val colors =
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

        // 字号调整
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(RStrings.font_size_value, state.fontSize),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                Slider(
                    value = state.fontSize.toFloat(),
                    onValueChange = { onFontSizeChange(it.roundToInt()) },
                    valueRange = 10f..32f,
                    steps = 21
                )
            },
            colors = colors
        )

        // 行间距调整
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(
                        RStrings.line_spacing_value,
                        (if (state.lineSpacingSp >= 0) "+" else "") + state.lineSpacingSp.toString()
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                Slider(
                    value = state.lineSpacingSp.toFloat(),
                    onValueChange = { onLineSpacingChange(it.roundToInt()) },
                    valueRange = -10f..10f,
                    steps = 19
                )
            },
            colors = colors
        )

        // 导出按钮
        ListItem(
            headlineContent = { Text(text = stringResource(RStrings.export_txt_button)) },
            modifier = Modifier
                .fillMaxWidth()
                .throttleClick(onClick = onExport),
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = stringResource(RStrings.export_txt_button)
                )
            },
            colors = colors
        )

        // 分享按钮
        ListItem(
            headlineContent = { Text(text = stringResource(RStrings.share_link)) },
            modifier = Modifier
                .fillMaxWidth()
                .throttleClick(onClick = onShare),
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(RStrings.share_link)
                )
            },
            colors = colors
        )

        state.novel?.let { novel ->
            ListItem(
                headlineContent = { Text(text = stringResource(RStrings.read_later)) },
                trailingContent = {
                    NovelReadLaterButton(
                        novel = novel,
                        tint = LocalContentColor.current,
                    )
                },
                colors = colors
            )
        }

        if (state.isTranslated && !state.isTranslating) {
            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.regenerate_translation))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .throttleClick(onClick = onRegenerateTranslation),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(RStrings.regenerate_translation)
                    )
                },
                colors = colors
            )

            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(
                            if (state.isShowingOriginalText) {
                                RStrings.show_translated_text
                            } else {
                                RStrings.show_original_text
                            }
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .throttleClick(onClick = onToggleDisplayedText),
                leadingContent = {
                    Icon(
                        imageVector = if (state.isShowingOriginalText) {
                            Icons.Rounded.Translate
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = stringResource(
                            if (state.isShowingOriginalText) {
                                RStrings.show_translated_text
                            } else {
                                RStrings.show_original_text
                            }
                        )
                    )
                },
                colors = colors
            )

            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.delete_translation))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .throttleClick(onClick = onDeleteTranslation),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(RStrings.delete_translation)
                    )
                },
                colors = colors
            )
        }

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(
                        if (isNovelBlocked) RStrings.show_novel else RStrings.hide_novel
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .throttleClick(onClick = onBlockNovel),
            leadingContent = {
                Icon(
                    imageVector = if (isNovelBlocked) Icons.Rounded.Image else Icons.Rounded.HideImage,
                    contentDescription = stringResource(
                        if (isNovelBlocked) RStrings.show_novel else RStrings.hide_novel
                    )
                )
            },
            colors = colors
        )

        ListItem(
            headlineContent = { Text(text = stringResource(RStrings.ai_translation_setting)) },
            modifier = Modifier
                .fillMaxWidth()
                .throttleClick(onClick = onAiSetting),
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(RStrings.ai_translation_setting)
                )
            },
            colors = colors
        )
    }
}
