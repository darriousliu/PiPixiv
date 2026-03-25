package com.mrl.pixiv.setting.block

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.router.Destination
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.block_comments
import com.mrl.pixiv.strings.block_illust
import com.mrl.pixiv.strings.block_novel
import com.mrl.pixiv.strings.block_settings
import com.mrl.pixiv.strings.block_tags
import com.mrl.pixiv.strings.block_user
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val KEY_BLOCK_ILLUST = "block_illust"
private const val KEY_BLOCK_NOVEL = "block_novel"
private const val KEY_BLOCK_USER = "block_user"
private const val KEY_BLOCK_TAG = "block_tag"
private const val KEY_BLOCK_COMMENTS = "block_comments"

@Composable
fun BlockSettingsScreen(
    modifier: Modifier = Modifier,
) {
    val navigationManager = koinInject<NavigationManager>()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.block_settings))
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
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item(key = KEY_BLOCK_ILLUST) {
                BlockEntry(
                    title = RStrings.block_illust,
                    onClick = { navigationManager.navigate(Destination.BlockIllust) }
                )
            }
            item(key = KEY_BLOCK_NOVEL) {
                BlockEntry(
                    title = RStrings.block_novel,
                    onClick = { navigationManager.navigate(Destination.BlockNovel) }
                )
            }
            item(key = KEY_BLOCK_USER) {
                BlockEntry(
                    title = RStrings.block_user,
                    onClick = { navigationManager.navigate(Destination.BlockUser) }
                )
            }
            item(key = KEY_BLOCK_TAG) {
                BlockEntry(
                    title = RStrings.block_tags,
                    onClick = { navigationManager.navigate(Destination.BlockTag) }
                )
            }
            item(key = KEY_BLOCK_COMMENTS) {
                BlockEntry(
                    title = RStrings.block_comments,
                    onClick = { navigationManager.navigate(Destination.BlockComments) }
                )
            }
        }
    }
}

@Composable
private fun BlockEntry(
    title: StringResource,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .throttleClick(indication = ripple(), onClick = onClick),
    )
}
