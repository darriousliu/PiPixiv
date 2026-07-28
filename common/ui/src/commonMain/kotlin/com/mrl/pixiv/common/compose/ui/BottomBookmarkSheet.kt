package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.transparentIndicatorColors
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.illust.BookmarkDetailTag
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.add_tags
import com.mrl.pixiv.strings.add_to_favorite
import com.mrl.pixiv.strings.bookmark_tags
import com.mrl.pixiv.strings.cancel_favorite
import com.mrl.pixiv.strings.edit_favorite
import com.mrl.pixiv.strings.max_bookmark_tags_reached
import com.mrl.pixiv.strings.word_private
import com.mrl.pixiv.strings.word_public
import org.jetbrains.compose.resources.stringResource

private const val MAX_BOOKMARK_TAGS = 10

@Composable
fun IllustBottomBookmarkSheet(
    hideBottomSheet: () -> Unit,
    illust: Illust,
    bottomSheetState: SheetState,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
) {
    var selectedRestrict by remember { mutableStateOf(Restrict.PUBLIC) }
    val illustBookmarkDetailTags = remember { mutableStateListOf<BookmarkDetailTag>() }
    LaunchedEffect(Unit) {
        launchProcess {
            val resp = withIOContext { PixivRepository.getIllustBookmarkDetail(illust.id) }
            val restrict = Restrict.fromValue(resp.bookmarkDetail.restrict)
            selectedRestrict = restrict
            BookmarkState.updateIllustBookmarkDetail(
                illustId = illust.id,
                isBookmarked = resp.bookmarkDetail.isBookmarked,
                restrict = restrict,
            )
            illustBookmarkDetailTags.clear()
            illustBookmarkDetailTags.addAll(resp.bookmarkDetail.tags)
        }
    }
    BottomBookmarkSheet(
        selectedRestrict = selectedRestrict,
        onRestrictChange = { selectedRestrict = it },
        tags = illustBookmarkDetailTags,
        hideBottomSheet = hideBottomSheet,
        bottomSheetState = bottomSheetState,
        onBookmarkClick = onBookmarkClick,
        isBookmarked = illust.isBookmark
    )
}

@Composable
fun NovelBottomBookmarkSheet(
    hideBottomSheet: () -> Unit,
    novel: Novel,
    bottomSheetState: SheetState,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
) {
    var selectedRestrict by remember { mutableStateOf(Restrict.PUBLIC) }
    val novelBookmarkDetailTags = remember { mutableStateListOf<BookmarkDetailTag>() }
    LaunchedEffect(Unit) {
        launchProcess {
            val resp = withIOContext { PixivRepository.getNovelBookmarkDetail(novel.id) }
            val restrict = Restrict.fromValue(resp.bookmarkDetail.restrict)
            selectedRestrict = restrict
            BookmarkState.updateNovelBookmarkDetail(
                novelId = novel.id,
                isBookmarked = resp.bookmarkDetail.isBookmarked,
                restrict = restrict,
            )
            novelBookmarkDetailTags.clear()
            novelBookmarkDetailTags.addAll(resp.bookmarkDetail.tags)
        }
    }
    BottomBookmarkSheet(
        selectedRestrict = selectedRestrict,
        onRestrictChange = { selectedRestrict = it },
        tags = novelBookmarkDetailTags,
        hideBottomSheet = hideBottomSheet,
        bottomSheetState = bottomSheetState,
        onBookmarkClick = onBookmarkClick,
        isBookmarked = novel.isBookmark
    )
}

@Composable
private fun BottomBookmarkSheet(
    selectedRestrict: Restrict,
    onRestrictChange: (Restrict) -> Unit,
    tags: SnapshotStateList<BookmarkDetailTag>,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState,
    onBookmarkClick: (Restrict, List<String>?, Boolean) -> Unit,
    isBookmarked: Boolean,
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        modifier = Modifier.imePadding(),
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val allTags = remember(tags.size) {
            tags.map { it.name to it.isRegistered }.toMutableStateList()
        }
        val selectedTagsIndex = allTags.indices.filter { allTags[it].second }
        var inputTag by remember { mutableStateOf(TextFieldValue()) }

        Text(
            text = if (isBookmarked) stringResource(RStrings.edit_favorite) else stringResource(
                RStrings.add_to_favorite
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RestrictRadioItem(
                restrict = Restrict.PUBLIC,
                selectedRestrict = selectedRestrict,
                onRestrictChange = onRestrictChange,
                modifier = Modifier.weight(1f),
            )
            RestrictRadioItem(
                restrict = Restrict.PRIVATE,
                selectedRestrict = selectedRestrict,
                onRestrictChange = onRestrictChange,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
//                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(RStrings.bookmark_tags),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedTagsIndex.size} / $MAX_BOOKMARK_TAGS",
                    style = MaterialTheme.typography.labelMedium
                )
                Checkbox(
                    checked = allTags.count { it.second }.let {
                        it != 0 && (it == MAX_BOOKMARK_TAGS || it == allTags.size)
                    },
                    onCheckedChange = { checked ->
                        if (checked) {
                            (0..<minOf(allTags.size, MAX_BOOKMARK_TAGS)).forEach { index ->
                                allTags[index] = allTags[index].first to true
                            }
                        } else {
                            allTags.indices.forEach { index ->
                                allTags[index] = allTags[index].first to false
                            }
                        }
                    }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputTag,
                onValueChange = { inputTag = it },
                modifier = Modifier.weight(1f),
                enabled = selectedTagsIndex.size < MAX_BOOKMARK_TAGS,
                placeholder = { Text(text = stringResource(RStrings.add_tags)) },
                shape = MaterialTheme.shapes.small,
                colors = transparentIndicatorColors
            )
            IconButton(
                onClick = throttleClick {
                    handleInputTag(inputTag, allTags)
                    inputTag = inputTag.copy(text = "")
                },
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            }
        }
        LazyColumn(
            modifier = Modifier
                .height(LocalWindowInfo.current.containerDpSize.height / 3)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            itemsIndexed(
                items = allTags,
                key = { index, item -> "${index}_${item.first}" }
            ) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .throttleClick(indication = ripple()) {
                            if (item.second) {
                                allTags[index] = item.first to false
                            } else {
                                if (selectedTagsIndex.size < MAX_BOOKMARK_TAGS) {
                                    allTags[index] = item.first to true
                                } else {
                                    ToastUtil.safeShortToast(
                                        RStrings.max_bookmark_tags_reached,
                                        MAX_BOOKMARK_TAGS
                                    )
                                }
                            }
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.first,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Checkbox(
                        checked = item.second,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (selectedTagsIndex.size < MAX_BOOKMARK_TAGS) {
                                    allTags[index] = item.first to true
                                } else {
                                    ToastUtil.safeShortToast(
                                        RStrings.max_bookmark_tags_reached,
                                        MAX_BOOKMARK_TAGS
                                    )
                                }
                            } else {
                                allTags[index] = item.first to false
                            }
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(8.dp),
            horizontalArrangement = 8f.spaceBy,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = throttleClick {
                    onBookmarkClick(
                        selectedRestrict,
                        selectedTagsIndex.map { allTags[it].first },
                        isBookmarked
                    )
                    hideBottomSheet()
                },
                modifier = Modifier
            ) {
                BookmarkIcon(
                    isBookmarked = isBookmarked,
                    isPrivate = selectedRestrict == Restrict.PRIVATE,
                    iconSize = 24.dp,
                )
                8f.HSpacer
                Text(
                    text = stringResource(if (isBookmarked) RStrings.edit_favorite else RStrings.add_to_favorite),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (isBookmarked) {
                OutlinedButton(
                    onClick = throttleClick {
                        onBookmarkClick(Restrict.PUBLIC, null, false)
                        hideBottomSheet()
                    },
                ) {
                    Text(
                        text = stringResource(RStrings.cancel_favorite),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun RestrictRadioItem(
    restrict: Restrict,
    selectedRestrict: Restrict,
    onRestrictChange: (Restrict) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectable(
                selected = selectedRestrict == restrict,
                role = Role.RadioButton,
            ) {
                onRestrictChange(restrict)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selectedRestrict == restrict,
            onClick = null,
        )
        8f.HSpacer
        Text(
            text = stringResource(
                if (restrict == Restrict.PUBLIC) RStrings.word_public else RStrings.word_private
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun handleInputTag(
    inputTag: TextFieldValue,
    allTags: SnapshotStateList<Pair<String, Boolean>>,
) {
    val tagText = inputTag.text.trim()
    if (tagText.isNotEmpty()) {
        // 检查是否已存在于illustBookmarkDetailTags中
        val existingTagIndex = allTags.indexOfFirst { it.first == tagText }
        if (existingTagIndex != -1) {
            // 如果存在，移动到首位
            val existingTag = allTags[existingTagIndex]
            allTags.remove(existingTag)
            allTags.add(0, existingTag)
        } else {
            // 如果不存在，添加到首位
            allTags.add(0, tagText to true)
        }
    }
}
