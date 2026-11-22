package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.mmkv.asMutableStateFlow
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import kotlinx.coroutines.flow.asStateFlow

@Deprecated("use BlockingRepository", replaceWith = ReplaceWith("BlockingRepositoryV2"))
object BlockingRepository : MMKVOwner {
    override val id = "pixiv.user"

    private val blockIllusts by mmkvStringSet(emptySet()).asMutableStateFlow()
    val blockIllustsFlow = blockIllusts.asStateFlow()

    private val blockUsers by mmkvStringSet(emptySet()).asMutableStateFlow()
    val blockUsersFlow = blockUsers.asStateFlow()

}
