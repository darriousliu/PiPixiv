package com.mrl.pixiv.novel

import com.mrl.pixiv.common.repository.NovelReadingProgress

internal sealed interface NovelMarkerMutation {
    data class Save(val page: Int) : NovelMarkerMutation

    data object Delete : NovelMarkerMutation
}

internal fun resolveNovelMarkerMutation(
    savedPage: Int?,
    currentPage: Int,
): NovelMarkerMutation {
    val page = currentPage.coerceAtLeast(1)
    return if (savedPage == page) {
        NovelMarkerMutation.Delete
    } else {
        NovelMarkerMutation.Save(page)
    }
}

internal fun initialMarkerPageForNovel(
    initialNovelId: Long,
    loadedNovelId: Long,
    requestedMarkerPage: Int?,
): Int? = requestedMarkerPage
    ?.takeIf { initialNovelId == loadedNovelId && it > 0 }

internal fun shouldApplyNovelMarkerUpdate(
    currentNovelId: Long?,
    markerNovelId: Long,
): Boolean = currentNovelId == markerNovelId

internal class NovelProgressSession {
    private var novelId: Long? = null
    private var progress: NovelReadingProgress? = null

    fun get(novelId: Long): NovelReadingProgress? =
        progress.takeIf { this.novelId == novelId }

    fun update(novelId: Long, progress: NovelReadingProgress) {
        this.novelId = novelId
        this.progress = progress
    }

    fun clear(novelId: Long) {
        if (this.novelId == novelId) {
            this.novelId = null
            progress = null
        }
    }
}

internal fun markerPageForParagraph(
    spans: List<NovelSpanData>,
    paragraphIndex: Int,
): Int {
    val pages = markerPagesForSpans(spans)
    if (pages.isEmpty()) return 1
    return pages[paragraphIndex.coerceIn(0, pages.lastIndex)]
}

internal fun markerPagesForSpans(spans: List<NovelSpanData>): List<Int> {
    var currentPage = 1
    return spans.map { span ->
        currentPage.also {
            if (span is NovelSpanData.NewPage) {
                currentPage += 1
            }
        }
    }
}

internal fun paragraphIndexForMarkerPage(
    spans: List<NovelSpanData>,
    markerPage: Int,
): Int {
    if (spans.isEmpty() || markerPage <= 1) return 0

    var currentPage = 1
    spans.forEachIndexed { index, span ->
        if (span is NovelSpanData.NewPage) {
            currentPage += 1
            if (currentPage == markerPage) {
                return (index + 1).coerceAtMost(spans.lastIndex)
            }
        }
    }
    return spans.lastIndex
}
