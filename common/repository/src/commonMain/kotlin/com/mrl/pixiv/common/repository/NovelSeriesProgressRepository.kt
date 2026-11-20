package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.mmkv.AccountMMKVProperty
import com.mrl.pixiv.common.mmkv.AccountScoped
import com.mrl.pixiv.common.mmkv.AccountStore
import com.mrl.pixiv.common.mmkv.MMKVUser
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Serializable
data class NovelSeriesReadingProgress(
    val novelId: Long,
    val title: String,
    val fraction: Float,
)

fun readingProgressFraction(progress: NovelReadingProgress, paragraphs: List<String>): Float {
    val total = paragraphs.sumOf { it.length.toLong() }
    if (total == 0L) return 0f
    val index = progress.paragraphIndex.coerceIn(paragraphs.indices)
    val read = paragraphs.take(index).sumOf { it.length.toLong() } +
        progress.charIndex.coerceIn(0, paragraphs[index].length)
    return (read.toDouble() / total).toFloat().coerceIn(0f, 1f)
}

@Single
class NovelSeriesProgressRepository : MMKVUser, AccountScoped {
    private val novelSeriesProgress = AccountMMKVProperty(
        mmkvSerializable<Map<Long, NovelSeriesReadingProgress>>(emptyMap()),
        "novelSeriesProgress",
        emptyMap(),
    )

    override fun reload() = novelSeriesProgress.reload(AccountStore.activeUserId)

    fun observe(seriesId: Long): Flow<NovelSeriesReadingProgress?> =
        novelSeriesProgress.flow.map { it[seriesId] }.distinctUntilChanged()

    fun record(novel: Novel, fraction: Float) {
        val userId = AccountStore.activeUserId
        val seriesId = novel.series.id?.takeIf { it > 0 } ?: return
        novelSeriesProgress.update(userId) {
            it + (seriesId to NovelSeriesReadingProgress(novel.id, novel.title, fraction.coerceIn(0f, 1f)))
        }
    }

    fun exportForAccount(userId: Long): Map<Long, NovelSeriesReadingProgress> =
        novelSeriesProgress.read(userId)

    fun restoreForAccount(userId: Long, progress: Map<Long, NovelSeriesReadingProgress>) {
        novelSeriesProgress.update(userId) { it + progress }
    }
}
