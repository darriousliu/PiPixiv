package com.mrl.pixiv.setting.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.BlockingGridDefaults
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.viewmodel.asState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val KEY_TITLE_MUTE_USERS = "title_mute_users"
private const val KEY_TITLE_MUTE_TAGS = "title_mute_tags"
private const val KEY_DIVIDER = "divider"

@Composable
fun BlockSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: BlockSettingsViewModel = koinViewModel(),
) {
    val navigationManager = koinInject<NavigationManager>()
    val state = viewModel.asState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(RString.block_settings),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.editMuteList {
                                navigationManager.popBackStack()
                            }
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        val lazyGridState = rememberLazyGridState()
        val layoutParams = BlockingGridDefaults.blockingLayoutParameters()
        val userEmpty = state.allMutedUsers.isEmpty()
        val tagEmpty = state.allMutedTags.isEmpty()

        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                state = lazyGridState,
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                columns = layoutParams.gridCells,
                verticalArrangement = layoutParams.verticalArrangement,
                horizontalArrangement = layoutParams.horizontalArrangement,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            ) {
                if (!userEmpty) {
                    item(
                        key = KEY_TITLE_MUTE_USERS,
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Text(
                            text = stringResource(RString.block_user),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(
                        items = state.allMutedUsers,
                        key = { it.user.id }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    url = it.user.profileImageUrls.medium,
                                    modifier = Modifier.size(40.dp),
                                )
                                Text(
                                    text = it.user.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            Switch(
                                checked = it.user.id !in state.toEditBlockUser,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        viewModel.removeMutedUser(it.user.id)
                                    } else {
                                        viewModel.addMutedUser(it.user.id)
                                    }
                                }
                            )
                        }
                    }
                    item(
                        key = KEY_DIVIDER,
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        HorizontalDivider()
                    }
                }
                if (!tagEmpty) {
                    item(
                        key = KEY_TITLE_MUTE_TAGS,
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Text(
                            text = stringResource(RString.block_tags),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(
                        items = state.allMutedTags,
                        key = { it.tag.name }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = it.tag.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Switch(
                                checked = it.tag.name !in state.toEditBlockTag,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        viewModel.removeMutedTag(it.tag.name)
                                    } else {
                                        viewModel.addMutedTag(it.tag.name)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}