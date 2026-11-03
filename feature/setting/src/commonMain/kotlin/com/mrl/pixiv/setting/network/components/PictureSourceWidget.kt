package com.mrl.pixiv.setting.network.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.DropDownSelector
import com.mrl.pixiv.strings.image_source
import com.mrl.pixiv.strings.label_default
import com.mrl.pixiv.strings.self_defined_source
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun PictureSourceWidget(
    modifier: Modifier = Modifier,
    currentSelected: String = "",
    savePictureSourceHost: (String) -> Unit = {}
) {
    val sources = mapOf(
        IMAGE_HOST to "${stringResource(RStrings.label_default)}: $IMAGE_HOST",
        "i.pixiv.cat" to "i.pixiv.cat",
        "i.pixiv.re" to "i.pixiv.re"
    )
    val selectedHost = currentSelected.ifEmpty { IMAGE_HOST }
    val isCustomSource = selectedHost !in sources
    var expanded by remember { mutableStateOf(false) }
    var showCustomSourceDialog by remember { mutableStateOf(false) }

    if (showCustomSourceDialog) {
        EditDialog(
            title = stringResource(RStrings.self_defined_source),
            initialValue = if (isCustomSource) selectedHost else "",
            onConfirm = { host ->
                savePictureSourceHost(host.trim())
                showCustomSourceDialog = false
            },
            onDismiss = { showCustomSourceDialog = false },
            isValid = { it.isNotBlank() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
    }

    ListItem(
        headlineContent = {
            Text(text = stringResource(RStrings.image_source))
        },
        modifier = modifier,
        trailingContent = {
            DropDownSelector(
                onClick = { expanded = !expanded },
                expanded = expanded,
                onDismissRequest = { expanded = false },
                current = sources[selectedHost] ?: selectedHost,
            ) {
                sources.forEach { (host, label) ->
                    DropdownMenuItem(
                        text = { Text(text = label) },
                        trailingIcon = {
                            if (host == selectedHost) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            savePictureSourceHost(host)
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(RStrings.self_defined_source)) },
                    trailingIcon = {
                        if (isCustomSource) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        showCustomSourceDialog = true
                    }
                )
            }
        }
    )
}
