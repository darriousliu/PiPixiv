package com.mrl.pixiv.common.paged

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.repository.feed.PagedFeedState
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.next_page
import com.mrl.pixiv.strings.page_index
import com.mrl.pixiv.strings.previous_page
import com.mrl.pixiv.strings.retry
import org.jetbrains.compose.resources.stringResource

@Composable
fun PageControls(
    state: PagedFeedState<*>,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onLoadPage: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageText by remember(state.currentPage) {
        mutableStateOf(state.currentPage.toString())
    }
    val submitPage = {
        pageText.toIntOrNull()?.takeIf { it > 0 }?.let(onLoadPage)
        Unit
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = state.hasPreviousPage && !state.isLoading,
                onClick = onPreviousPage,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(RStrings.previous_page),
                    modifier = Modifier.size(20.dp),
                )
            }
            if (state.canJumpToPage) {
                PageNumberField(
                    value = pageText,
                    onValueChange = { value ->
                        pageText = value.filter { it.isDigit() }.take(5)
                    },
                    enabled = !state.isLoading,
                    onSubmit = submitPage,
                )
                IconButton(
                    enabled = !state.isLoading,
                    onClick = submitPage,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(RStrings.confirm),
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Text(
                    text = stringResource(RStrings.page_index, state.currentPage),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(
                enabled = state.hasNextPage && !state.isLoading,
                onClick = onNextPage,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(RStrings.next_page),
                    modifier = Modifier.size(20.dp),
                )
            }
            if (state.error != null) {
                IconButton(
                    enabled = !state.isLoading,
                    onClick = onRetry,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(RStrings.retry),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(64.dp)
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { onSubmit() },
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.width(44.dp),
            )
        }
    }
}
