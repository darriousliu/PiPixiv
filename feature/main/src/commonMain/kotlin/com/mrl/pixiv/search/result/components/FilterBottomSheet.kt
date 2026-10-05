package com.mrl.pixiv.search.result.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.mrl.pixiv.search.result.SearchOptionsState
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
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.search.SearchAiType
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.compose.ui.SearchContentFilterControls
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.data.search.SearchSort
import com.mrl.pixiv.common.data.search.SearchTarget
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.search.SearchState.SearchFilter
import com.mrl.pixiv.strings.ai_generate
import com.mrl.pixiv.strings.apply
import com.mrl.pixiv.strings.date_asc
import com.mrl.pixiv.strings.date_desc
import com.mrl.pixiv.strings.filter
import com.mrl.pixiv.strings.popular_desc
import com.mrl.pixiv.strings.popular_female
import com.mrl.pixiv.strings.popular_male
import com.mrl.pixiv.strings.tags_exact_match
import com.mrl.pixiv.strings.tags_partially_match
import com.mrl.pixiv.strings.title_and_description
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FilterBottomSheet(
    bottomSheetState: SheetState,
    searchFilter: SearchFilter,
    onDismissRequest: () -> Unit,
    onUpdateFilter: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
    isNovelMode: Boolean = false,
    options: SearchOptionsState = SearchOptionsState(),
    onLoadOptions: (SearchTarget, SearchAiType) -> Unit = { _, _ -> },
) {
    var innerSearchFilter by remember { mutableStateOf(searchFilter) }
    var rangesValid by remember { mutableStateOf(true) }
    LaunchedEffect(innerSearchFilter.searchTarget, innerSearchFilter.searchAiType) {
        onLoadOptions(innerSearchFilter.searchTarget, innerSearchFilter.searchAiType)
    }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val searchTargetMap = remember(isNovelMode) {
            if (isNovelMode) {
                mapOf(
                    SearchTarget.PARTIAL_MATCH_FOR_TAGS to RStrings.tags_partially_match,
                    SearchTarget.EXACT_MATCH_FOR_TAGS to RStrings.tags_exact_match,
                    SearchTarget.KEYWORD to RStrings.title_and_description,
                )
            } else {
                mapOf(
                    SearchTarget.PARTIAL_MATCH_FOR_TAGS to RStrings.tags_partially_match,
                    SearchTarget.EXACT_MATCH_FOR_TAGS to RStrings.tags_exact_match,
                    SearchTarget.TITLE_AND_CAPTION to RStrings.title_and_description,
                )
            }
        }
        val searchSortMap = remember(isNovelMode) {
            if (isNovelMode) {
                mapOf(
                    SearchSort.DATE_DESC to RStrings.date_desc,
                    SearchSort.DATE_ASC to RStrings.date_asc,
                    SearchSort.POPULAR_DESC to RStrings.popular_desc,
                )
            } else {
                mapOf(
                    SearchSort.DATE_DESC to RStrings.date_desc,
                    SearchSort.DATE_ASC to RStrings.date_asc,
                    SearchSort.POPULAR_DESC to RStrings.popular_desc,
                    SearchSort.POPULAR_MALE_DESC to RStrings.popular_male,
                    SearchSort.POPULAR_FEMALE_DESC to RStrings.popular_female,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(RStrings.filter))
            TextButton(enabled = rangesValid, onClick = {
                onUpdateFilter(innerSearchFilter)
                scope.launch { bottomSheetState.hide(); onDismissRequest() }
            }) {
                Text(stringResource(RStrings.apply))
            }
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
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
            SearchAdvancedControls(
                filter = innerSearchFilter,
                isNovel = isNovelMode,
                options = options,
                onChange = { innerSearchFilter = it },
                onValidityChanged = { rangesValid = it },
                onRetry = { onLoadOptions(innerSearchFilter.searchTarget, innerSearchFilter.searchAiType) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SearchContentFilterControls(
                filter = innerSearchFilter.contentFilter,
                defaultShowR18 = requireUserPreferenceValue.isR18Enabled,
                onChange = { innerSearchFilter = innerSearchFilter.copy(contentFilter = it) },
                modifier = Modifier.padding(horizontal = 32.dp),
                mode = if (isNovelMode) AppViewMode.NOVEL else AppViewMode.ILLUST,
            )

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
                    text = stringResource(RStrings.ai_generate),
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
