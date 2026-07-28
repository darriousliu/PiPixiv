package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.data.Novel
import org.koin.core.annotation.Single

data class NovelReadLaterSourceContent(
    val novel: Novel,
    val text: String,
)

interface NovelReadLaterSource {
    suspend fun load(novelId: Long): NovelReadLaterSourceContent
}

@Single(binds = [NovelReadLaterSource::class])
class PixivNovelReadLaterSource : NovelReadLaterSource {
    override suspend fun load(novelId: Long): NovelReadLaterSourceContent {
        val novel = PixivRepository.getNovelDetail(novelId).novel
        val html = PixivRepository.getNovelContent(novelId)
        val text = requireNotNull(NovelContentParser.extract(html)) {
            "Novel content is unavailable."
        }.text
        require(text.isNotBlank()) {
            "Novel content is empty."
        }
        return NovelReadLaterSourceContent(novel = novel, text = text)
    }
}
