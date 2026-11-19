package com.mrl.pixiv.setting.network.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.Constants.API_HOST
import com.mrl.pixiv.common.network.normalizeAppApiProxyHost
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.SettingDropdownRow
import com.mrl.pixiv.strings.app_api_proxy_address
import com.mrl.pixiv.strings.app_api_proxy_credentials
import com.mrl.pixiv.strings.app_api_proxy_custom
import com.mrl.pixiv.strings.app_api_proxy_invalid
import com.mrl.pixiv.strings.app_api_proxy_title
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.label_default
import com.mrl.pixiv.strings.save
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppApiProxySetting(
    proxyHost: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customHost = proxyHost.takeUnless { it.isBlank() || it == API_HOST }
    var expanded by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val official = appApiProxyHostLabel("")
    SettingDropdownRow(
        title = stringResource(RStrings.app_api_proxy_title),
        icon = Icons.Rounded.Link,
        current = customHost ?: API_HOST,
        expanded = expanded,
        onClick = { expanded = !expanded },
        onDismissRequest = { expanded = false },
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text(official) },
            onClick = {
                onSave("")
                expanded = false
            },
            trailingIcon = if (customHost == null) {
                { Icon(Icons.Rounded.Check, contentDescription = null) }
            } else null,
        )
        DropdownMenuItem(
            text = { Text(stringResource(RStrings.app_api_proxy_custom)) },
            onClick = {
                expanded = false
                editing = true
            },
            trailingIcon = if (customHost != null) {
                { Icon(Icons.Rounded.Check, contentDescription = null) }
            } else null,
        )
    }
    if (editing) {
        AppApiProxyEditDialog(
            proxyHost = customHost.orEmpty(),
            onSave = {
                onSave(if (it == API_HOST) "" else it)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
internal fun appApiProxyHostLabel(host: String): String =
    if (host.isBlank() || host == API_HOST) {
        "${stringResource(RStrings.label_default)} · $API_HOST"
    } else {
        host
    }

@Composable
internal fun AppApiProxyEditDialog(
    proxyHost: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable(proxyHost) { mutableStateOf(proxyHost) }
    val normalizedHost = remember(draft) { normalizeAppApiProxyHost(draft) }
    val valid = normalizedHost != null
    val invalidDraft = draft.isNotBlank() && !valid
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RStrings.app_api_proxy_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(RStrings.app_api_proxy_credentials),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(RStrings.app_api_proxy_address)) },
                    placeholder = { Text("proxy.example.com") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = invalidDraft,
                    supportingText = if (invalidDraft) {
                        { Text(stringResource(RStrings.app_api_proxy_invalid)) }
                    } else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { normalizedHost?.let(onSave) }, enabled = valid) {
                Text(stringResource(RStrings.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(RStrings.cancel))
            }
        },
    )
}
