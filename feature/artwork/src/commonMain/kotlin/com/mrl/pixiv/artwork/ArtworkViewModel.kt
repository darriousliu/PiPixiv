package com.mrl.pixiv.artwork

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.repository.paging.UserIllustPagingSource
import com.mrl.pixiv.common.repository.paging.UserNovelsPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ArtworkViewModel(
    private val userId: Long,
) : BaseMviViewModel<Any, ViewIntent>(
    initialState = Any()
) {
    val userIllusts = Pager(PagingConfig(pageSize = 20)) {
        UserIllustPagingSource(userId, Type.Illust)
    }.flow.cachedIn(viewModelScope)

    val userNovels = Pager(PagingConfig(pageSize = 20)) {
        UserNovelsPagingSource(userId)
    }.flow.cachedIn(viewModelScope)

    val userMangas = Pager(PagingConfig(pageSize = 20)) {
        UserIllustPagingSource(userId, Type.Manga)
    }.flow.cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: ViewIntent) {

    }
}
