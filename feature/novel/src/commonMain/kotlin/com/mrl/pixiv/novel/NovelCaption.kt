package com.mrl.pixiv.novel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import be.digitalia.compose.htmlconverter.HtmlStyle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

internal sealed interface NovelCaptionLinkTarget {
    data class Illust(val id: Long) : NovelCaptionLinkTarget

    data class Novel(val id: Long) : NovelCaptionLinkTarget

    data class User(val id: Long) : NovelCaptionLinkTarget

    data class External(val url: String) : NovelCaptionLinkTarget
}

internal fun resolveNovelCaptionLink(rawUrl: String): NovelCaptionLinkTarget? {
    if (rawUrl.isEmpty() || rawUrl.any { it.isWhitespace() || it.code < 0x20 }) {
        return null
    }

    val schemeSeparatorIndex = rawUrl.indexOf("://")
    if (schemeSeparatorIndex <= 0) return null

    val scheme = rawUrl.substring(0, schemeSeparatorIndex).lowercase()
    val remainder = rawUrl.substring(schemeSeparatorIndex + 3)
    return when (scheme) {
        "pixiv" -> resolvePixivCaptionLink(remainder)
        "http", "https" -> {
            val authority = remainder.substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
            if (isValidHttpAuthority(authority)) {
                NovelCaptionLinkTarget.External(rawUrl)
            } else {
                null
            }
        }

        else -> null
    }
}

private fun isValidHttpAuthority(authority: String): Boolean {
    if (authority.isBlank() || authority.any { it.code < 0x21 }) return false

    val hostAndPort = authority.substringAfterLast('@')
    val host = if (hostAndPort.startsWith('[')) {
        val closingBracket = hostAndPort.indexOf(']')
        if (closingBracket <= 1) return false
        val suffix = hostAndPort.substring(closingBracket + 1)
        if (suffix.isNotEmpty()) {
            if (!suffix.startsWith(':')) return false
            val port = suffix.drop(1).toIntOrNull() ?: return false
            if (port !in 1..65535) return false
        }
        hostAndPort.substring(1, closingBracket)
    } else {
        val portSeparator = hostAndPort.lastIndexOf(':')
        if (portSeparator == -1) {
            hostAndPort
        } else {
            val port = hostAndPort.substring(portSeparator + 1).toIntOrNull() ?: return false
            if (port !in 1..65535) return false
            hostAndPort.substring(0, portSeparator)
        }
    }

    return host.isNotBlank() &&
            host.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == ':' }
}

private fun resolvePixivCaptionLink(remainder: String): NovelCaptionLinkTarget? {
    if ('?' in remainder || '#' in remainder) return null

    val segments = remainder.trimEnd('/').split('/')
    if (segments.size != 2) return null

    val id = segments[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
    return when (segments[0].lowercase()) {
        "illusts" -> NovelCaptionLinkTarget.Illust(id)
        "novels" -> NovelCaptionLinkTarget.Novel(id)
        "users" -> NovelCaptionLinkTarget.User(id)
        else -> null
    }
}

internal fun novelCaptionToAnnotatedString(
    html: String,
    linkColor: Color,
    onLinkClick: (String) -> Unit,
): AnnotatedString = htmlToAnnotatedString(
    html = html,
    compactMode = true,
    style = HtmlStyle(
        textLinkStyles = TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            )
        )
    ),
    linkInteractionListener = LinkInteractionListener { link ->
        val url = (link as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
        onLinkClick(url)
    },
)
