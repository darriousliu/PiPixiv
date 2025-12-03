package com.mrl.pixiv.search.result.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.util.throttleClick

@Composable
internal fun SearchResultAppBar(
    searchWords: String,
    popBack: () -> Unit,
    showBottomSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .throttleClick { popBack() },
                text = searchWords,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(
                onClick = popBack,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            //筛选按钮
            IconButton(
                onClick = showBottomSheet,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterAlt,
                    contentDescription = "Back"
                )
            }
        }
    )
}