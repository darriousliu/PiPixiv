package com.mrl.pixiv.comment.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.ui.image.LoadingImage
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.kts.HSpacer
import com.mrl.pixiv.common.kts.VSpacer
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.isSelf
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.throttleClick

@Composable
fun CommentItem(
    comment: Comment,
    onReplyComment: () -> Unit,
    onBlockComment: () -> Unit,
    onReportComment: () -> Unit,
    onNavToUserProfile: () -> Unit,
    onDeleteComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(modifier = modifier) {
        UserAvatar(
            url = comment.user.profileImageUrls.medium,
            modifier = Modifier.size(70.dp),
            onClick = { onNavToUserProfile() }
        )
        10.HSpacer
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = 4.spaceBy,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.user.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                if (comment.user.id.isSelf) {
                    Text(
                        text = stringResource(RString.delete),
                        modifier = Modifier
                            .throttleClick(indication = ripple()) {
                                showDeleteConfirm = true
                            }
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(RString.reply),
                    modifier = Modifier
                        .throttleClick(indication = ripple()) {
                            onReplyComment()
                        }
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Box {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = null,
                        modifier = Modifier
                            .throttleClick(indication = ripple(radius = 16.dp)) {
                                showMenu = true
                            }
                            .padding(4.dp)
                            .size(24.dp)
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(RString.block_comment)
                                )
                            },
                            onClick = {
                                onBlockComment()
                                showMenu = false
                            }
                        )
                        // 根据官方APP逻辑，纯stamp评论无法举报
                        if (comment.stamp == null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(RString.report_comment)
                                    )
                                },
                                onClick = {
                                    onReportComment()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
            if (comment.stamp != null) {
                LoadingImage(
                    model = comment.stamp!!.stampUrl,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
            } else {
                Text(
                    text = comment.comment,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            8.VSpacer
            Text(
                text = comment.dateString,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteComment()
                        showDeleteConfirm = false
                    }
                ) {
                    Text(text = stringResource(RString.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text(text = stringResource(RString.cancel))
                }
            },
            title = {
                Text(text = stringResource(RString.confirm_to_delete_comment))
            }
        )
    }
}