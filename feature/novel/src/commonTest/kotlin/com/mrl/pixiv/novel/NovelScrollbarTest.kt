package com.mrl.pixiv.novel

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelScrollbarTest {
    @Test
    fun draggingAcrossTallCoverAndDenseTextNeverReversesTheSettledPosition() = runTest {
        val reader = FakeReader(listOf(128, 10_000, 1), listOf(1000, 1000, 10), 1200)
        var previous = 0.0
        (1 until 100).forEach { step ->
            val fraction = step / 100f
            reader.seek(fraction)
            assertTrue(reader.offset >= previous, "进度 $fraction 不应让正文倒退")
            assertEquals(fraction, reader.fraction(), 0.001f)
            previous = reader.offset
        }
        (98 downTo 1).forEach { step ->
            val fraction = step / 100f
            reader.seek(fraction)
            assertTrue(reader.offset <= previous, "进度 $fraction 不应让正文向前跳")
            assertEquals(fraction, reader.fraction(), 0.001f)
            previous = reader.offset
        }
    }

    @Test
    fun repeatedLargeDragsLandAtTheRequestedProgressAcrossMixedParagraphs() = runTest {
        val reader = FakeReader(
            listOf(128, 20_000, 1, 128, 5000, 1),
            listOf(800, 30_000, 2, 600, 12_000, 32),
            1500,
        )
        listOf(0.8f, 0.2f, 0.99f, 0.01f, 0.5f, 0.9f).forEach { fraction ->
            reader.seek(fraction)
            assertEquals(fraction, reader.fraction(), 0.001f)
        }
    }

    @Test
    fun longAndShortParagraphTransitionsDoNotReverseProgressOrResizeThumb() {
        val content = NovelScrollContent(listOf(128, 10_000, 1, 500, 1))
        val windows = listOf(
            0.0 to 128.0,
            64.0 to 628.0,
            128.0 to 1128.0,
            9000.0 to 10_000.0,
            9800.0 to 10_300.0,
            10_128.0 to 10_629.0,
            10_300.0 to content.total,
        )
        val metrics = windows.map { (start, end) -> content.metrics(start, end) }
        metrics.zipWithNext().forEach { (before, after) ->
            assertTrue(after.thumbOffsetFraction >= before.thumbOffsetFraction)
            assertEquals(before.thumbSizeFraction, after.thumbSizeFraction)
        }
        assertEquals(0f, metrics.first().thumbOffsetFraction)
        assertEquals(1f - metrics.last().thumbSizeFraction, metrics.last().thumbOffsetFraction)
    }

    @Test
    fun singleVeryLongParagraphTracksItsInternalPosition() {
        val content = NovelScrollContent(listOf(100_000))
        val start = content.position(0, 0.45)
        val end = content.position(0, 0.55)
        val metrics = content.metrics(start, end)
        assertEquals(0.5f, metrics.thumbOffsetFraction / (1f - metrics.thumbSizeFraction), 0.00001f)
        assertEquals(NovelScrollTarget(0, 0.5), content.target(0.5f))
    }

    @Test
    fun changingVisibleItemCountDoesNotChangeTheDocumentScale() {
        val content = NovelScrollContent(listOf(1, 10_000, 1, 1, 1))
        val target = assertNotNull(content.target(0.5f))
        assertEquals(1, target.index)
        assertEquals(content.total / 2, content.position(target.index, target.itemFraction), 0.00001)
        assertEquals(10_004.0, content.total)
    }

    @Test
    fun dragTargetsAreMonotonicAcrossVeryDifferentItemSizes() {
        val content = NovelScrollContent(listOf(128, 1, 100_000, 1, 500, 1))
        val targets = (0..100).map { step ->
            val target = assertNotNull(content.target(step / 100f))
            content.position(target.index, target.itemFraction)
        }
        targets.zipWithNext().forEach { (before, after) -> assertTrue(after >= before) }
        assertEquals(0.0, targets.first())
        assertEquals(content.total, targets.last())
    }

    @Test
    fun allMetadataVariantsMatchTheReaderItemIndices() {
        val spans = listOf(NovelSpanData.Text("正文"), NovelSpanData.NewPage)
        listOf(false, true).forEach { hasSeries ->
            listOf("", "简介").forEach { caption ->
                val content = novelScrollContent(spans, hasSeries, caption)
                val start = paragraphStartItemIndex(hasSeries, caption.isNotEmpty())
                assertEquals(start + spans.size + 1, content.size)
                assertEquals(2.0, content.position(start + 1, 0.0) - content.position(start, 0.0))
            }
        }
    }

    @Test
    fun emptyTextAndImagesRemainReachable() {
        val content = novelScrollContent(
            spans = listOf(NovelSpanData.Text(""), NovelSpanData.UploadedImage("image"), NovelSpanData.NewPage),
            hasSeriesTitle = false,
            caption = "",
        )
        assertTrue(content.position(9, 0.0) > content.position(8, 0.0))
        assertTrue(content.position(10, 0.0) > content.position(9, 0.0))
    }

    @Test
    fun shortAndEmptyContentHideTheScrollbar() {
        assertEquals(1f, NovelScrollContent(listOf(20)).metrics(0.0, 20.0).thumbSizeFraction)
        val empty = NovelScrollContent(emptyList())
        assertNull(empty.target(0.5f))
        assertEquals(1f, empty.metrics(0.0, 0.0).thumbSizeFraction)
    }

    @Test
    fun paddingAndOutOfRangeFractionsAreClampedAtBothEnds() {
        val content = NovelScrollContent(listOf(100, 900))
        assertEquals(NovelScrollTarget(0, 0.0), content.target(-1f))
        assertEquals(NovelScrollTarget(1, 1.0), content.target(2f))
        assertEquals(0f, content.metrics(-20.0, 50.0).thumbOffsetFraction)
        val bottom = content.metrics(950.0, 1020.0)
        assertEquals(1f - bottom.thumbSizeFraction, bottom.thumbOffsetFraction)
    }

    private class FakeReader(weights: List<Int>, val heights: List<Int>, val viewport: Int) {
        val content = NovelScrollContent(weights)
        val tops = heights.runningFold(0) { sum, height -> sum + height }
        var offset = 0.0

        fun logicalPosition(pixel: Double): Double {
            val index = heights.indices.firstOrNull { pixel < tops[it + 1] } ?: heights.lastIndex
            return content.position(index, (pixel - tops[index]) / heights[index])
        }

        fun range() = logicalPosition(offset) to logicalPosition(offset + viewport)

        fun fraction(): Float {
            val (start, end) = range()
            val metrics = content.metrics(start, end)
            return metrics.thumbOffsetFraction / (1f - metrics.thumbSizeFraction)
        }

        suspend fun seek(fraction: Float) {
            content.seek(fraction, ::range) { target ->
                offset = (tops[target.index] + target.itemFraction * heights[target.index])
                    .coerceIn(0.0, (tops.last() - viewport).toDouble())
            }
        }
    }
}
