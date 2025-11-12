package com.mrl.pixiv.common.domain.illust

import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.illust.IllustBookmarkDetailResp
import com.mrl.pixiv.common.repository.PixivRepository

object GetIllustBookmarkDetailUseCase {
    suspend operator fun invoke(illustId: Long): IllustBookmarkDetailResp {
        val resp = withIOContext { PixivRepository.getIllustBookmarkDetail(illustId) }
        return resp
    }
}