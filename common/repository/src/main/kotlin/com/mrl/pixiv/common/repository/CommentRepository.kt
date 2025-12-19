package com.mrl.pixiv.common.repository

import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.mrl.pixiv.common.data.comment.EmojiResp
import com.mrl.pixiv.common.data.comment.StampsResp
import com.mrl.pixiv.common.mmkv.MMKVApp
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.util.AppUtil
import kotlinx.coroutines.flow.StateFlow

object CommentRepository : MMKVApp {
    private val stampsCache by mmkvSerializable<StampsResp?>(null).asMutableStateFlow()
    val stampsCacheFlow: StateFlow<StampsResp?> = stampsCache

    private val emojiCache by mmkvSerializable<EmojiResp?>(null).asMutableStateFlow()
    val emojiCacheFlow: StateFlow<EmojiResp?> = emojiCache

    suspend fun loadStamps() {
        val cache = stampsCache.value
        if (cache != null) return
        val remote = PixivRepository.getStamps()
        stampsCache.value = remote
        SingletonImageLoader.get(AppUtil.appContext).let { imageLoader ->
            remote.stamps.forEach {
                imageLoader.execute(
                    ImageRequest.Builder(AppUtil.appContext).data(it.stampUrl).build()
                )
            }
        }
    }

    suspend fun loadEmojis() {
        val cache = emojiCache.value
        if (cache != null) return
        val remote = PixivRepository.getEmojis()
        emojiCache.value = remote
        SingletonImageLoader.get(AppUtil.appContext).let { imageLoader ->
            remote.emojiDefinitions.forEach {
                imageLoader.execute(
                    ImageRequest.Builder(AppUtil.appContext).data(it.imageUrlMedium).build()
                )
            }
        }
    }
}
