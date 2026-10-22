package com.mrl.pixiv.profile.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.novels
import org.jetbrains.compose.resources.stringResource

internal fun shouldShowNovelWorks(novels: Collection<*>): Boolean = novels.isNotEmpty()

@Composable
fun NovelWorksWidget(
    novels: List<Novel>,
    onAllClick: () -> Unit,
    onNovelClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileNovelPreview(
        title = stringResource(RStrings.novels),
        novels = novels,
        showAuthor = false,
        onAllClick = onAllClick,
        onNovelClick = onNovelClick,
        onSeriesClick = onSeriesClick,
        modifier = modifier,
    )
}
