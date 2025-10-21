package com.mrl.pixiv.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mrl.pixiv.collection.RestrictBookmarkTag
import com.mrl.pixiv.common.compose.lightBlue
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.throttleClick
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun FilterDialog(
    onDismissRequest: () -> Unit,
    userBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag>,
    privateBookmarkTagsIllust: ImmutableList<RestrictBookmarkTag>,
    @Restrict restrict: String,
    filterTag: String?,
    onLoadUserBookmarksTags: (String) -> Unit,
    onSelected: (restrict: String, tag: String?) -> Unit,
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
                                text = stringResource(RString.word_public),
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
                                text = stringResource(RString.word_private),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(RString.bookmark_tags),
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
