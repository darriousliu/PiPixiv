package com.mrl.pixiv.search.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.search.preview.components.TrendingItem
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SearchPreviewScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchPreviewViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val textState by remember { mutableStateOf(TextFieldValue()) }
    val lazyGridState = viewModel.lazyGridState
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextField(
                        value = textState,
                        onValueChange = {},
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .throttleClick {
                                navigationManager.navigateToSearchScreen()
                            },
                        placeholder = { Text(stringResource(RString.enter_keywords)) },
                        colors = TextFieldDefaults.colors(
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        }
    ) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.dispatch(SearchPreviewAction.LoadTrendingTags) },
            modifier = Modifier.padding(it),
        ) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = lazyGridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(
                    span = { GridItemSpan(3) },
                ) {
                    Text(
                        text = stringResource(RString.popular_tags),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(state.trendingTags) { tag ->
                    TrendingItem(
                        trendingTag = tag,
                        onSearch = {
                            navigationManager.navigateToSearchResultScreen(it)
                            viewModel.dispatch(SearchPreviewAction.AddSearchHistory(it))
                        }
                    )
                }
            }
        }
    }
}