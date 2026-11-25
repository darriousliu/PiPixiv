package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.block_tags
import com.mrl.pixiv.strings.bookmark_add_success
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.copy_to_clipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun TagItem(
    tag: Tag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var showCollectionDialog by rememberSaveable { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .throttleClick(
                indication = ripple(),
                onLongClick = { showCollectionDialog = true },
                onClick = onClick,
            )
            .heightIn(min = 32.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        TagText(tag)
    }
    if (showCollectionDialog) {
        val indication = LocalIndication.current
        val itemModifier = Modifier.padding(vertical = 8.dp)
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            confirmButton = {},
            title = {
                Text(text = tag.name)
            },
            text = {
                Column {
                    Text(
                        text = stringResource(RStrings.block_tags),
                        modifier = Modifier
                            .fillMaxWidth()
                            .throttleClick(indication = indication) {
                                BlockingRepositoryV2.blockTag(tag.name)
                                showCollectionDialog = false
                            }
                            .then(itemModifier)
                    )
                    Text(
                        text = stringResource(RStrings.collection),
                        modifier = Modifier
                            .fillMaxWidth()
                            .throttleClick(
                                indication = indication
                            ) {
                                BookmarkedTagRepository.addTag(tag)
                                ToastUtil.safeShortToast(RStrings.bookmark_add_success)
                                showCollectionDialog = false
                            }
                            .then(itemModifier)
                    )
                    Text(
                        text = stringResource(RStrings.copy_to_clipboard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .throttleClick(
                                indication = indication
                            ) {
                                coroutineScope.launch {
                                    copyToClipboard(tag.name)
                                    showCollectionDialog = false
                                }
                            }
                            .then(itemModifier)
                    )
                }
            }
        )
    }
}

@Composable
private fun TagText(tag: Tag) {
    Layout(
        content = {
            Text(
                text = "#${tag.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tag.translatedName.isNotBlank()) {
                Text(
                    text = tag.translatedName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) { measurables, constraints ->
        val translation = measurables.getOrNull(1)
        val gap = if (translation == null) 0 else 6.dp.roundToPx().coerceAtMost(constraints.maxWidth)
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth - gap else Constraints.Infinity
        // 空间不足时为两种名称各保留至多一半，较短名称用不完的空间交给另一种。
        val reserved = if (translation != null && constraints.hasBoundedWidth) {
            minOf(translation.maxIntrinsicWidth(constraints.maxHeight), available / 2)
        } else 0
        val name = measurables.first().measure(constraints.copy(minWidth = 0, maxWidth = available - reserved))
        val remaining = if (constraints.hasBoundedWidth) available - name.width else Constraints.Infinity
        val translated = translation?.measure(constraints.copy(minWidth = 0, maxWidth = remaining))
        val height = maxOf(name.height, translated?.height ?: 0)
        val width = (name.width + gap + (translated?.width ?: 0)).coerceAtLeast(constraints.minWidth)
        layout(width, height) {
            name.placeRelative(0, (height - name.height) / 2)
            translated?.placeRelative(name.width + gap, (height - translated.height) / 2)
        }
    }
}
