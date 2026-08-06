package com.mrl.pixiv.novel

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.ai_translation_in_progress
import org.jetbrains.compose.resources.stringResource

private data class ParagraphRenderData(
    val annotatedText: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>,
)

@Composable
internal fun NovelTranslationLoading(
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (centered) {
                    Modifier.heightIn(min = 240.dp)
                } else {
                    Modifier.padding(vertical = 24.dp)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        CircularWavyProgressIndicator(modifier = Modifier.size(32.dp))
        Text(
            text = stringResource(RStrings.ai_translation_in_progress),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NovelParagraph(
    paragraphIndex: Int,
    fontSize: Int,
    lineSpacingSp: Int,
    span: NovelSpanData,
    onParagraphTextLayout: (Int, TextLayoutResult) -> Unit,
    onContentClick: () -> Unit,
    onPixivImageClick: (Long) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize.sp,
        lineHeight = (fontSize + lineSpacingSp + 8).sp
    )
    val renderData = remember(span, linkColor, uriHandler, onPixivImageClick, paragraphIndex) {
        buildParagraphRenderData(
            span = span,
            paragraphIndex = paragraphIndex,
            linkColor = linkColor,
            uriHandler = uriHandler,
            onPixivImageClick = onPixivImageClick,
            textStyle = textStyle,
        )
    }

    val baseTextModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .combinedClickable(
            interactionSource = null,
            indication = null,
            onClick = onContentClick
        )

    val hasVisibleText =
        renderData.annotatedText.text.isNotBlank() || renderData.inlineContent.isNotEmpty()

    val paragraphContent: @Composable () -> Unit = {
        Text(
            text = if (hasVisibleText) renderData.annotatedText else AnnotatedString("\u200B"),
            style = if (hasVisibleText) textStyle else TextStyle(fontSize = 1.sp, lineHeight = 1.sp),
            color = if (hasVisibleText) Color.Unspecified else Color.Transparent,
            inlineContent = renderData.inlineContent,
            onTextLayout = { layoutResult ->
                onParagraphTextLayout(paragraphIndex, layoutResult)
            },
            modifier = baseTextModifier,
        )
    }

    if (span is NovelSpanData.Text || span is NovelSpanData.JumpUri) {
        SelectionContainer(content = paragraphContent)
    } else {
        paragraphContent()
    }
}

private fun buildParagraphRenderData(
    span: NovelSpanData,
    paragraphIndex: Int,
    linkColor: Color,
    uriHandler: UriHandler,
    onPixivImageClick: (Long) -> Unit,
    textStyle: TextStyle,
): ParagraphRenderData {
    val inlineContentMap = mutableMapOf<String, InlineTextContent>()
    val annotatedText = buildAnnotatedString {
        when (span) {
            is NovelSpanData.Text -> append(span.value)
            is NovelSpanData.JumpUri -> {
                val start = length
                append(span.value)
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = start,
                    end = length
                )
                addLink(
                    url = LinkAnnotation.Url(span.url) {
                        uriHandler.openUri(span.url)
                    },
                    start = start,
                    end = length
                )
            }

            is NovelSpanData.PixivImage -> {
                val inlineId = "pixiv_image_${paragraphIndex}_${span.illustId}_${span.targetIndex}"
                appendInlineContent(inlineId, "[pixivimage]")
                inlineContentMap[inlineId] = InlineTextContent(
                    placeholder = Placeholder(
                        width = 220.sp,
                        height = 180.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    val imageUrl = span.imageUrl
                    if (imageUrl.isNullOrBlank()) {
                        Text(text = span.token, style = textStyle)
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(imageUrl)
                                .build(),
                            contentDescription = span.token,
                            contentScale = ContentScale.FillBounds,
                            placeholder = rememberVectorPainter(Icons.Rounded.Refresh),
                            error = rememberVectorPainter(Icons.Rounded.ErrorOutline),
                            modifier = Modifier
                                .fillMaxSize()
                                .combinedClickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClick = { onPixivImageClick(span.illustId) }
                                ),
                        )
                    }
                }
            }

            is NovelSpanData.UploadedImage -> {
                val inlineId = "uploaded_image_${paragraphIndex}"
                appendInlineContent(inlineId, "[uploadedimage]")
                inlineContentMap[inlineId] = InlineTextContent(
                    placeholder = Placeholder(
                        width = 220.sp,
                        height = 180.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(span.url)
                            .build(),
                        contentDescription = span.url,
                        contentScale = ContentScale.FillBounds,
                        placeholder = rememberVectorPainter(Icons.Rounded.Refresh),
                        error = rememberVectorPainter(Icons.Rounded.ErrorOutline),
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                interactionSource = null,
                                indication = null,
                                onClick = { uriHandler.openUri(span.url) }
                            ),
                    )
                }
            }

            NovelSpanData.NewPage -> append("\n")
        }
    }

    return ParagraphRenderData(
        annotatedText = annotatedText,
        inlineContent = inlineContentMap,
    )
}
