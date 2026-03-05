package com.mrl.pixiv.collection.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.bookmark_tags
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkedTagsScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val tags by BookmarkedTagRepository.bookmarkedTags.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(RStrings.bookmark_tags)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigationManager.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(items = tags, key = { it.name }) { tag ->
                val state = rememberSwipeToDismissBoxState { it / 3 }
                SwipeToDismissBox(
                    state = state,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .align(
                                        when (state.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            SwipeToDismissBoxValue.Settled -> Alignment.Center
                                        }
                                    ),
                                tint = Color.White
                            )
                        }
                    },
                    onDismiss = {
                        BookmarkedTagRepository.removeTag(tag)
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(text = tag.name) },
                        supportingContent = if (tag.translatedName.isNotEmpty()) {
                            { Text(text = tag.translatedName) }
                        } else null,
                        modifier = Modifier
                            .animateItem()
                            .clickable {
                                navigationManager.navigateToSearchResultScreen(tag.name)
                            }
                    )
                }
            }
        }
    }
}
