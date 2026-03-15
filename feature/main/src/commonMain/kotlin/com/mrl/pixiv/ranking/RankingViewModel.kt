package com.mrl.pixiv.ranking

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.repository.paging.IllustRankingPagingSource
import com.mrl.pixiv.common.repository.paging.NovelRankingPagingSource
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Stable
data class RankingState(
    val currentMode: RankingMode = RankingMode.DAY,
    val showR18: Boolean = false
) {
    companion object {
        val nonR18Modes = listOf(
            RankingMode.DAY,
            RankingMode.DAY_MALE,
            RankingMode.DAY_FEMALE,
            RankingMode.WEEK_ORIGINAL,
            RankingMode.WEEK_ROOKIE,
            RankingMode.WEEK,
            RankingMode.MONTH,
            RankingMode.DAY_AI,
            RankingMode.PAST,
        )
        val r18Modes = listOf(
            RankingMode.DAY_R18,
            RankingMode.DAY_MALE_R18,
            RankingMode.DAY_FEMALE_R18,
            RankingMode.DAY_R18_AI,
            RankingMode.WEEK_R18,
            RankingMode.WEEK_R18G,
        )
        val novelNonR18Modes = listOf(
            RankingMode.DAY,
            RankingMode.DAY_MALE,
            RankingMode.DAY_FEMALE,
            RankingMode.WEEK_ROOKIE,
            RankingMode.WEEK,
            RankingMode.WEEK_AI,
        )
        val novelR18Modes = listOf(
            RankingMode.DAY_R18,
            RankingMode.WEEK_R18,
            RankingMode.WEEK_AI_R18,
            RankingMode.WEEK_R18G,
        )
    }

    fun availableModes(appViewMode: AppViewMode): List<RankingMode> =
        when (appViewMode) {
            AppViewMode.ILLUST -> if (showR18) r18Modes else nonR18Modes
            AppViewMode.NOVEL -> if (showR18) novelR18Modes else novelNonR18Modes
        }
}

sealed class RankingSideEffect : SideEffect {
    data class Refresh(val mode: RankingMode) : RankingSideEffect()
}

@KoinViewModel
class RankingViewModel : BaseMviViewModel<RankingState, ViewIntent>(
    initialState = RankingState()
), KoinComponent {
    private val lazyStaggeredGridStates = mutableStateMapOf<RankingMode, LazyStaggeredGridState>()
    private val lazyListStates = mutableStateMapOf<RankingMode, LazyListState>()
    private val illustRankingList = mutableStateMapOf<RankingMode, Flow<PagingData<Illust>>>()
    private val novelRankingList = mutableStateMapOf<RankingMode, Flow<PagingData<Novel>>>()
    private val illustRankingDate = mutableStateMapOf<RankingMode, LocalDate?>()
    private val novelRankingDate = mutableStateMapOf<RankingMode, LocalDate?>()

    fun getIllustRankingFlow(mode: RankingMode): Flow<PagingData<Illust>> {
        return illustRankingList.getOrPut(mode) {
            val queryDate = getIllustRankingDate(mode)?.format(LocalDate.Formats.ISO)
            Pager(PagingConfig(pageSize = 20)) {
                IllustRankingPagingSource(mode.value, queryDate)
            }.flow.cachedIn(viewModelScope)
        }
    }

    fun getNovelRankingFlow(mode: RankingMode): Flow<PagingData<Novel>> {
        return novelRankingList.getOrPut(mode) {
            val queryDate = getNovelRankingDate(mode)?.format(LocalDate.Formats.ISO)
            Pager(PagingConfig(pageSize = 30)) {
                NovelRankingPagingSource(mode.value, queryDate)
            }.flow.cachedIn(viewModelScope)
        }
    }

    fun getLazyStaggeredGridState(mode: RankingMode): LazyStaggeredGridState {
        return lazyStaggeredGridStates.getOrPut(mode) {
            LazyStaggeredGridState()
        }
    }

    fun getLazyListState(mode: RankingMode): LazyListState {
        return lazyListStates.getOrPut(mode) {
            LazyListState()
        }
    }

    fun getIllustRankingDate(mode: RankingMode): LocalDate? {
        return illustRankingDate.getOrPut(mode) {
            if (mode == RankingMode.PAST) {
                (Clock.System.now() - 1.days)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            } else {
                null
            }
        }
    }

    fun getNovelRankingDate(mode: RankingMode): LocalDate? {
        return novelRankingDate.getOrPut(mode) {
            if (mode == RankingMode.PAST) {
                (Clock.System.now() - 1.days)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            } else {
                null
            }
        }
    }

    override suspend fun handleIntent(intent: ViewIntent) {

    }

    fun selectMode(mode: RankingMode) {
        updateState { copy(currentMode = mode) }
    }

    fun changeDate(date: LocalDate, appViewMode: AppViewMode) {
        val mode = state.currentMode
        when (appViewMode) {
            AppViewMode.ILLUST -> {
                illustRankingDate[mode] = date
                illustRankingList.remove(mode)
            }

            AppViewMode.NOVEL -> {
                novelRankingDate[mode] = date
                novelRankingList.remove(mode)
            }
        }
        sendEffect(RankingSideEffect.Refresh(mode))
    }

    fun refresh(mode: RankingMode) {
        sendEffect(RankingSideEffect.Refresh(mode))
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

    fun switchViewMode(mode: AppViewMode) {
        SettingRepository.setAppViewMode(mode)
        updateState {
            copy(currentMode = if (showR18) RankingMode.DAY_R18 else RankingMode.DAY)
        }
    }
}
