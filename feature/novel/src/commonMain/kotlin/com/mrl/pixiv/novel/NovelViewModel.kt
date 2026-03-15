package com.mrl.pixiv.novel

import androidx.compose.runtime.Stable
import com.dokar.sonner.ToastType
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent

@Stable
data class NovelState(
    val loading: Boolean = true,
    val novel: Novel? = null,
    val novelText: String = "",
    val fontSize: Int = 16,
    val lineSpacingSp: Int = 0,
    val isBookmarked: Boolean = false,
    val showBottomSheet: Boolean = false,
    val paragraphs: ImmutableList<String> = persistentListOf(),
    val prevNovelId: Long? = null,
    val nextNovelId: Long? = null,
)

sealed class NovelIntent : ViewIntent {
    data class LoadNovelDetail(val novelId: Long) : NovelIntent()
    data object ToggleBookmark : NovelIntent()
    data class UpdateFontSize(val size: Int) : NovelIntent()
    data class UpdateLineSpacing(val spacing: Int) : NovelIntent()
    data object ToggleBottomSheet : NovelIntent()
    data object ShareNovel : NovelIntent()
    data object ExportToTxt : NovelIntent()
    data class NavigateToChapter(val novelId: Long) : NovelIntent()
}

@KoinViewModel
class NovelViewModel(
    private val novelId: Long,
) : BaseMviViewModel<NovelState, NovelIntent>(
    initialState = NovelState()
), KoinComponent {

    init {
        dispatch(NovelIntent.LoadNovelDetail(novelId))
    }

    override suspend fun handleIntent(intent: NovelIntent) {
        when (intent) {
            is NovelIntent.LoadNovelDetail -> loadNovelDetail(intent.novelId)
            is NovelIntent.ToggleBookmark -> toggleBookmark()
            is NovelIntent.UpdateFontSize -> updateFontSize(intent.size)
            is NovelIntent.UpdateLineSpacing -> updateLineSpacing(intent.spacing)
            is NovelIntent.ToggleBottomSheet -> toggleBottomSheet()
            is NovelIntent.ShareNovel -> shareNovel()
            is NovelIntent.ExportToTxt -> exportToTxt()
            is NovelIntent.NavigateToChapter -> loadNovelDetail(intent.novelId)
        }
    }

    private suspend fun loadNovelDetail(novelId: Long) {
        updateState { copy(loading = true) }
        try {
            val response = PixivRepository.getNovelDetail(novelId)
            val novel = response.novel

            // 使用caption作为示例文本(MVP版本)
            val text = novel.caption.ifEmpty { "暂无内容" }
            val paragraphs = text.split("\n").toImmutableList()

            // TODO: 后续实现从webview HTML获取真实小说文本
            // val novelTextResp = PixivRepository.getNovelText(novelId)
            // val seriesNav = novelTextResp.seriesNavigation

            updateState {
                copy(
                    loading = false,
                    novel = novel,
                    novelText = text,
                    paragraphs = paragraphs,
                    isBookmarked = novel.isBookmarked,
                    prevNovelId = null, // TODO: seriesNav?.prevNovel?.id
                    nextNovelId = null, // TODO: seriesNav?.nextNovel?.id
                )
            }
        } catch (e: Exception) {
            updateState { copy(loading = false) }
            handleError(e)
            ToastUtil.safeShortToast("加载小说详情失败: ${e.message}")
        }
    }

    private suspend fun toggleBookmark() {
        val novel = uiState.value.novel ?: return
        val currentBookmarkState = uiState.value.isBookmarked

        try {
            if (currentBookmarkState) {
                PixivRepository.postNovelBookmarkDelete(novel.id)
                updateState {
                    copy(
                        isBookmarked = false,
                        novel = novel.copy(
                            isBookmarked = false,
                            totalBookmarks = (novel.totalBookmarks - 1).coerceAtLeast(0)
                        )
                    )
                }
                ToastUtil.safeShortToast("已取消收藏", type = ToastType.Success)
            } else {
                PixivRepository.postNovelBookmarkAdd(novel.id, Restrict.PUBLIC)
                updateState {
                    copy(
                        isBookmarked = true,
                        novel = novel.copy(
                            isBookmarked = true,
                            totalBookmarks = novel.totalBookmarks + 1
                        )
                    )
                }
                ToastUtil.safeShortToast("收藏成功", type = ToastType.Success)
            }
        } catch (e: Exception) {
            handleError(e)
            ToastUtil.safeShortToast("操作失败: ${e.message}", type = ToastType.Error)
        }
    }

    private fun updateFontSize(size: Int) {
        updateState { copy(fontSize = size.coerceIn(10, 32)) }
    }

    private fun updateLineSpacing(spacing: Int) {
        updateState { copy(lineSpacingSp = spacing.coerceIn(-10, 10)) }
    }

    private fun toggleBottomSheet() {
        updateState { copy(showBottomSheet = !showBottomSheet) }
    }

    private fun shareNovel() {
        val novel = uiState.value.novel ?: return
        val url = "https://www.pixiv.net/novel/show.php?id=${novel.id}"
        ShareUtil.shareText(url)
        ToastUtil.safeShortToast("已复制链接", type = ToastType.Success)
    }

    private fun exportToTxt() {
        val novel = uiState.value.novel ?: return
        val text = uiState.value.novelText

        // TODO: 实现txt文件导出
        // 这需要使用FileKit或平台特定API
        ToastUtil.safeShortToast("导出功能开发中...", type = ToastType.Info)
    }
}
