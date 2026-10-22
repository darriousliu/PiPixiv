package com.mrl.pixiv.profile.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.novel_collection
import org.jetbrains.compose.resources.stringResource

@Composable
fun NovelBookmarkWidget(
    novels: List<Novel>,
    onAllClick: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileNovelPreview(
        title = stringResource(RStrings.novel_collection),
        novels = novels,
        showAuthor = true,
        onAllClick = onAllClick,
        onNovelClick = onNovelClick,
        onSeriesClick = onSeriesClick,
        modifier = modifier,
    )
}
