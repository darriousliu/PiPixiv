package com.mrl.pixiv.ranking

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.repository.paging.IllustRankingPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import kotlinx.coroutines.flow.Flow
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Stable
data class RankingState(
    val currentMode: RankingMode = RankingMode.DAY,
    val date: String? = null,
    val showR18: Boolean = false
) {
    companion object {
        val nonR18Modes = RankingMode.entries.filter { it.type == RankingType.GENERAL }
        val r18Modes = RankingMode.entries.filter { it.type != RankingType.GENERAL }
    }

    val availableModes: List<RankingMode>
        get() = if (showR18) r18Modes else nonR18Modes
}

@KoinViewModel
class RankingViewModel : BaseMviViewModel<RankingState, ViewIntent>(
    initialState = RankingState()
), KoinComponent {
    // Stores LazyStaggeredGridState for each mode to preserve scroll position
    val lazyStaggeredGridStates = mutableStateMapOf<RankingMode, LazyStaggeredGridState>()
    val rankingList = mutableStateMapOf<RankingMode, Flow<PagingData<Illust>>>()

    fun getRankingFlow(mode: RankingMode): Flow<PagingData<Illust>> {
        return rankingList.getOrPut(mode) {
            val queryDate = if (mode == RankingMode.PAST) {
                state.date ?: LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE)
            } else {
                null
            }
            Pager(PagingConfig(pageSize = 20)) {
                IllustRankingPagingSource(mode.value, queryDate)
            }.flow.cachedIn(viewModelScope)
        }
    }

    fun getLazyStaggeredGridState(mode: RankingMode): LazyStaggeredGridState {
        return lazyStaggeredGridStates.getOrPut(mode) {
            LazyStaggeredGridState()
        }
    }

    override suspend fun handleIntent(intent: ViewIntent) {

    }

    fun selectMode(mode: RankingMode) {
        updateState { copy(currentMode = mode) }
    }

    fun changeDate(date: String) {
        updateState { copy(date = date) }
        // Clear PAST ranking flow so it gets recreated with new date
        rankingList.remove(RankingMode.PAST)
    }

    fun toggleR18() {
        updateState {
            val newShowR18 = !showR18
            val newMode = if (newShowR18) {
                RankingMode.DAY_R18
            } else {
                RankingMode.DAY
            }
            copy(showR18 = newShowR18, currentMode = newMode)
        }
    }
}
