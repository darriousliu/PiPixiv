package com.mrl.pixiv.setting.network.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.confirm
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isValid: (String) -> Boolean = { true },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                isError = !isValid(text),
                keyboardOptions = keyboardOptions
            )
        },
        confirmButton = {
            TextButton(onClick = { if (isValid(text)) onConfirm(text) }) {
                Text(stringResource(RStrings.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(RStrings.cancel))
            }
        }
    )
}