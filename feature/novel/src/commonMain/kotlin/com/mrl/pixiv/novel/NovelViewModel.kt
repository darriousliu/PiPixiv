package com.mrl.pixiv.novel

import androidx.compose.runtime.Stable
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.novel.NovelTextResp
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.strings.load_failed
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
    novelId: Long,
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

    private fun loadNovelDetail(novelId: Long) {
        launchIO(
            onError = { e ->
                updateState { copy(loading = false) }
                handleError(e)
                ToastUtil.safeShortToast(RStrings.load_failed, e.message)
            }
        ) {
            updateState { copy(loading = true) }
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


    private fun toggleBookmark() {
        val novel = uiState.value.novel ?: return
        val currentBookmarkState = uiState.value.isBookmarked
        if (currentBookmarkState) {

            BookmarkState.deleteBookmarkNovel(novel.id)
        } else {
            val privateBookmark = requireUserPreferenceValue.defaultPrivateBookmark
            BookmarkState.bookmarkNovel(
                novel.id,
                if (privateBookmark) Restrict.PRIVATE else Restrict.PUBLIC
            )
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
            }
        }
    }
}
