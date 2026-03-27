package com.mrl.pixiv.novel

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.intl.Locale
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.novel.NovelTextResp
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.NovelAiTranslationService
import com.mrl.pixiv.common.repository.NovelReadingProgress
import com.mrl.pixiv.common.repository.NovelReadingProgressRepository
import com.mrl.pixiv.common.repository.NovelTranslationRepository
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.repository.viewmodel.bookmark.isBookmark
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.strings.ai_translation_cache_hit
import com.mrl.pixiv.strings.ai_translation_config_required
import com.mrl.pixiv.strings.ai_translation_deleted
import com.mrl.pixiv.strings.ai_translation_failed
import com.mrl.pixiv.strings.ai_translation_success
import com.mrl.pixiv.strings.load_failed
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent

@Stable
data class NovelState(
    val loading: Boolean = true,
    val novel: Novel? = null,
    val novelTextResp: NovelTextResp? = null,
    val novelText: String = "",
    val fontSize: Int = 16,
    val lineSpacingSp: Int = 0,
    val isBookmarked: Boolean = false,
    val showBottomSheet: Boolean = false,
    val paragraphs: ImmutableList<String> = persistentListOf(),
    val paragraphSpans: ImmutableList<NovelSpanData> = persistentListOf(),
    val prevNovelId: Long? = null,
    val nextNovelId: Long? = null,
    val restoreProgress: NovelReadingProgress? = null,
    val restoreVersion: Long = 0L,
    val isTranslating: Boolean = false,
    val isTranslated: Boolean = false,
    val isShowingOriginalText: Boolean = false,
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
    data class TranslateNovel(val forceRefresh: Boolean = false) : NovelIntent()
    data object DeleteNovelTranslation : NovelIntent()
    data object ToggleDisplayOriginalText : NovelIntent()
}

@KoinViewModel
class NovelViewModel(
    novelId: Long,
    private val readingProgressRepository: NovelReadingProgressRepository,
    private val translationRepository: NovelTranslationRepository,
    private val aiTranslationService: NovelAiTranslationService,
) : BaseMviViewModel<NovelState, NovelIntent>(
    initialState = NovelState()
), KoinComponent {
    private var lastHistoryNovelId: Long? = null
    private var latestProgress: NovelReadingProgress? = null
    private var sourceNovelText: String = ""
    private var translatedNovelText: String = ""

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
            is NovelIntent.NavigateToChapter -> {
                addHistory()
                loadNovelDetail(intent.novelId)
            }
            is NovelIntent.TranslateNovel -> translateNovel(intent.forceRefresh)
            is NovelIntent.DeleteNovelTranslation -> deleteNovelTranslation()
            is NovelIntent.ToggleDisplayOriginalText -> toggleDisplayOriginalText()
        }
    }

    private fun loadNovelDetail(novelId: Long) {
        launchIO(
            onError = { e ->
                sourceNovelText = ""
                translatedNovelText = ""
                updateState {
                    copy(
                        loading = false,
                        isTranslating = false,
                        isTranslated = false,
                        isShowingOriginalText = false,
                    )
                }
                handleError(e)
                ToastUtil.safeShortToast(RStrings.load_failed, e.message)
            }
        ) {
            updateState {
                copy(
                    loading = true,
                    restoreProgress = null,
                    isTranslating = false,
                    isTranslated = false,
                    isShowingOriginalText = false,
                )
            }
            val response = PixivRepository.getNovelDetail(novelId)
            val novel = response.novel

            val novelHtml = PixivRepository.getNovelContent(novelId)
            val novelText = extractNovelData(novelHtml)
            val text = novelText?.text.orEmpty()
            sourceNovelText = text
            translatedNovelText = ""
            val spans = NovelSpanParser.buildSpans(text, novelText).toImmutableList()
            val paragraphs = spans.toProgressParagraphs()

            updateState {
                copy(
                    loading = false,
                    novel = novel,
                    novelTextResp = novelText,
                    novelText = text,
                    paragraphs = paragraphs,
                    paragraphSpans = spans,
                    isBookmarked = novel.isBookmarked,
                    prevNovelId = novelText?.seriesNavigation?.prevNovel?.id,
                    nextNovelId = novelText?.seriesNavigation?.nextNovel?.id,
                    isTranslating = false,
                    isTranslated = false,
                    isShowingOriginalText = false,
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

    fun blockNovel() {
        val novel = uiState.value.novel ?: return
        BlockingRepositoryV2.blockNovel(novelId = novel.id, title = novel.title)
    }

    fun removeBlockNovel() {
        val novel = uiState.value.novel ?: return
        BlockingRepositoryV2.removeBlockNovel(novel.id)
    }

    fun addHistory() {
        launchProcess(Dispatchers.IO) {
            val novelId = uiState.value.novel?.id ?: return@launchProcess
            if (lastHistoryNovelId == novelId) return@launchProcess
            PixivRepository.addNovelBrowsingHistory(novelId)
            lastHistoryNovelId = novelId
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

    private fun translateNovel(forceRefresh: Boolean = false) {
        if (uiState.value.isTranslating) return

        val novel = uiState.value.novel ?: return
        val sourceText = sourceNovelText.trim().ifBlank { uiState.value.novelText.trim() }
        if (sourceText.isBlank()) return

        val config = requireUserPreferenceValue.aiTranslationConfig.normalized()
        if (!config.isReady()) {
            ToastUtil.safeShortToast(RStrings.ai_translation_config_required)
            return
        }

        val sourceMd5 = sourceText.toMd5Hex()
        val targetLanguageTag = resolveTargetLanguageTag()

        launchIO(
            onError = { throwable ->
                updateState { copy(isTranslating = false) }
                handleError(throwable)
                ToastUtil.safeShortToast(
                    RStrings.ai_translation_failed,
                    throwable.message.orEmpty()
                )
            }
        ) {
            updateState { copy(isTranslating = true) }

            val cached = if (!forceRefresh) {
                translationRepository.getTranslation(
                    novelId = novel.id,
                    targetLanguage = targetLanguageTag,
                )
            } else {
                null
            }

            val translatedText = if (
                cached != null &&
                cached.provider == config.provider &&
                cached.model == config.model &&
                cached.sourceMd5 == sourceMd5 &&
                cached.translatedText.isNotBlank()
            ) {
                ToastUtil.safeShortToast(RStrings.ai_translation_cache_hit)
                cached.translatedText
            } else {
                val translated = aiTranslationService.translate(
                    text = sourceText,
                    targetLanguageTag = targetLanguageTag,
                    config = config,
                )
                translationRepository.saveTranslation(
                    novelId = novel.id,
                    targetLanguage = targetLanguageTag,
                    provider = config.provider,
                    model = config.model,
                    sourceMd5 = sourceMd5,
                    translatedText = translated,
                )
                ToastUtil.safeShortToast(RStrings.ai_translation_success)
                translated
            }

            val translatedSpans = NovelSpanParser
                .buildSpans(translatedText, uiState.value.novelTextResp)
                .toImmutableList()
            val translatedParagraphs = translatedSpans.toProgressParagraphs()
            translatedNovelText = translatedText
            updateState {
                copy(
                    novelText = translatedText,
                    paragraphs = translatedParagraphs,
                    paragraphSpans = translatedSpans,
                    isTranslating = false,
                    isTranslated = true,
                    isShowingOriginalText = false,
                )
            }

            requestRestoreProgress(
                novelId = novel.id,
                paragraphs = translatedParagraphs,
            )
        }
    }

    private fun deleteNovelTranslation() {
        if (uiState.value.isTranslating) return

        val novel = uiState.value.novel ?: return
        val sourceText = sourceNovelText.trim().ifBlank { uiState.value.novelText.trim() }
        if (sourceText.isBlank()) return

        val targetLanguageTag = resolveTargetLanguageTag()

        launchIO(
            onError = { throwable ->
                handleError(throwable)
                ToastUtil.safeShortToast(
                    RStrings.ai_translation_failed,
                    throwable.message.orEmpty()
                )
            }
        ) {
            translationRepository.deleteTranslation(
                novelId = novel.id,
                targetLanguage = targetLanguageTag,
            )

            val sourceSpans = NovelSpanParser
                .buildSpans(sourceText, uiState.value.novelTextResp)
                .toImmutableList()
            val sourceParagraphs = sourceSpans.toProgressParagraphs()
            translatedNovelText = ""
            updateState {
                copy(
                    novelText = sourceText,
                    paragraphs = sourceParagraphs,
                    paragraphSpans = sourceSpans,
                    isTranslated = false,
                    isShowingOriginalText = false,
                )
            }

            requestRestoreProgress(
                novelId = novel.id,
                paragraphs = sourceParagraphs,
            )
            ToastUtil.safeShortToast(RStrings.ai_translation_deleted)
        }
    }

    private fun toggleDisplayOriginalText() {
        if (uiState.value.isTranslating || !uiState.value.isTranslated) return

        val novel = uiState.value.novel ?: return
        val sourceText = sourceNovelText.trim().ifBlank { uiState.value.novelText.trim() }
        val translatedText = translatedNovelText.trim()
        if (sourceText.isBlank() || translatedText.isBlank()) return

        val shouldShowOriginal = !uiState.value.isShowingOriginalText
        val targetText = if (shouldShowOriginal) sourceText else translatedText
        val targetSpans = NovelSpanParser
            .buildSpans(targetText, uiState.value.novelTextResp)
            .toImmutableList()
        val targetParagraphs = targetSpans.toProgressParagraphs()

        updateState {
            copy(
                novelText = targetText,
                paragraphs = targetParagraphs,
                paragraphSpans = targetSpans,
                isShowingOriginalText = shouldShowOriginal,
            )
        }

        requestRestoreProgress(
            novelId = novel.id,
            paragraphs = targetParagraphs,
        )
    }

    fun saveProgress(novelId: Long, progress: NovelReadingProgress) {
        latestProgress = progress
        launchIO {
            readingProgressRepository.saveProgress(novelId, progress)
            Logger.d(tag = "NovelScreen") { "Saved progress for novel $progress" }
        }
    }

    fun clearProgress(novelId: Long) {
        latestProgress = null
        updateState { copy(restoreProgress = null) }
        launchIO {
            readingProgressRepository.clearProgress(novelId)
            Logger.d(tag = "NovelScreen") { "Cleared progress for novelId=$novelId" }
        }
    }

    private fun requestRestoreProgress(
        novelId: Long,
        paragraphs: List<String>
    ) {
        launchIO {
            if (paragraphs.isEmpty()) return@launchIO
            val saved =
                latestProgress ?: readingProgressRepository.getProgress(novelId) ?: return@launchIO
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

    private fun resolveTargetLanguageTag(): String {
        return requireUserPreferenceValue.appLanguage
            ?.takeIf { it.isNotBlank() }
            ?: Locale.current.toLanguageTag().ifBlank { "en" }
    }
}

private fun AiTranslationConfig.normalized(): AiTranslationConfig {
    return copy(
        endpoint = endpoint.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
    )
}

private fun AiTranslationConfig.isReady(): Boolean {
    return endpoint.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}

private fun String.toMd5Hex(): String {
    return encodeToByteArray().toByteString().md5().hex()
}

private fun List<NovelSpanData>.toProgressParagraphs(): ImmutableList<String> {
    if (isEmpty()) return persistentListOf("\u200B")
    return map { span ->
        when (span) {
            is NovelSpanData.Text -> span.value
            is NovelSpanData.JumpUri -> span.value
            is NovelSpanData.PixivImage -> " "
            is NovelSpanData.UploadedImage -> " "
            NovelSpanData.NewPage -> "\n"
        }
    }.toImmutableList()
}
