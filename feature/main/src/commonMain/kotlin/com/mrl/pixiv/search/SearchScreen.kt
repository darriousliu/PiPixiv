package com.mrl.pixiv.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.rememberThrottleClick
import com.mrl.pixiv.common.compose.ui.ViewModeToggleButton
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.router.DestinationsDeepLink
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.PixivLinkTarget
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.readTextFromClipboard
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.clear
import com.mrl.pixiv.strings.enter_keywords
import com.mrl.pixiv.strings.find_for
import com.mrl.pixiv.strings.id_search
import com.mrl.pixiv.strings.illust
import com.mrl.pixiv.strings.novel
import com.mrl.pixiv.strings.search_history
import com.mrl.pixiv.strings.select_pixiv_link
import com.mrl.pixiv.strings.users
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val dispatch = viewModel::dispatch
    val state = viewModel.asState()
    val appViewMode by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle { appViewMode }
    val readClipboardOnSearch by SettingRepository.userPreferenceFlow
        .collectAsStateWithLifecycle { readClipboardOnSearch }
    val searchHistory by remember(appViewMode) {
        when (appViewMode) {
            AppViewMode.ILLUST -> SearchRepository.searchHistoryFlow.map { it.searchHistoryList }
            AppViewMode.NOVEL -> SearchRepository.novelSearchHistoryFlow.map { it.novelSearchHistory }
        }
    }.collectAsStateWithLifecycle(emptyList())
    val searchIdHistory by remember(appViewMode) {
        when (appViewMode) {
            AppViewMode.ILLUST -> SearchRepository.searchIdHistoryFlow.map {
                it?.toList().orEmpty()
            }

            AppViewMode.NOVEL -> SearchRepository.novelSearchIdHistoryFlow.map {
                it?.toList().orEmpty()
            }
        }
    }.collectAsStateWithLifecycle(emptyList())
    val searchTextState = rememberTextFieldState(viewModel.searchWords)
    var pendingLinks by remember { mutableStateOf<List<PixivLinkTarget>>(emptyList()) }
    fun handlePixivLinks(
        text: String,
        alwaysShowSelection: Boolean = false,
    ): Boolean {
        return when (val action = resolvePixivLinkSearchAction(text, alwaysShowSelection)) {
            PixivLinkSearchAction.NoMatch -> false
            is PixivLinkSearchAction.Open -> {
                navigationManager.navigate(action.link.toDestination())
                true
            }

            is PixivLinkSearchAction.ShowSelection -> {
                pendingLinks = action.links
                true
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    LifecycleResumeEffect(readClipboardOnSearch) {
        val clipboardJob = coroutineScope.launch {
            val handledClipboardLink = if (readClipboardOnSearch) {
                val clipboardText = readTextFromClipboard().orEmpty()
                viewModel.isClipboardTextChanged(clipboardText) && handlePixivLinks(
                    text = clipboardText,
                    alwaysShowSelection = true,
                )
            } else {
                false
            }
            if (!handledClipboardLink) {
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {
                }
                searchTextState.edit { placeCursorAtEnd() }
            }
        }
        onPauseOrDispose { clipboardJob.cancel() }
    }
    LaunchedEffect(searchTextState) {
        // 文本或光标变化时同步搜索词，并按光标所在关键词刷新补全
        snapshotFlow { searchTextState.text.toString() to searchTextState.selection }
            .drop(1)
            .collect { (text, selection) ->
                dispatch(SearchAction.UpdateSearchWords(text))
                val token = completionToken(text, selection)
                if (token.text.isNotBlank()) {
                    dispatch(SearchAction.SearchAutoComplete(token.text))
                } else {
                    dispatch(SearchAction.ClearAutoCompleteSearchWords)
                }
            }
    }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    if (pendingLinks.isNotEmpty()) {
        PixivLinkSelectionDialog(
            links = pendingLinks,
            onSelect = { link ->
                pendingLinks = emptyList()
                navigationManager.navigate(link.toDestination())
            },
            onDismiss = { pendingLinks = emptyList() },
        )
    }

    Scaffold(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            focusManager.clearFocus()
            softwareKeyboardController?.hide()
        },
        topBar = {
            SearchScreenAppBar(
                state = searchTextState,
                focusRequester = focusRequester,
                onBack = { navigationManager.popBackStack() },
                onSearch = search@{
                    val searchWord = searchTextState.text.toString()
                    if (handlePixivLinks(searchWord)) {
                        focusRequester.freeFocus()
                        return@search
                    }
                    if (state.isIdSearch) {
                        viewModel.addSearchIdHistory(searchWord)
                    } else {
                        dispatch(SearchAction.AddSearchHistory(searchWord))
                    }
                    focusRequester.freeFocus()
                    navigationManager.navigateToSearchResultScreen(
                        searchWord = searchWord,
                        isIdSearch = state.isIdSearch,
                        searchMode = appViewMode
                    )
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.imePadding()
            ) {
                FloatingActionButton(
                    onClick = { dispatch(SearchAction.UpdateIsIdSearch(!state.isIdSearch)) }
                ) {
                    Text(
                        text = stringResource(RStrings.id_search),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textDecoration = if (state.isIdSearch) null else TextDecoration.LineThrough,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                8.VSpacer
                ViewModeToggleButton(
                    currentMode = appViewMode,
                    onModeChange = { newMode ->
                        viewModel.switchViewMode(newMode)
                    }
                )
            }
        }
    ) {
        // 用LazyColumn构造自动补全列表，点击跳转搜索结果页面
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(top = 8.dp)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalArrangement = 16f.spaceBy
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Text(
                        text = if (searchTextState.text.isEmpty())
                            stringResource(RStrings.search_history)
                        else
                            stringResource(RStrings.find_for),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                }
            }
            if (searchTextState.text.isEmpty()) {
                if (state.isIdSearch) {
                    items(
                        items = searchIdHistory,
                        key = { it }
                    ) {
                        ListItem(
                            onClick = rememberThrottleClick {
                                viewModel.addSearchIdHistory(it)
                                focusRequester.freeFocus()
                                navigationManager.navigateToSearchResultScreen(
                                    searchWord = it,
                                    isIdSearch = true,
                                    searchMode = appViewMode
                                )
                            },
                            shapes = ListItemDefaults.shapes(shape = RectangleShape),
                            content = { Text(text = it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            trailingContent = {
                                Icon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .throttleClick(indication = ripple()) {
                                            viewModel.deleteSearchIdHistory(it)
                                        },
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "delete"
                                )
                            },
                        )
                    }
                } else {
                    items(
                        items = searchHistory,
                        key = { it.keyword }
                    ) { item ->
                        ListItem(
                            onClick = rememberThrottleClick {
                                dispatch(SearchAction.AddSearchHistory(item.keyword))
                                focusRequester.freeFocus()
                                navigationManager.navigateToSearchResultScreen(
                                    searchWord = item.keyword,
                                    isIdSearch = false,
                                    searchMode = appViewMode
                                )
                            },
                            shapes = ListItemDefaults.shapes(shape = RectangleShape),
                            content = {
                                Text(
                                    text = item.keyword,
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                                .animateItem(),
                            trailingContent = {
                                Icon(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .throttleClick(indication = ripple()) {
                                            dispatch(SearchAction.DeleteSearchHistory(item.keyword))
                                        },
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "delete"
                                )
                            },
                        )
                    }
                }
            } else {
                items(
                    items = state.autoCompleteSearchWords,
                    key = { it.name }
                ) { word ->
                    ListItem(
                        onClick = rememberThrottleClick {
                            val query = searchTextState.completionToken()
                                .replaceIn(searchTextState.text.toString(), word.name)
                            dispatch(SearchAction.AddSearchHistory(query))
                            focusRequester.freeFocus()
                            navigationManager.navigateToSearchResultScreen(
                                searchWord = query,
                                isIdSearch = state.isIdSearch,
                                searchMode = appViewMode
                            )
                        },
                        onLongClick = {
                            coroutineScope.launch {
                                com.mrl.pixiv.common.util.copyToClipboard(word.name)
                            }
                        },
                        shapes = ListItemDefaults.shapes(shape = RectangleShape),
                        content = {
                            Text(
                                text = word.name,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        supportingContent = {
                            if (word.translatedName.isNotBlank()) {
                                Text(
                                    text = word.translatedName,
//                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PixivLinkSelectionDialog(
    links: List<PixivLinkTarget>,
    onSelect: (PixivLinkTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(RStrings.select_pixiv_link)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                links.forEach { link ->
                    val type = when (link) {
                        is PixivLinkTarget.Illust -> stringResource(RStrings.illust)
                        is PixivLinkTarget.Novel -> stringResource(RStrings.novel)
                        is PixivLinkTarget.User -> stringResource(RStrings.users)
                    }
                    ListItem(
                        onClick = rememberThrottleClick {
                            onSelect(link)
                        },
                        shapes = ListItemDefaults.shapes(shape = RectangleShape),
                        content = { Text(text = "$type #${link.id}") },
                        supportingContent = { Text(text = link.url) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(RStrings.cancel))
            }
        },
    )
}

@Composable
private fun SearchScreenAppBar(
    state: TextFieldState,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {},
        modifier = modifier,
        actions = {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    TextField(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .throttleClick {
                                focusRequester.requestFocus()
                            },
                        placeholder = { Text(stringResource(RStrings.enter_keywords)) },
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                        ),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        shape = MaterialTheme.shapes.extraLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        onKeyboardAction = { onSearch() },
                        trailingIcon = if (shouldShowSearchInputClearIcon(state.text.toString())) {
                            {
                                IconButton(
                                    onClick = { state.clearText() },
                                    shapes = IconButtonDefaults.shapes(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(RStrings.clear),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    )
}

internal fun shouldShowSearchInputClearIcon(input: String): Boolean = input.isNotEmpty()

internal fun resolvePixivLinkSearchAction(
    text: String,
    alwaysShowSelection: Boolean,
): PixivLinkSearchAction {
    val links = DestinationsDeepLink.findLinks(text)
    return when {
        links.isEmpty() -> PixivLinkSearchAction.NoMatch
        alwaysShowSelection || links.size > 1 -> PixivLinkSearchAction.ShowSelection(links)
        else -> PixivLinkSearchAction.Open(links.single())
    }
}

internal sealed interface PixivLinkSearchAction {
    data object NoMatch : PixivLinkSearchAction
    data class Open(val link: PixivLinkTarget) : PixivLinkSearchAction
    data class ShowSelection(val links: List<PixivLinkTarget>) : PixivLinkSearchAction
}
