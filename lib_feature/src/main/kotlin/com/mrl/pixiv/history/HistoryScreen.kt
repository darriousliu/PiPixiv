package com.mrl.pixiv.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.filter
import com.mrl.pixiv.common.compose.transparentIndicatorColors
import com.mrl.pixiv.common.compose.ui.illust.illustGrid
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.asState
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    var searchValue by remember { mutableStateOf(TextFieldValue(state.currentSearch)) }
    val illusts = viewModel.illusts.map {
        it.filter {
            it.title.contains(searchValue.text, ignoreCase = true) ||
                    it.user.name.contains(searchValue.text, ignoreCase = true)
        }
    }.collectAsLazyPagingItems()
    Scaffold(
        modifier = modifier,
        topBar = {
            HistoryAppBar(
                searchValue = searchValue,
                onValueChange = {
                    searchValue = it
                    viewModel.dispatch(HistoryAction.UpdateSearch(it.text))
                },
                onBack = { navigationManager.popBackStack() }
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) {
        val lazyGridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = lazyGridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 20.dp),
        ) {
            illustGrid(
                illusts = illusts,
                navToPictureScreen = navigationManager::navigateToPictureScreen,
                enableLoading = true
            )
        }
    }
}

@Composable
private fun HistoryAppBar(
    searchValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onBack: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    TopAppBar(
        title = {
            TextField(
                value = searchValue,
                onValueChange = onValueChange,
                colors = transparentIndicatorColors.copy(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                placeholder = {
                    Text(text = stringResource(RString.search_by_title_author))
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { onValueChange(TextFieldValue()) }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear"
                        )
                    }
                },
                singleLine = true,
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}