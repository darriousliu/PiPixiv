package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrl.pixiv.common.compose.deepBlue
import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.kts.round
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BookmarkedTagRepository
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.strings.bookmark_add_success
import com.mrl.pixiv.strings.collection
import com.mrl.pixiv.strings.copy_to_clipboard
import org.jetbrains.compose.resources.stringResource

@Composable
fun TagItem(
    tag: Tag,
    onClick: () -> Unit,
) {
    var showCollectionDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                10f.round
            )
            .throttleClick(
                indication = ripple(),
                onLongClick = {
                    showCollectionDialog = true
                },
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 2.5.dp),
        horizontalArrangement = 5f.spaceBy,
    ) {
        Text(
            text = "#" + tag.name,
            modifier = Modifier,
            color = MaterialTheme.colorScheme.primary,
            style = TextStyle(fontSize = 13.sp, color = deepBlue),
        )
        Text(
            text = tag.translatedName,
            modifier = Modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontSize = 13.sp)
        )
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
                                copyToClipboard(tag.name)
                                showCollectionDialog = false
                            }
                            .then(itemModifier)
                    )
                }
            }
        )
    }
}