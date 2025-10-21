package com.mrl.pixiv.profile.detail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrl.pixiv.common.compose.ui.illust.SquareIllustItem
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.bookmark.isBookmark

private const val SPAN_COUNT = 3
private const val MAX_SHOW_ILLUST_COUNT = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IllustWidget(
    title: String,
    endText: String,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    illusts: List<Illust>,
    modifier: Modifier = Modifier,
    onAllClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
    ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .throttleClick {
                        onAllClick()
                    }
            ) {
                Text(
                    text = endText,
                    fontSize = 12.sp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(12.dp),
                    tint = Color.Blue
                )
            }

        }
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = 5f.spaceBy,
            verticalArrangement = 5f.spaceBy,
            maxItemsInEachRow = SPAN_COUNT,
        ) {
            val takenIllusts = illusts.take(MAX_SHOW_ILLUST_COUNT)
            takenIllusts.forEachIndexed { index, illust ->
                val isBookmarked = illust.isBookmark
                SquareIllustItem(
                    illust = illust,
                    isBookmarked = isBookmarked,
                    onBookmarkClick = { restrict, tags ->
                        if (isBookmarked) {
                            BookmarkState.deleteBookmarkIllust(illust.id)
                        } else {
                            BookmarkState.bookmarkIllust(illust.id, restrict, tags)
                        }
                    },
                    navToPictureScreen = { prefix, enableTransition ->
                        navToPictureScreen(takenIllusts, index, prefix, enableTransition)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
