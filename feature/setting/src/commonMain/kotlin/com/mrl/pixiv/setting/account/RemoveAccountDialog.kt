package com.mrl.pixiv.setting.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.account_removal_description
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.delete_account_local_data
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RemoveAccountDialog(
    title: String,
    confirmLabel: String,
    onDismissRequest: () -> Unit,
    onConfirm: (deleteLocalData: Boolean) -> Unit,
) {
    var deleteLocalData by rememberSaveable { mutableStateOf(false) }
    val cancelFocusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.key != Key.Escape) return@onPreviewKeyEvent false
            // 在桌面端的全局返回处理器收到 Escape 之前关闭弹窗。
            if (event.type == KeyEventType.KeyUp) onDismissRequest()
            true
        },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(RStrings.account_removal_description))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = deleteLocalData,
                            role = Role.Checkbox,
                            onValueChange = { deleteLocalData = it },
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = deleteLocalData, onCheckedChange = null)
                    Text(stringResource(RStrings.delete_account_local_data))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.focusRequester(cancelFocusRequester),
            ) {
                Text(stringResource(RStrings.cancel))
            }
            LaunchedEffect(Unit) { cancelFocusRequester.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(deleteLocalData) }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
    )
}
