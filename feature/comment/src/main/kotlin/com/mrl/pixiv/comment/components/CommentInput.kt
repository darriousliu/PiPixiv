package com.mrl.pixiv.comment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mrl.pixiv.comment.MAX_COMMENT_LENGTH
import com.mrl.pixiv.common.compose.ui.image.LoadingImage
import com.mrl.pixiv.common.data.comment.Emoji
import com.mrl.pixiv.common.data.comment.Stamp
import com.mrl.pixiv.common.kts.round
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.conditionally
import com.mrl.pixiv.common.util.throttleClick
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

private val EMOJI_REGEX = Regex("\\([a-zA-Z0-9_]+\\)")
private const val REPLACEMENT_STRING = "\u3000"

@Composable
fun CommentInput(
    state: TextFieldState,
    isSending: Boolean,
    emojis: ImmutableList<Emoji>,
    stamps: ImmutableList<Stamp>,
    onInsertEmoji: (Emoji) -> Unit,
    onSendStamp: (Stamp) -> Unit,
    onSendText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outputTransformation = remember(emojis) {
        OutputTransformation {
            val text = asCharSequence().toString()
            val matches = ArrayList<Pair<Int, Int>>()

            EMOJI_REGEX.findAll(text).forEach { matchResult ->
                val slugFull = matchResult.value
                val slug = slugFull.removeSurrounding("(", ")")
                if (emojis.any { it.slug == slug }) {
                    matches.add(matchResult.range.first to matchResult.range.last + 1)
                }
            }

            for (i in matches.indices.reversed()) {
                val (start, end) = matches[i]
                replace(start, end, "\u200B$REPLACEMENT_STRING")
            }
        }
    }
    val maxLengthTransformation = InputTransformation.maxLength(MAX_COMMENT_LENGTH)

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    var showEmojiPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisibility = WindowInsets.isImeVisible
    val colors = TextFieldDefaults.colors()

    LaunchedEffect(imeVisibility) {
        if (imeVisibility) {
            showEmojiPicker = false
        }
    }

    Surface(
        color = BottomAppBarDefaults.containerColor,
        tonalElevation = BottomAppBarDefaults.ContainerElevation,
    ) {
        Column(modifier = modifier.windowInsetsPadding(BottomAppBarDefaults.windowInsets)) {
            HorizontalDivider()
            CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
                val interactionSource = remember { MutableInteractionSource() }
                val textStyle = LocalTextStyle.current
                val textColor = textStyle.color.takeOrElse {
                    val focused = interactionSource.collectIsFocusedAsState().value
                    colors.textColor(enabled = true, isError = false, focused = focused)
                }
                val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onKeyEvent { event ->
                            if (event.key == Key.Backspace) {
                                val selection = state.selection
                                if (selection.collapsed && selection.start > 0) {
                                    val text = state.text.toString()
                                    val cursor = selection.start
                                    if (text[cursor - 1] == ')') {
                                        val start = text.lastIndexOf('(', cursor - 1)
                                        if (start != -1) {
                                            val potentialSlug = text.substring(start, cursor)
                                            if (EMOJI_REGEX.matches(potentialSlug)) {
                                                val slugName =
                                                    potentialSlug.removeSurrounding("(", ")")
                                                if (emojis.any { it.slug == slugName }) {
                                                    state.edit {
                                                        delete(start, cursor)
                                                    }
                                                    return@onKeyEvent true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            false
                        },
                    enabled = !isSending,
                    inputTransformation = maxLengthTransformation,
                    textStyle = mergedTextStyle,
                    onTextLayout = { textLayoutResult = it() },
                    cursorBrush = SolidColor(colors.cursorColor(false)),
                    outputTransformation = outputTransformation,
                    decorator = { innerTextField ->
                        val layout = textLayoutResult
                        val text = state.text
                        // Parse emojis from raw text
                        val matchedEmojis = remember(text, emojis) {
                            val list = mutableListOf<Emoji>()
                            EMOJI_REGEX.findAll(text).forEach { matchResult ->
                                val slug = matchResult.value.removeSurrounding("(", ")")
                                emojis.find { it.slug == slug }?.let { list.add(it) }
                            }
                            list
                        }

                        Box(modifier = Modifier.padding(8.dp)) {
                            innerTextField()

                            if (layout != null) {
                                val visualText = layout.layoutInput.text
                                var searchIndex = 0
                                var emojiIndex = 0

                                while (emojiIndex < matchedEmojis.size && searchIndex < visualText.length) {
                                    val index = visualText.indexOf(REPLACEMENT_STRING, searchIndex)
                                    if (index == -1) break

                                    val emoji = matchedEmojis[emojiIndex]
                                    if (index < visualText.length) {
                                        val bounds = layout.getBoundingBox(index)
                                        LoadingImage(
                                            model = emoji.imageUrlMedium,
                                            contentDescription = emoji.slug,
                                            modifier = Modifier
                                                .layout { measurable, _ ->
                                                    val width =
                                                        bounds.width.roundToInt().coerceAtLeast(0)
                                                    val height =
                                                        bounds.height.roundToInt().coerceAtLeast(0)

                                                    val placeable = measurable.measure(
                                                        Constraints.fixed(width, height)
                                                    )
                                                    layout(placeable.width, placeable.height) {
                                                        placeable.place(
                                                            bounds.left.roundToInt(),
                                                            bounds.top.roundToInt() - scrollState.value
                                                        )
                                                    }
                                                },
                                        )
                                    }

                                    searchIndex = index + REPLACEMENT_STRING.length
                                    emojiIndex++
                                }
                            }
                        }
                    },
                    scrollState = scrollState,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        showEmojiPicker = !showEmojiPicker
                        if (showEmojiPicker) {
                            keyboardController?.hide()
                        } else {
                            keyboardController?.show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = null,
                        tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${state.text.length}/$MAX_COMMENT_LENGTH",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isSending) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                } else {
                    IconButton(
                        onClick = {
                            onSendText()
                            showEmojiPicker = false
                            keyboardController?.hide()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null
                        )
                    }
                }
            }
            if (showEmojiPicker) {
                HorizontalDivider()
                EmojiPalette(
                    emojis = emojis,
                    stamps = stamps,
                    onInsertEmoji = onInsertEmoji,
                    onSendStamp = {
                        onSendStamp(it)
                        showEmojiPicker = false
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
        }
    }
}


@Composable
private fun EmojiPalette(
    emojis: ImmutableList<Emoji>,
    stamps: ImmutableList<Stamp>,
    onInsertEmoji: (Emoji) -> Unit,
    onSendStamp: (Stamp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { 2 }
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            listOf(
                RString.comment_emoji,
                RString.comment_stamp,
            ).forEachIndexed { index, resId ->
                Text(
                    text = stringResource(resId),
                    modifier = Modifier
                        .conditionally(pagerState.currentPage == index) {
                            background(MaterialTheme.colorScheme.background, 8.round)
                        }
                        .padding(vertical = 4.dp)
                        .weight(1f)
                        .throttleClick {
                            pagerState.requestScrollToPage(index)
                        },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1
        ) { index ->
            LazyVerticalGrid(
                columns = if (index == 0) GridCells.Adaptive(40.dp) else GridCells.Adaptive(80.dp),
                verticalArrangement = 4.spaceBy,
                horizontalArrangement = 4.spaceBy
            ) {
                if (index == 0) {
                    items(
                        items = emojis,
                        key = { it.id }
                    ) {
                        AsyncImage(
                            model = it.imageUrlMedium,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxSize()
                                .clickable { onInsertEmoji(it) }
                        )
                    }
                } else {
                    items(
                        items = stamps,
                        key = { it.stampId }
                    ) {
                        AsyncImage(
                            model = it.stampUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxSize()
                                .clip(4.round)
                                .throttleClick(indication = ripple()) {
                                    onSendStamp(it)
                                }
                        )
                    }
                }
            }
        }
    }

}