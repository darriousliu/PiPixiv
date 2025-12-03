package com.mrl.pixiv.search.result.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.search.SearchState.SearchFilter
import kotlinx.coroutines.launch

@Composable
internal fun FilterBottomSheet(
    bottomSheetState: SheetState,
    searchFilter: SearchFilter,
    onDismissRequest: () -> Unit,
    onUpdateFilter: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var innerSearchFilter by remember { mutableStateOf(searchFilter) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val searchTargetMap = remember {
            mapOf(
                SearchTarget.PARTIAL_MATCH_FOR_TAGS to RString.tags_partially_match,
                SearchTarget.EXACT_MATCH_FOR_TAGS to RString.tags_exact_match,
                SearchTarget.TITLE_AND_CAPTION to RString.title_and_description,
            )
        }
        val searchSortMap = remember {
            mapOf(
                SearchSort.DATE_DESC to RString.date_desc,
                SearchSort.DATE_ASC to RString.date_asc,
                SearchSort.POPULAR_DESC to RString.popular_desc,
                SearchSort.POPULAR_MALE_DESC to RString.popular_male,
                SearchSort.POPULAR_FEMALE_DESC to RString.popular_female,
            )
        }

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
                    scope.launch { bottomSheetState.hide() }
                        .invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                onDismissRequest()
                            }
                        }
                    onUpdateFilter(innerSearchFilter)
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            searchTargetMap.forEach { (key, value) ->
                FilterItem(
                    text = stringResource(value),
                    selected = innerSearchFilter.searchTarget == key,
                    onClick = {
                        innerSearchFilter = innerSearchFilter.copy(searchTarget = key)
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(4.dp))

            searchSortMap.forEach { (key, value) ->
                FilterItem(
                    text = stringResource(value),
                    selected = innerSearchFilter.sort == key,
                    onClick = {
                        innerSearchFilter = innerSearchFilter.copy(sort = key)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .throttleClick(
                        indication = ripple()
                    ) {
                        innerSearchFilter = innerSearchFilter.copy(
                            searchAiType = if (innerSearchFilter.searchAiType == SearchAiType.SHOW_AI) {
                                SearchAiType.HIDE_AI
                            } else {
                                SearchAiType.SHOW_AI
                            }
                        )
                    }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(RString.ai_generate),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = innerSearchFilter.searchAiType == SearchAiType.SHOW_AI,
                    onCheckedChange = { checked ->
                        innerSearchFilter = innerSearchFilter.copy(
                            searchAiType = if (checked) SearchAiType.SHOW_AI else SearchAiType.HIDE_AI
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
private fun FilterItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
