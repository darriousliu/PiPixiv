package com.mrl.pixiv.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mrl.pixiv.collection.RestrictBookmarkTag
import com.mrl.pixiv.common.compose.lightBlue
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.bookmark_tags
import com.mrl.pixiv.strings.word_private
import com.mrl.pixiv.strings.word_public
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun FilterDialog(
    onDismissRequest: () -> Unit,
    userBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag>,
    privateBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag>,
    restrict: Restrict,
    filterTag: String?,
    onLoadUserBookmarksTags: (Restrict) -> Unit,
    onSelected: (restrict: Restrict, tag: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember(restrict) { mutableIntStateOf(if (restrict == Restrict.PUBLIC) 0 else 1) }
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 2 })
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.small)
                ) {
                    LaunchedEffect(pagerState.currentPage) {
                        selectedTab = pagerState.currentPage
                    }
                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small),
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        indicator = {
                            Surface(
                                modifier = Modifier
                                    .tabIndicatorOffset(selectedTab)
                                    .fillMaxHeight(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            ) {}
                        },
                        divider = {},
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                        ) {
                            Text(
                                text = stringResource(RStrings.word_public),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                        ) {
                            Text(
                                text = stringResource(RStrings.word_private),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(RStrings.bookmark_tags),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .padding(vertical = 8.dp)
                        .padding(start = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                HorizontalPager(state = pagerState) { currentPage ->
                    LaunchedEffect(Unit) {
                        onLoadUserBookmarksTags(if (currentPage == 0) Restrict.PUBLIC else Restrict.PRIVATE)
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(300.dp)
                    ) {
                        items(
                            if (currentPage == 0) userBookmarkTagsIllust else privateBookmarkTagsIllust,
                            key = { it.name.toString() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .throttleClick(indication = ripple()) {
                                        onSelected(
                                            if (selectedTab == 0) Restrict.PUBLIC else Restrict.PRIVATE,
                                            it.name
                                        )
                                        onDismissRequest()
                                    }
                                    .conditionally(((restrict == Restrict.PUBLIC && it.isPublic) || (restrict == Restrict.PRIVATE && !it.isPublic)) && filterTag == it.name) {
                                        Modifier.background(lightBlue, MaterialTheme.shapes.small)
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = it.displayName)
                                if (it.count != null) {
                                    Text(text = it.count.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
