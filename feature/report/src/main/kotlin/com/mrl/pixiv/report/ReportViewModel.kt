package com.mrl.pixiv.report

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.data.report.ReportTopic
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.router.ReportType
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.koin.android.annotation.KoinViewModel

@Stable
data class ReportCommentState(
    val topicList: ImmutableList<ReportTopic> = persistentListOf(),
    val selectedTopicId: Int? = null,
)

@KoinViewModel
class ReportCommentViewModel(
    private val id: Long,
    private val type: ReportType,
) : BaseMviViewModel<ReportCommentState, ViewIntent>(
    initialState = ReportCommentState()
) {
    val reportContent = TextFieldState()

    override suspend fun handleIntent(intent: ViewIntent) {
    }

    init {
        fetchTopicList()
    }

    private fun fetchTopicList() {
        launchIO {
            val resp = when (type) {
                ReportType.USER -> PixivRepository.getUserReportTopicList()
                ReportType.ILLUST -> PixivRepository.getIllustReportTopicList()
                ReportType.NOVEL -> PixivRepository.getNovelReportTopicList()
                ReportType.ILLUST_COMMENT -> PixivRepository.getIllustCommentReportTopicList()
                ReportType.NOVEL_COMMENT -> PixivRepository.getNovelCommentReportTopicList()
            }
            updateState {
                ReportCommentState(topicList = resp.topicList.toPersistentList())
            }
        }
    }

    fun selectTopic(topicId: Int) {
        updateState {
            ReportCommentState(selectedTopicId = topicId)
        }
    }

    fun submitReport() {
        launchIO {
            val topicId = state.selectedTopicId ?: return@launchIO
            val description = reportContent.text.toString().ifBlank { return@launchIO }
            when (type) {
                ReportType.USER -> PixivRepository.reportUser(
                    userId = id,
                    topicId = topicId,
                    description = description
                )

                ReportType.ILLUST -> PixivRepository.reportIllust(
                    illustId = id,
                    topicId = topicId,
                    description = description
                )

                ReportType.NOVEL -> PixivRepository.reportNovel(
                    novelId = id,
                    topicId = topicId,
                    description = description
                )

                ReportType.ILLUST_COMMENT -> PixivRepository.reportIllustComment(
                    commentId = id,
                    topicId = topicId,
                    description = description
                )

                ReportType.NOVEL_COMMENT -> PixivRepository.reportNovelComment(
                    commentId = id,
                    topicId = topicId,
                    description = description
                )
            }
        }
    }
}
