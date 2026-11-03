package com.mrl.pixiv.setting.block

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.block_comments
import com.mrl.pixiv.strings.block_illust
import com.mrl.pixiv.strings.block_novel
import com.mrl.pixiv.strings.block_settings
import com.mrl.pixiv.strings.block_tags
import com.mrl.pixiv.strings.block_user
import com.mrl.pixiv.strings.cloud_mute_back
import com.mrl.pixiv.strings.cloud_mute_title
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class BlockCategory(val title: StringResource) {
    Cloud(RStrings.cloud_mute_title),
    Illust(RStrings.block_illust),
    Novel(RStrings.block_novel),
    User(RStrings.block_user),
    Tag(RStrings.block_tags),
    Comment(RStrings.block_comments),
}

@Composable
fun BlockSettingsScreen(
    blockedCommentsContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationManager = currentNavigationManager()
    BlockSettingsContent(
        onCategoryChange = { navigationManager.closeCurrentDetailBranch() },
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    ) { category, contentModifier ->
        when (category) {
            BlockCategory.Cloud -> CloudMuteScreen(modifier = contentModifier, embedded = true)
            BlockCategory.Illust -> BlockIllustScreen(
                modifier = contentModifier,
                embedded = true
            )

            BlockCategory.Novel -> BlockNovelScreen(modifier = contentModifier, embedded = true)
            BlockCategory.User -> BlockUserScreen(modifier = contentModifier, embedded = true)
            BlockCategory.Tag -> BlockTagScreen(modifier = contentModifier, embedded = true)
            BlockCategory.Comment -> blockedCommentsContent(contentModifier)
        }
    }
}

@Composable
internal fun BlockSettingsContent(
    onCategoryChange: (BlockCategory) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialCategory: BlockCategory = BlockCategory.Cloud,
    categoryContent: @Composable (BlockCategory, Modifier) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialCategory.ordinal) {
        BlockCategory.entries.size
    }
    val scope = rememberCoroutineScope()
    val onSettledCategoryChange by rememberUpdatedState(onCategoryChange)
    LaunchedEffect(pagerState) {
        // Preserve a restored detail until the user actually changes the category.
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            onSettledCategoryChange(BlockCategory.entries[page])
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.block_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(RStrings.cloud_mute_back)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 8.dp) {
                BlockCategory.entries.forEach { tab ->
                    Tab(
                        selected = tab.ordinal == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                        text = { Text(stringResource(tab.title)) },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxSize(),
                key = { BlockCategory.entries[it].name },
            ) { page ->
                categoryContent(BlockCategory.entries[page], Modifier.fillMaxSize())
            }
        }
    }
}
