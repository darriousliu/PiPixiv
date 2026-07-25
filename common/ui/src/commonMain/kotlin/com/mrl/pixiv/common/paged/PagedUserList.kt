package com.mrl.pixiv.common.paged

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.ui.illust.SquareIllustItem
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.user.UserPreview
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.feed.PagedFeedState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.repository.viewmodel.follow.FollowState
import com.mrl.pixiv.common.repository.viewmodel.follow.isFollowing
import com.mrl.pixiv.common.router.NavigateToHorizontalPictureScreen
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.follow
import com.mrl.pixiv.strings.followed
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

private const val USER_PREVIEW_SIZE = 3

fun LazyListScope.PagedUserList(
    state: PagedFeedState<UserPreview>,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    navToUserProfile: (Long) -> Unit,
    showIllusts: Boolean = true,
) {
    items(
        count = state.items.size,
        key = { index -> "${index}_${state.items[index].user.id}" },
    ) { index ->
        val userPreview = state.items[index]
        PagedUserItem(
            userPreview = userPreview,
            navToPictureScreen = navToPictureScreen,
            navToUserProfile = { navToUserProfile(userPreview.user.id) },
            showIllusts = showIllusts,
        )
    }
}

@Composable
private fun PagedUserItem(
    userPreview: UserPreview,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    navToUserProfile: () -> Unit,
    modifier: Modifier = Modifier,
    showIllusts: Boolean = true,
) {
    val illusts = userPreview.illusts.toImmutableList()
    val user = userPreview.user

    Card(modifier = modifier) {
        if (showIllusts) {
            Row {
                val preview = illusts.take(USER_PREVIEW_SIZE)
                preview.forEachIndexed { index, illust ->
                    PreviewIllust(
                        illust = illust,
                        allIllusts = illusts,
                        index = index,
                        navToPictureScreen = navToPictureScreen,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (preview.size < USER_PREVIEW_SIZE) {
                    Spacer(modifier = Modifier.weight((USER_PREVIEW_SIZE - preview.size).toFloat()))
                }
            }
        }
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = 8f.spaceBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                url = user.profileImageUrls.medium,
                onClick = navToUserProfile,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = user.name,
                modifier = Modifier.weight(1f),
            )
            if (user.isFollowing) {
                OutlinedButton(
                    onClick = { FollowState.unFollowUser(user.id) },
                ) {
                    Text(text = stringResource(RStrings.followed))
                }
            } else {
                Button(
                    onClick = { FollowState.followUser(user.id) },
                ) {
                    Text(text = stringResource(RStrings.follow))
                }
            }
        }
    }
}

@Composable
private fun PreviewIllust(
    illust: Illust,
    allIllusts: List<Illust>,
    index: Int,
    navToPictureScreen: NavigateToHorizontalPictureScreen,
    modifier: Modifier = Modifier,
) {
    val isBookmarked = illust.isBookmark
    SquareIllustItem(
        illust = illust,
        isBookmarked = isBookmarked,
        onBookmarkClick = { restrict, tags, isEdit ->
            if (isEdit || !isBookmarked) {
                BookmarkState.bookmarkIllust(illust.id, restrict, tags)
            } else {
                BookmarkState.deleteBookmarkIllust(illust.id)
            }
        },
        navToPictureScreen = { prefix, enableTransition ->
            navToPictureScreen(allIllusts, index, prefix, enableTransition)
        },
        modifier = modifier,
        elevation = 0.dp,
        shape = RectangleShape,
    )
}
