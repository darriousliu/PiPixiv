package com.mrl.pixiv.novel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.ui.BlockSurface
import com.mrl.pixiv.common.compose.ui.TagItem
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.NovelReadingProgress
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.CommentType
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.ai_translation_setting
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.bookmark
import com.mrl.pixiv.strings.bookmarked
import com.mrl.pixiv.strings.chapter_next
import com.mrl.pixiv.strings.chapter_previous
import com.mrl.pixiv.strings.cover
import com.mrl.pixiv.strings.delete_translation
import com.mrl.pixiv.strings.export_txt_button
import com.mrl.pixiv.strings.font_size_value
import com.mrl.pixiv.strings.hide_novel
import com.mrl.pixiv.strings.line_spacing_value
import com.mrl.pixiv.strings.more
import com.mrl.pixiv.strings.novel_hidden
import com.mrl.pixiv.strings.regenerate_translation
import com.mrl.pixiv.strings.share_link
import com.mrl.pixiv.strings.show_novel
import com.mrl.pixiv.strings.show_original_text
import com.mrl.pixiv.strings.show_translated_text
import com.mrl.pixiv.strings.translate_novel
import com.mrl.pixiv.strings.view_comments
import com.mrl.pixiv.strings.view_comments_count
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

private const val KEY_COVER = "cover"
private const val KEY_TITLE = "title"
private const val KEY_SERIES_TITLE = "series_title"
private const val KEY_AUTHOR = "author"
private const val KEY_STATS = "stats"
private const val KEY_CREATE_DATE = "create_date"
private const val KEY_TAGS = "tags"
private const val KEY_CAPTION = "caption"
private const val KEY_VIEW_COMMENTS = "view_comments"
private const val KEY_DIVIDER = "divider"
private const val KEY_SPACER_END = "spacer_end"

@Composable
fun NovelScreen(
    novelId: Long,
    modifier: Modifier = Modifier,
    viewModel: NovelViewModel = koinViewModel { parametersOf(novelId) },
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val currentNovelId = state.novel?.id ?: novelId
    val isNovelBlocked = BlockingRepositoryV2.collectNovelBlockAsState(currentNovelId)
    val listState = rememberLazyListState()
    val paragraphLayouts = remember(state.novel?.id) { mutableStateMapOf<Int, TextLayoutResult>() }

    // 沉浸逻辑: 滚动到正文区域时隐藏TopBar和FAB
    val isContentVisible by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key is Int // index
        }
    }
    var manuallyShowTopBar by remember { mutableStateOf(false) }
    val showBar = !isContentVisible || manuallyShowTopBar

    LaunchedEffect(manuallyShowTopBar) {
        if (manuallyShowTopBar) {
            delay(3000) // 3秒后自动隐藏
            manuallyShowTopBar = false
        }
    }

    val saveReadingProgress = remember(state.novel?.id, listState) {
        {
            val novel = state.novel ?: return@remember
            if (state.paragraphs.isEmpty()) return@remember
            val paragraphStartIndex =
                paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())
            val firstVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: return@remember
            val contentRange =
                paragraphStartIndex until (paragraphStartIndex + state.paragraphs.size)
            if (firstVisibleItemIndex !in contentRange) {
                viewModel.clearProgress(novelId = novel.id)
                return@remember
            }
            val progress = buildVisibleReadingProgress(
                listState = listState,
                paragraphStartIndex = paragraphStartIndex,
                paragraphCount = state.paragraphs.size,
                paragraphLayouts = paragraphLayouts,
                paragraphs = state.paragraphs
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

    LaunchedEffect(state.restoreVersion, state.novel?.id) {
        val novel = state.novel ?: return@LaunchedEffect
        val resolvedProgress = state.restoreProgress ?: return@LaunchedEffect
        if (state.paragraphs.isEmpty()) return@LaunchedEffect
        val paragraphStartIndex =
            paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())

        val targetItemIndex = paragraphStartIndex + resolvedProgress.paragraphIndex
        Logger.d(tag = "NovelScreen") { "Restore: paragraphStartIndex=$paragraphStartIndex, targetItemIndex=$targetItemIndex" }

        // 清空布局缓存并滚动到目标段落
        paragraphLayouts.clear()
        listState.scrollToItem(targetItemIndex, 0)

        // 等待目标段落的布局完成。包含图片标记的段落可能没有文本布局，这里做超时兜底。
        val layout = withTimeoutOrNull(500L) {
            while (paragraphLayouts[resolvedProgress.paragraphIndex] == null) {
                delay(16)
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
                        NovelContent(
                            state = state,
                            listState = listState,
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
                            onCommentClick = {
                                navigationManager.navigateToCommentScreen(novelId, CommentType.NOVEL)
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
                                    IconButton(
                                        onClick = {
                                            if (!state.isTranslating) {
                                                viewModel.dispatch(
                                                    NovelIntent.TranslateNovel(forceRefresh = state.isTranslated)
                                                )
                                            }
                                        }
                                    ) {
                                        if (state.isTranslating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (state.isTranslated) {
                                                    Icons.Rounded.Refresh
                                                } else {
                                                    Icons.Rounded.Translate
                                                },
                                                contentDescription = stringResource(
                                                    if (state.isTranslated) {
                                                        RStrings.regenerate_translation
                                                    } else {
                                                        RStrings.translate_novel
                                                    }
                                                )
                                            )
                                        }
                                    }
                                    if (state.isTranslated) {
                                        IconButton(
                                            onClick = { viewModel.dispatch(NovelIntent.ToggleDisplayOriginalText) }
                                        ) {
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
                                        }
                                        IconButton(
                                            onClick = { viewModel.dispatch(NovelIntent.DeleteNovelTranslation) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = stringResource(RStrings.delete_translation)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.dispatch(NovelIntent.ToggleBookmark) }
                                    ) {
                                        val isBookmark = state.novel.isBookmark
                                        Icon(
                                            imageVector = if (isBookmark) {
                                                Icons.Rounded.Bookmark
                                            } else {
                                                Icons.Rounded.BookmarkBorder
                                            },
                                            contentDescription = stringResource(if (isBookmark) RStrings.bookmarked else RStrings.bookmark)
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
private fun NovelContent(
    state: NovelState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onParagraphTextLayout: (Int, TextLayoutResult) -> Unit,
    onContentClick: () -> Unit = {},
    onTagClick: (String) -> Unit,
    onPixivImageClick: (Long) -> Unit,
    onAuthorClick: (Long) -> Unit,
    onCommentClick: () -> Unit,
) {
    val novel = state.novel ?: return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Vertical).asPaddingValues(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面图
        item(key = KEY_COVER) {
            val isWidthAtLeastMedium = currentWindowAdaptiveInfo().isWidthAtLeastMedium
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(novel.imageUrls.medium)
                    .build(),
                contentDescription = stringResource(RStrings.cover),
                modifier = Modifier
                    .padding(top = 56.dp)
                    .fillMaxWidth(if (isWidthAtLeastMedium) 0.2f else 0.4f),
                contentScale = ContentScale.FillWidth,
                placeholder = rememberVectorPainter(Icons.Rounded.Refresh),
                error = rememberVectorPainter(Icons.Rounded.ErrorOutline),
            )
        }

        // 标题
        item(key = KEY_TITLE) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
        }

        item(key = KEY_AUTHOR) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .throttleClick { onAuthorClick(novel.user.id) }
            ) {
                UserAvatar(
                    url = novel.user.profileImageUrls.medium,
                    modifier = Modifier.size(36.dp),
                    onClick = { onAuthorClick(novel.user.id) }
                )
                8.HSpacer
                Text(
                    text = novel.user.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }
        }

        // 系列标题
        novel.series.title?.let { seriesTitle ->
            item(key = KEY_SERIES_TITLE) {
                Text(
                    text = seriesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 收藏数和观看数
        item(key = KEY_STATS) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                4.HSpacer
                Text(
                    text = novel.totalBookmarks.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )

                16.HSpacer

                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                4.HSpacer
                Text(
                    text = novel.totalView.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 创建时间
        item(key = KEY_CREATE_DATE) {
            Text(
                text = convertUtcStringToLocalDateTime(novel.createDate),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // 标签
        item(key = KEY_TAGS) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = 5f.spaceBy,
                verticalArrangement = 5f.spaceBy,
            ) {
                novel.tags.forEach { tag ->
                    TagItem(
                        tag = tag,
                        onClick = {
                            onTagClick(tag.name)
                        }
                    )
                }
            }
        }

        // Caption卡片(如果有内容)
        if (novel.caption.isNotEmpty()) {
            item(key = KEY_CAPTION) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = novel.caption,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = state.fontSize.sp,
                            lineHeight = (state.fontSize + state.lineSpacingSp + 8).sp
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        item(key = KEY_VIEW_COMMENTS) {
            Row(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
                    .throttleClick(indication = ripple()) {
                        onCommentClick()
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = stringResource(RStrings.view_comments)
                )
                5.HSpacer
                Text(
                    text = if (novel.totalComments != null) {
                        stringResource(
                            RStrings.view_comments_count,
                            novel.totalComments!!
                        )
                    } else {
                        stringResource(RStrings.view_comments)
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // 正文分隔线
        item(key = KEY_DIVIDER) {
            HorizontalDivider(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
            )
        }


        // 正文段落
        items(
            count = state.paragraphSpans.size,
            // 两个段落内容相同，hashcode也一样，这样就会导致列表状态异常，所以这里直接用index作为key
            key = { it }
        ) { index ->
            NovelParagraph(
                paragraphIndex = index,
                fontSize = state.fontSize,
                lineSpacingSp = state.lineSpacingSp,
                span = state.paragraphSpans[index],
                onParagraphTextLayout = onParagraphTextLayout,
                onContentClick = onContentClick,
                onPixivImageClick = onPixivImageClick,
            )
        }

        item(key = KEY_SPACER_END) { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

private data class ParagraphRenderData(
    val annotatedText: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>,
)

@Composable
private fun NovelParagraph(
    paragraphIndex: Int,
    fontSize: Int,
    lineSpacingSp: Int,
    span: NovelSpanData,
    onParagraphTextLayout: (Int, TextLayoutResult) -> Unit,
    onContentClick: () -> Unit,
    onPixivImageClick: (Long) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize.sp,
        lineHeight = (fontSize + lineSpacingSp + 8).sp
    )
    val renderData = remember(span, linkColor, uriHandler, onPixivImageClick, paragraphIndex) {
        buildParagraphRenderData(
            span = span,
            paragraphIndex = paragraphIndex,
            linkColor = linkColor,
            uriHandler = uriHandler,
            onPixivImageClick = onPixivImageClick,
            textStyle = textStyle,
        )
    }

    val baseTextModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .combinedClickable(
            interactionSource = null,
            indication = null,
            onClick = onContentClick
        )

    val hasVisibleText =
        renderData.annotatedText.text.isNotBlank() || renderData.inlineContent.isNotEmpty()

    Text(
        text = if (hasVisibleText) renderData.annotatedText else AnnotatedString("\u200B"),
        style = if (hasVisibleText) textStyle else TextStyle(fontSize = 1.sp, lineHeight = 1.sp),
        color = if (hasVisibleText) Color.Unspecified else Color.Transparent,
        inlineContent = renderData.inlineContent,
        onTextLayout = { layoutResult ->
            onParagraphTextLayout(paragraphIndex, layoutResult)
        },
        modifier = baseTextModifier,
    )
}

private fun buildParagraphRenderData(
    span: NovelSpanData,
    paragraphIndex: Int,
    linkColor: Color,
    uriHandler: UriHandler,
    onPixivImageClick: (Long) -> Unit,
    textStyle: TextStyle,
): ParagraphRenderData {
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()
    val annotatedText = buildAnnotatedString {
        when (span) {
            is NovelSpanData.Text -> append(span.value)
            is NovelSpanData.JumpUri -> {
                val start = length
                append(span.value)
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = start,
                    end = length
                )
                addLink(
                    url = LinkAnnotation.Url(span.url) {
                        uriHandler.openUri(span.url)
                    },
                    start = start,
                    end = length
                )
            }

            is NovelSpanData.PixivImage -> {
                val inlineId = "pixiv_image_${paragraphIndex}_${span.illustId}_${span.targetIndex}"
                appendInlineContent(inlineId, "[pixivimage]")
                inlineContentMap[inlineId] = InlineTextContent(
                    placeholder = Placeholder(
                        width = 220.sp,
                        height = 180.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    val imageUrl = span.imageUrl
                    if (imageUrl.isNullOrBlank()) {
                        Text(text = span.token, style = textStyle)
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(imageUrl)
                                .build(),
                            contentDescription = span.token,
                            contentScale = ContentScale.FillBounds,
                            placeholder = rememberVectorPainter(Icons.Rounded.Refresh),
                            error = rememberVectorPainter(Icons.Rounded.ErrorOutline),
                            modifier = Modifier
                                .fillMaxSize()
                                .combinedClickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClick = { onPixivImageClick(span.illustId) }
                                ),
                        )
                    }
                }
            }

            is NovelSpanData.UploadedImage -> {
                val inlineId = "uploaded_image_${paragraphIndex}"
                appendInlineContent(inlineId, "[uploadedimage]")
                inlineContentMap[inlineId] = InlineTextContent(
                    placeholder = Placeholder(
                        width = 220.sp,
                        height = 180.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(span.url)
                            .build(),
                        contentDescription = span.url,
                        contentScale = ContentScale.FillBounds,
                        placeholder = rememberVectorPainter(Icons.Rounded.Refresh),
                        error = rememberVectorPainter(Icons.Rounded.ErrorOutline),
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                interactionSource = null,
                                indication = null,
                                onClick = { uriHandler.openUri(span.url) }
                            ),
                    )
                }
            }

            NovelSpanData.NewPage -> append("\n")
        }
    }

    return ParagraphRenderData(
        annotatedText = annotatedText,
        inlineContent = inlineContentMap,
    )
}

private fun paragraphStartItemIndex(
    hasSeriesTitle: Boolean,
    hasCaption: Boolean
): Int {
    // cover + title + author + stats + create_date + tags + comments + divider
    var itemCountBeforeParagraphs = 8
    if (hasSeriesTitle) itemCountBeforeParagraphs += 1
    if (hasCaption) itemCountBeforeParagraphs += 1
    return itemCountBeforeParagraphs
}

private fun buildVisibleReadingProgress(
    listState: LazyListState,
    paragraphStartIndex: Int,
    paragraphCount: Int,
    paragraphLayouts: Map<Int, TextLayoutResult>,
    paragraphs: List<String>
): NovelReadingProgress? {
    if (paragraphCount <= 0) return null
    val layoutInfo = listState.layoutInfo
    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    val contentRange = paragraphStartIndex until (paragraphStartIndex + paragraphCount)
    if (firstVisibleItem.index !in contentRange) {
        return null
    }

    val textVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
        val paragraphIndex = itemInfo.index - paragraphStartIndex
        paragraphIndex in 0 until paragraphCount && paragraphLayouts[paragraphIndex] != null
    } ?: return null

    val paragraphIndex =
        (textVisibleItem.index - paragraphStartIndex).coerceIn(0, paragraphCount - 1)
    val paragraphLayout = paragraphLayouts[paragraphIndex] ?: return null

    // 计算视口顶部相对于段落的Y坐标
    val yInParagraph = (layoutInfo.viewportStartOffset - textVisibleItem.offset)
        .coerceIn(0, textVisibleItem.size - 1)
        .toFloat()

    // 获取视口顶部对应的字符位置
    val charAtViewportTop = paragraphLayout.getOffsetForPosition(
        position = Offset(x = 0f, y = yInParagraph)
    )

    // 获取该字符所在的行号
    val lineIndex = paragraphLayout.getLineForOffset(charAtViewportTop)

    // 获取该行的第一个字符位置（行首字符）
    val lineStartChar = paragraphLayout.getLineStart(lineIndex)

    Logger.d(tag = "NovelScreen") {
        "Save: paragraphIndex=$paragraphIndex, lineIndex=$lineIndex, " +
                "lineStartChar=$lineStartChar, yInParagraph=$yInParagraph"
    }

    val paragraphHash = paragraphs[paragraphIndex].hashCode()
    return NovelReadingProgress(
        paragraphIndex = paragraphIndex,
        charIndex = lineStartChar,
        paragraphHash = paragraphHash
    )
}

@Composable
private fun NovelBottomSheetContent(
    state: NovelState,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Int) -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onAiSetting: () -> Unit,
    isNovelBlocked: Boolean,
    onBlockNovel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
