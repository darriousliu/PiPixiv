package com.mrl.pixiv.novel

import androidx.compose.runtime.Stable
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.novel.NovelTextResp
import com.mrl.pixiv.common.repository.NovelReadingProgress
import com.mrl.pixiv.common.repository.NovelReadingProgressRepository
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
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
    val restoreProgress: NovelReadingProgress? = null,
    val restoreVersion: Long = 0L,
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
    private val readingProgressRepository: NovelReadingProgressRepository
) : BaseMviViewModel<NovelState, NovelIntent>(
    initialState = NovelState()
), KoinComponent {
    private var latestProgress: NovelReadingProgress? = null

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
            updateState { copy(loading = true, restoreProgress = null) }
            val response = PixivRepository.getNovelDetail(novelId)
            val novel = response.novel

            val novelHtml = PixivRepository.getNovelContent(novelId)
            val novelText = extractNovelData(novelHtml)
            val text = novelText?.text.orEmpty()
            val paragraphs = text.split("\n").toImmutableList()

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

            requestRestoreProgress(novelId = novel.id, paragraphs = paragraphs)
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
        val currentBookmarkState = novel.isBookmark
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
        val currentNovelId = uiState.value.novel?.id ?: return
        requestRestoreProgress(novelId = currentNovelId, paragraphs = uiState.value.paragraphs)
    }

    private fun updateLineSpacing(spacing: Int) {
        updateState { copy(lineSpacingSp = spacing.coerceIn(-10, 10)) }
        val currentNovelId = uiState.value.novel?.id ?: return
        requestRestoreProgress(novelId = currentNovelId, paragraphs = uiState.value.paragraphs)
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

    fun saveProgress(novelId: Long, progress: NovelReadingProgress) {
        latestProgress = progress
        launchIO {
            readingProgressRepository.saveProgress(novelId, progress)
            Logger.d(tag = "NovelScreen") { "Saved progress for novel $progress" }
        }
    }

    private fun requestRestoreProgress(
        novelId: Long,
        paragraphs: List<String>
    ) {
        launchIO {
            if (paragraphs.isEmpty()) return@launchIO
            val saved = latestProgress ?: readingProgressRepository.getProgress(novelId) ?: return@launchIO
            val resolved = resolveProgress(saved, paragraphs)
            latestProgress = resolved
            updateState {
                copy(
                    restoreProgress = resolved,
                    restoreVersion = restoreVersion + 1
                )
            }
        }
    }

    private fun resolveProgress(
        saved: NovelReadingProgress,
        paragraphs: List<String>
    ): NovelReadingProgress {
        if (paragraphs.isEmpty()) return saved
        val directIndex = saved.paragraphIndex.coerceIn(0, paragraphs.lastIndex)
        if (paragraphs[directIndex].hashCode() == saved.paragraphHash) {
            return saved.copy(paragraphIndex = directIndex)
        }
        val fallbackIndex = paragraphs.indexOfFirst { it.hashCode() == saved.paragraphHash }
        val targetIndex = if (fallbackIndex >= 0) fallbackIndex else directIndex
        val targetLength = paragraphs[targetIndex].length
        return saved.copy(
            paragraphIndex = targetIndex,
            charIndex = saved.charIndex.coerceIn(0, targetLength)
        )
    }
}
