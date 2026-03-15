package com.mrl.pixiv.novel

import androidx.compose.runtime.Stable
import com.dokar.sonner.ToastType
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.novel.NovelTextResp
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
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
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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

            val novelHtml = PixivRepository.getNovelContent(novelId)
            val novelText = extractNovelData(novelHtml)
            val text = novelText?.text.orEmpty()
            val paragraphs = text.split("\n\n").toImmutableList()

            updateState {
                copy(
                    loading = false,
                    novel = novel,
                    novelText = text,
                    paragraphs = paragraphs,
                    isBookmarked = novel.isBookmarked,
                    prevNovelId = novelText?.seriesNavigation?.prevNovel?.id,
                    nextNovelId = novelText?.seriesNavigation?.nextNovel?.id,
                )
            }
        } catch (e: Exception) {
            updateState { copy(loading = false) }
            handleError(e)
            ToastUtil.safeShortToast("加载小说详情失败: ${e.message}")
        }
    }

    private fun extractNovelData(html: String): NovelTextResp? {
        // 正则解释：
        // novel:\s* 匹配 `novel: `
        // (\{.*?\}) 捕获组，非贪婪匹配花括号内的 JSON 内容
        // \s*,\s*isOwnWork: 匹配结尾，确保截取到 `isOwnWork:` 前的逗号
        // (?s) 即 DOT_MATCHES_ALL，让 . 能够匹配换行符
        val regex = """novel:\s*(\{.*?\})\s*,\s*isOwnWork:""".toRegex()

        val matchResult = regex.find(html)
        val novelJsonString = matchResult?.groupValues?.get(1)

        return if (!novelJsonString.isNullOrBlank()) {
            try {
                // 将截取到的 JSON 字符串反序列化为对象
                json.decodeFromString<NovelTextResp>(novelJsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
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

        // 这需要使用FileKit或平台特定API
        launchUI {
            val file = FileKit.openFileSaver(
                suggestedName = novel.title,
                extension = "txt"
            )
            if (file != null) {
                withIOContext {
                    file.writeString(text)
                }
            } else {
                ToastUtil.safeShortToast("文件保存已取消", type = ToastType.Info)
            }
        }
    }
}
