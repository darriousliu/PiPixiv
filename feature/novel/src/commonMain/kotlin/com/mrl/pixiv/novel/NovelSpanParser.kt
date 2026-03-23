package com.mrl.pixiv.novel

import com.mrl.pixiv.common.data.novel.NovelIllusts
import com.mrl.pixiv.common.data.novel.NovelTextResp

internal sealed interface NovelSpanData {
    data class Text(val value: String) : NovelSpanData

    data object NewPage : NovelSpanData

    data class JumpUri(
        val value: String,
        val url: String,
    ) : NovelSpanData

    data class PixivImage(
        val illustId: Long,
        val targetIndex: Int,
        val token: String,
        val imageUrl: String?,
    ) : NovelSpanData

    data class UploadedImage(
        val url: String,
    ) : NovelSpanData
}

internal class NovelSpanParser {
    private val linkRegex = Regex("https?://\\S+")

    fun buildSpans(source: String, webResponse: NovelTextResp?): List<NovelSpanData> {
        if (source.isEmpty()) return emptyList()
        return try {
            var nowStr = ""
            var spanCollectStart = false
            val result = mutableListOf<NovelSpanData>()

            source.forEach { posStr ->
                when (posStr) {
                    '[' -> {
                        if (nowStr.isEmpty()) {
                            nowStr = "["
                            spanCollectStart = true
                        } else {
                            if (nowStr == "[") {
                                spanCollectStart = true
                                nowStr += "["
                            } else {
                                result += NovelSpanData.Text(nowStr)
                                nowStr = "["
                                spanCollectStart = true
                            }
                        }
                    }

                    ']' -> {
                        if (nowStr.startsWith("[[")) {
                            if (nowStr.endsWith("]")) {
                                spanCollectStart = false
                                nowStr += "]"
                                result += parseToken(nowStr, webResponse)
                                nowStr = ""
                            } else {
                                nowStr += "]"
                            }
                        } else {
                            spanCollectStart = false
                            nowStr += "]"
                            result += parseToken(nowStr, webResponse)
                            nowStr = ""
                        }
                    }

                    else -> {
                        nowStr += if (spanCollectStart) {
                            posStr
                        } else {
                            posStr
                        }
                    }
                }
            }

            if (nowStr.isNotEmpty()) {
                result += NovelSpanData.Text(nowStr)
            }

            result
        } catch (_: Exception) {
            listOf(NovelSpanData.Text(source))
        }
    }

    private fun parseToken(spanStr: String, webResponse: NovelTextResp?): NovelSpanData {
        return when {
            spanStr.startsWith("[newpage]") || spanStr.startsWith("(newpage)") -> {
                NovelSpanData.NewPage
            }

            spanStr.startsWith("[chapter:") -> {
                val title = spanStr.removePrefix("[chapter:").removeSuffix("]")
                NovelSpanData.Text(title)
            }

            spanStr.startsWith("[pixivimage:") -> {
                parsePixivImage(spanStr = spanStr, webResponse = webResponse)
            }

            spanStr.startsWith("[uploadedimage:") -> {
                val flag = "[uploadedimage:"
                val now = spanStr.substring(flag.length, spanStr.indexOf("]"))
                val image = webResponse?.images?.get(now)
                val url = image?.urls?.the128X128
                    ?.takeIf { it.isNotBlank() }
                    ?: image?.urls?.the1200X1200?.takeIf { it.isBlank().not() }
                    ?: image?.urls?.original?.takeIf { it.isBlank().not() }
                if (url != null) {
                    NovelSpanData.UploadedImage(url)
                } else {
                    NovelSpanData.Text(now)
                }
            }

            spanStr.startsWith("[[jumpuri:") -> {
                val flag = "[[jumpuri:"
                val now = spanStr.substring(flag.length, spanStr.indexOf("]"))
                val link = linkRegex.find(now)?.value
                if (link != null && link.contains("pixiv.net")) {
                    NovelSpanData.JumpUri(value = link, url = link)
                } else {
                    NovelSpanData.Text(now)
                }
            }

            spanStr.startsWith("[[rb:") -> {
                val flag = "[[rb:"
                val text = spanStr.removePrefix(flag).removeSuffix("]")
                val contentText = text.split('>', '＞')
                if (contentText.size >= 2) {
                    NovelSpanData.Text("${contentText.first()}(${contentText.last()})")
                } else {
                    NovelSpanData.Text(text)
                }
            }

            else -> {
                NovelSpanData.Text(spanStr)
            }
        }
    }

    private fun parsePixivImage(
        spanStr: String,
        webResponse: NovelTextResp?,
    ): NovelSpanData {
        val flag = "[pixivimage:"
        val now = spanStr.substring(flag.length, spanStr.indexOf("]"))

        var trueId = now.toLongOrNull() ?: 0L
        var targetIndex = 0
        if (now.contains('-')) {
            val parts = now.split('-')
            trueId = parts.firstOrNull()?.toLongOrNull() ?: trueId
            targetIndex = parts.lastOrNull()?.toIntOrNull() ?: 0
        }

        val illust = resolveIllustRef(
            webResponse = webResponse,
            key = now,
            illustId = trueId,
            targetIndex = targetIndex
        )
        val imageUrl = resolvePixivImageUrl(illust = illust)

        return if (trueId > 0L) {
            NovelSpanData.PixivImage(
                illustId = trueId,
                targetIndex = targetIndex,
                token = spanStr,
                imageUrl = imageUrl,
            )
        } else {
            NovelSpanData.Text(now)
        }
    }

    private fun resolveIllustRef(
        webResponse: NovelTextResp?,
        key: String,
        illustId: Long,
        targetIndex: Int,
    ): NovelIllusts? {
        val illusts = webResponse?.illusts ?: return null
        return illusts[key]
            ?: illusts["$illustId-$targetIndex"]
            ?: illusts[illustId.toString()]
    }

    private fun resolvePixivImageUrl(illust: NovelIllusts?): String? {
        val inlineIllust = illust?.illust ?: return null
        return inlineIllust.images.medium.takeIf { !it.isNullOrBlank() }
            ?: inlineIllust.images.original.takeIf { !it.isNullOrBlank() }
            ?: inlineIllust.images.small.takeIf { !it.isNullOrBlank() }
    }
}
