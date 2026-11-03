package com.mrl.pixiv.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mrl.pixiv.common.compose.ui.BookmarkIcon
import com.mrl.pixiv.common.compose.ui.IllustBottomBookmarkSheet
import com.mrl.pixiv.common.data.AiType
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isPrivateBookmark
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.bookmark
import com.mrl.pixiv.strings.bookmarked
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MangaChapter(
    illust: Illust,
    onClick: () -> Unit,
) {
    val bookmarked = illust.isBookmark
    val blocked = BlockingRepositoryV2.collectIllustBlockAsState(illust.id) ||
            BlockingRepositoryV2.collectUserBlockAsState(illust.user.id)
    var showBookmarkSheet by rememberSaveable { mutableStateOf(false) }
    val onBookmark: (Restrict, List<String>?, Boolean) -> Unit = { restrict, tags, edit ->
        if (edit || !bookmarked) BookmarkState.bookmarkIllust(illust.id, restrict, tags)
        else BookmarkState.deleteBookmarkIllust(illust.id)
    }
    MangaChapterCard(
        illust = illust,
        bookmarked = bookmarked,
        privateBookmark = illust.isPrivateBookmark,
        blocked = blocked,
        onClick = onClick,
        onBookmarkClick = {
            onBookmark(
                if (requireUserPreferenceValue.defaultPrivateBookmark) Restrict.PRIVATE else Restrict.PUBLIC,
                null,
                false
            )
        },
        onBookmarkLongClick = { showBookmarkSheet = true },
    )
    if (showBookmarkSheet) {
        IllustBottomBookmarkSheet(
            hideBottomSheet = { showBookmarkSheet = false },
            illust = illust,
            bottomSheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
            onBookmarkClick = onBookmark,
        )
    }
}

/** Stateless chapter tile used by the series screen. */
@Composable
internal fun MangaChapterCard(
    illust: Illust,
    bookmarked: Boolean,
    privateBookmark: Boolean = false,
    blocked: Boolean = false,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onBookmarkLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = illust.imageUrls.squareMedium,
                contentDescription = null,
                modifier = Modifier.matchParentSize()
                    .then(if (blocked) Modifier.blur(50.dp) else Modifier),
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (illust.illustAIType == AiType.AiGeneratedWorks) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            "AI",
                            Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (illust.pageCount > 1) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        contentColor = androidx.compose.ui.graphics.Color.White,
                    ) {
                        Text(
                            illust.pageCount.toString(),
                            Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                illust.title,
                modifier = Modifier.weight(1f).padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!blocked) {
                IconButton(onClick = onBookmarkClick, onLongClick = onBookmarkLongClick) {
                    BookmarkIcon(
                        isBookmarked = bookmarked,
                        isPrivate = privateBookmark,
                        contentDescription = stringResource(if (bookmarked) RStrings.bookmarked else RStrings.bookmark),
                    )
                }
            }
        }
    }
}
