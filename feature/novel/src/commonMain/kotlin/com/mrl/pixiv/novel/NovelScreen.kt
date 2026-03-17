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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.mrl.pixiv.common.compose.layout.isWidthAtLeastMedium
import com.mrl.pixiv.common.compose.ui.TagItem
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.NovelReadingProgress
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.convertUtcStringToLocalDateTime
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.back
import com.mrl.pixiv.strings.bookmark
import com.mrl.pixiv.strings.bookmarked
import com.mrl.pixiv.strings.chapter_next
import com.mrl.pixiv.strings.chapter_previous
import com.mrl.pixiv.strings.cover
import com.mrl.pixiv.strings.export_txt_button
import com.mrl.pixiv.strings.font_size_value
import com.mrl.pixiv.strings.line_spacing_value
import com.mrl.pixiv.strings.more
import com.mrl.pixiv.strings.share_link
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val KEY_COVER = "cover"
private const val KEY_TITLE = "title"
private const val KEY_SERIES_TITLE = "series_title"
private const val KEY_STATS = "stats"
private const val KEY_CREATE_DATE = "create_date"
private const val KEY_TAGS = "tags"
private const val KEY_CAPTION = "caption"
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
            delay(2000) // 2秒后自动隐藏
            manuallyShowTopBar = false
        }
    }

    val saveReadingProgress = remember(state.novel?.id, listState) {
        {
            val novel = state.novel ?: return@remember
            if (state.paragraphs.isEmpty()) return@remember
            val paragraphStartIndex =
                paragraphStartItemIndex(novel.series.title != null, novel.caption.isNotEmpty())
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

        // 等待目标段落的布局完成
        val layout = snapshotFlow { paragraphLayouts[resolvedProgress.paragraphIndex] }
            .filterNotNull()
            .first()

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.systemBars)
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
                        }
                    )
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
                onShare = { viewModel.dispatch(NovelIntent.ShareNovel) }
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
    onTagClick: (String) -> Unit
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
            count = state.paragraphs.size,
            // 两个段落内容相同，hashcode也一样，这样就会导致列表状态异常，所以这里直接用index作为key
            key = { it }
        ) { index ->
            val paragraph = state.paragraphs[index]
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = state.fontSize.sp,
                    lineHeight = (state.fontSize + state.lineSpacingSp + 8).sp
                ),
                onTextLayout = { layoutResult ->
                    onParagraphTextLayout(index, layoutResult)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .combinedClickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onContentClick
                    )
            )
        }

        item(key = KEY_SPACER_END) { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

private fun paragraphStartItemIndex(
    hasSeriesTitle: Boolean,
    hasCaption: Boolean
): Int {
    var itemCountBeforeParagraphs = 6 // cover + title + stats + create_date + tags + divider
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
    val firstVisibleIndex = firstVisibleItem.index
    if (firstVisibleIndex !in paragraphStartIndex until (paragraphStartIndex + paragraphCount)) {
        return null
    }

    val paragraphIndex = (firstVisibleIndex - paragraphStartIndex).coerceIn(0, paragraphCount - 1)
    val paragraphLayout = paragraphLayouts[paragraphIndex] ?: return null

    // 计算视口顶部相对于段落的Y坐标
    val yInParagraph = (layoutInfo.viewportStartOffset - firstVisibleItem.offset)
        .coerceIn(0, firstVisibleItem.size - 1)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 导出按钮
        TextButton(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(RStrings.export_txt_button))
        }

        HorizontalDivider()

        // 字号调整
        Column {
            Text(
                text = stringResource(RStrings.font_size_value, state.fontSize),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = state.fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 10f..32f,
                steps = 21
            )
        }

        HorizontalDivider()

        // 行间距调整
        Column {
            Text(
                text = stringResource(
                    RStrings.line_spacing_value,
                    if (state.lineSpacingSp >= 0) "+" else "" + state.lineSpacingSp.toString()
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = state.lineSpacingSp.toFloat(),
                onValueChange = { onLineSpacingChange(it.toInt()) },
                valueRange = -10f..10f,
                steps = 19
            )
        }

        HorizontalDivider()

        // 分享按钮
        TextButton(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null)
            8.HSpacer
            Text(stringResource(RStrings.share_link))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
