package com.mrl.pixiv.search.result.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.search.SearchState.SearchFilter

@Composable
internal fun FilterBottomSheet(
    bottomSheetState: SheetState,
    searchFilter: SearchFilter,
    onRefresh: () -> Unit,
    onDismissRequest: () -> Unit,
    onUpdateFilter: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val filter = searchFilter
        val searchTargetMap = remember {
            mapOf(
                SearchTarget.PARTIAL_MATCH_FOR_TAGS to context.getString(RString.tags_partially_match),
                SearchTarget.EXACT_MATCH_FOR_TAGS to context.getString(RString.tags_exact_match),
                SearchTarget.TITLE_AND_CAPTION to context.getString(RString.title_and_description),
            )
        }
        val searchSortMap = remember {
            mapOf(
                SearchSort.DATE_DESC to context.getString(RString.date_desc),
                SearchSort.DATE_ASC to context.getString(RString.date_asc),
                SearchSort.POPULAR_DESC to context.getString(RString.popular_desc),
            )
        }
        var selectedTargetIndex by remember { mutableIntStateOf(searchTargetMap.keys.indexOf(filter.searchTarget)) }
        var selectedSortIndex by remember { mutableIntStateOf(searchSortMap.keys.indexOf(filter.sort)) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(RString.filter))
            Text(
                text = stringResource(RString.apply),
                modifier = Modifier.throttleClick {
                    onRefresh()
                }
            )
        }
        SelectedTabRow(selectedIndex = selectedTargetIndex) {
            searchTargetMap.forEach { (key, value) ->
                Tab(
                    modifier = Modifier.clip(MaterialTheme.shapes.medium),
                    selected = filter.searchTarget == key,
                    onClick = {
                        selectedTargetIndex = searchTargetMap.keys.indexOf(key)
                        onUpdateFilter(filter.copy(searchTarget = key))

                    }
                ) {
                    Text(
                        text = value,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SelectedTabRow(selectedIndex = selectedSortIndex) {
            searchSortMap.forEach { (key, value) ->
                Tab(
                    modifier = Modifier.clip(MaterialTheme.shapes.medium),
                    selected = filter.sort == key,
                    onClick = {
                        selectedSortIndex = searchSortMap.keys.indexOf(key)
                        onUpdateFilter(filter.copy(sort = key))
                    }
                ) {
                    Text(
                        text = value,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
private fun SelectedTabRow(
    selectedIndex: Int,
    tabs: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = {
                Surface(
                    modifier = Modifier
                        .tabIndicatorOffset(selectedIndex)
                        .fillMaxHeight(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ) {}
            },
            divider = {}
        ) {
            tabs()
        }
    }
}