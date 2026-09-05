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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.SettingRepository.collectAsStateWithLifecycle
import com.mrl.pixiv.common.router.DestinationsDeepLink
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.PixivLinkTarget
import com.mrl.pixiv.common.util.DebounceUtil
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.readTextFromClipboard
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.main.components.ViewModeToggleButton
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
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
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
    var textState by remember { mutableStateOf(TextFieldValue(viewModel.searchWords)) }
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
    LifecycleResumeEffect(readClipboardOnSearch) {
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
            textState = textState.copy(selection = TextRange(textState.text.length))
        }
        onPauseOrDispose { }
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
                textState = textState,
                focusRequester = focusRequester,
                onValueChange = {
                    textState = it
                    dispatch(SearchAction.UpdateSearchWords(it.text))
                    if (it.text.isNotBlank()) {
                        DebounceUtil.debounce {
                            dispatch(SearchAction.SearchAutoComplete(it.text))
                        }
                    } else {
                        dispatch(SearchAction.ClearAutoCompleteSearchWords)
                    }
                },
                onBack = { navigationManager.popBackStack() },
                onSearch = search@{
                    if (handlePixivLinks(textState.text)) {
                        focusRequester.freeFocus()
                        return@search
                    }
                    if (state.isIdSearch) {
                        viewModel.addSearchIdHistory(textState.text)
                    } else {
                        dispatch(SearchAction.AddSearchHistory(textState.text))
                    }
                    focusRequester.freeFocus()
                    navigationManager.navigateToSearchResultScreen(
                        searchWord = textState.text,
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
                        text = if (textState.text.isEmpty())
                            stringResource(RStrings.search_history)
                        else
                            stringResource(RStrings.find_for),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                }
            }
            if (textState.text.isEmpty()) {
                if (state.isIdSearch) {
                    items(
                        items = searchIdHistory,
                        key = { it }
                    ) {
                        ListItem(
                            headlineContent = { Text(text = it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .throttleClick(indication = ripple()) {
                                    viewModel.addSearchIdHistory(it)
                                    focusRequester.freeFocus()
                                    navigationManager.navigateToSearchResultScreen(
                                        searchWord = it,
                                        isIdSearch = true,
                                        searchMode = appViewMode
                                    )
                                }
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
                            }
                        )
                    }
                } else {
                    items(
                        items = searchHistory,
                        key = { it.keyword }
                    ) { item ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = item.keyword,
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                                .throttleClick(indication = ripple()) {
                                    dispatch(SearchAction.AddSearchHistory(item.keyword))
                                    focusRequester.freeFocus()
                                    navigationManager.navigateToSearchResultScreen(
                                        searchWord = item.keyword,
                                        isIdSearch = false,
                                        searchMode = appViewMode
                                    )
                                }
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
                            }
                        )
                    }
                }
            } else {
                items(
                    items = state.autoCompleteSearchWords,
                    key = { it.name }
                ) { word ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = word.name,
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                            .throttleClick(indication = ripple()) {
                                dispatch(SearchAction.AddSearchHistory(word.name))
                                focusRequester.freeFocus()
                                navigationManager.navigateToSearchResultScreen(
                                    searchWord = word.name,
                                    isIdSearch = state.isIdSearch,
                                    searchMode = appViewMode
                                )
                            },
                        supportingContent = {
                            if (word.translatedName.isNotBlank()) {
                                Text(
                                    text = word.translatedName,
//                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
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
                        headlineContent = { Text(text = "$type #${link.id}") },
                        supportingContent = { Text(text = link.url) },
                        modifier = Modifier.throttleClick(indication = ripple()) {
                            onSelect(link)
                        },
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
    textState: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
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
                        value = textState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .throttleClick {
                                focusRequester.requestFocus()
                            },
                        onValueChange = onValueChange,
                        placeholder = { Text(stringResource(RStrings.enter_keywords)) },
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { onSearch() }
                        ),
                        trailingIcon = if (shouldShowSearchInputClearIcon(textState.text)) {
                            {
                                IconButton(
                                    onClick = { onValueChange(TextFieldValue()) },
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
