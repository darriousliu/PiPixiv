package com.mrl.pixiv.novel

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.ai.isReadyForAiRequest
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.coroutine.withIOContext
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.novel.NovelTextResp
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.BrowsingHistoryRepository
import com.mrl.pixiv.common.repository.NovelAiTranslationService
import com.mrl.pixiv.common.repository.NovelContentParser
import com.mrl.pixiv.common.repository.NovelMarkerChanges
import com.mrl.pixiv.common.repository.NovelReadLaterRepository
import com.mrl.pixiv.common.repository.NovelReadingProgress
import com.mrl.pixiv.common.repository.NovelReadingProgressRepository
import com.mrl.pixiv.common.repository.NovelTranslationStreamProgress
import com.mrl.pixiv.common.repository.NovelTranslationRepository
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.buildNovelAiConfigFingerprint
import com.mrl.pixiv.common.repository.buildNovelTranslationSourceHash
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
import com.mrl.pixiv.strings.novel_marker_add_success
import com.mrl.pixiv.strings.novel_marker_delete_success
import com.mrl.pixiv.strings.novel_marker_update_failed
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Stable
@ConsistentCopyVisibility
data class NovelState internal constructor(
    val loading: Boolean = true,
    val novel: Novel? = null,
    val novelTextResp: NovelTextResp? = null,
    val novelText: String = "",
    val fontSize: Int = 16,
    val lineSpacingSp: Int = 0,
    val isBookmarked: Boolean = false,
    val markerPage: Int? = null,
    val markerUpdating: Boolean = false,
    val showBottomSheet: Boolean = false,
    val paragraphs: ImmutableList<String> = persistentListOf(),
    val paragraphSpans: ImmutableList<NovelSpanData> = persistentListOf(),
    internal val translationPresentation: NovelTranslationPresentation =
        NovelTranslationPresentation.Idle,
    val prevNovelId: Long? = null,
    val nextNovelId: Long? = null,
    val restoreProgress: NovelReadingProgress? = null,
    val restoreVersion: Long = 0L,
    val isTranslated: Boolean = false,
    val isShowingOriginalText: Boolean = false,
) {
    val isTranslating: Boolean
        get() = translationPresentation !is NovelTranslationPresentation.Idle
}

@Stable
internal sealed interface NovelTranslationPresentation {
    data object Idle : NovelTranslationPresentation

    data object Waiting : NovelTranslationPresentation

    data class Streaming(
        val spans: ImmutableList<NovelSpanData>,
        val completedChunks: Int,
        val totalChunks: Int,
    ) : NovelTranslationPresentation
}

sealed class NovelIntent : ViewIntent {
    data class LoadNovelDetail(val novelId: Long) : NovelIntent()
    data object ToggleBookmark : NovelIntent()
    data class ToggleMarker(val page: Int) : NovelIntent()
    data class UpdateFontSize(val size: Int) : NovelIntent()
    data class UpdateLineSpacing(val spacing: Int) : NovelIntent()
    data object ToggleBottomSheet : NovelIntent()
    data object ShareNovel : NovelIntent()
    data object ExportToTxt : NovelIntent()
    data class NavigateToChapter(val novelId: Long) : NovelIntent()
    data class TranslateNovel(val forceRefresh: Boolean = false) : NovelIntent()
    data object CancelTranslation : NovelIntent()
    data object DeleteNovelTranslation : NovelIntent()
    data object ToggleDisplayOriginalText : NovelIntent()
    data class ApplyReadLaterTranslation(val targetLanguage: String) : NovelIntent()
}

@KoinViewModel
class NovelViewModel(
    novelId: Long,
    markerPage: Int,
    private val readingProgressRepository: NovelReadingProgressRepository,
    private val translationRepository: NovelTranslationRepository,
    private val aiTranslationService: NovelAiTranslationService,
    private val readLaterRepository: NovelReadLaterRepository,
    private val browsingHistoryRepository: BrowsingHistoryRepository,
) : BaseMviViewModel<NovelState, NovelIntent>(
    initialState = NovelState()
), KoinComponent {
    private var lastHistoryNovelId: Long? = null
    private val progressSession = NovelProgressSession()
    private var sourceNovelText: String = ""
    private var translatedNovelText: String = ""
    private val initialNovelId = novelId
    private var markerPageToRestore = markerPage.takeIf { it > 0 }
    private var translationJob: Job? = null
    private var translationRunToken: Any? = null
    private var translationNovelId: Long? = null
    private var translationRestoreProgress: NovelReadingProgress? = null

    init {
        dispatch(NovelIntent.LoadNovelDetail(novelId))
    }

    override suspend fun handleIntent(intent: NovelIntent) {
        when (intent) {
            is NovelIntent.LoadNovelDetail -> {
                cancelTranslation(restoreOriginal = false)
                loadNovelDetail(intent.novelId)
            }
            is NovelIntent.ToggleBookmark -> toggleBookmark()
            is NovelIntent.ToggleMarker -> toggleMarker(intent.page)
            is NovelIntent.UpdateFontSize -> updateFontSize(intent.size)
            is NovelIntent.UpdateLineSpacing -> updateLineSpacing(intent.spacing)
            is NovelIntent.ToggleBottomSheet -> toggleBottomSheet()
            is NovelIntent.ShareNovel -> shareNovel()
            is NovelIntent.ExportToTxt -> exportToTxt()
            is NovelIntent.NavigateToChapter -> {
                cancelTranslation(restoreOriginal = false)
                addHistory()
                loadNovelDetail(intent.novelId)
            }

            is NovelIntent.TranslateNovel -> translateNovel(intent.forceRefresh)
            is NovelIntent.CancelTranslation -> cancelTranslation()
            is NovelIntent.DeleteNovelTranslation -> deleteNovelTranslation()
            is NovelIntent.ToggleDisplayOriginalText -> toggleDisplayOriginalText()
            is NovelIntent.ApplyReadLaterTranslation ->
                applyReadLaterTranslation(intent.targetLanguage)
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
                        isTranslated = false,
                        isShowingOriginalText = false,
                        translationPresentation = NovelTranslationPresentation.Idle,
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
                    markerUpdating = false,
                    isTranslated = false,
                    isShowingOriginalText = false,
                    translationPresentation = NovelTranslationPresentation.Idle,
                )
            }
            val response = PixivRepository.getNovelDetail(novelId)
            val novel = response.novel

            val novelHtml = PixivRepository.getNovelContent(novelId)
            val novelText = NovelContentParser.extract(novelHtml)
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
                    markerPage = novelText?.marker?.page?.takeIf { it > 0 },
                    prevNovelId = novelText?.seriesNavigation?.prevNovel?.id,
                    nextNovelId = novelText?.seriesNavigation?.nextNovel?.id,
                    isTranslated = false,
                    isShowingOriginalText = false,
                    translationPresentation = NovelTranslationPresentation.Idle,
                )
            }

            val initialMarkerPage = initialMarkerPageForNovel(
                initialNovelId = initialNovelId,
                loadedNovelId = novel.id,
                requestedMarkerPage = markerPageToRestore,
            )
            markerPageToRestore = null
            if (initialMarkerPage != null) {
                requestMarkerRestoreProgress(
                    novelId = novel.id,
                    markerPage = initialMarkerPage,
                    spans = spans,
                    paragraphs = paragraphs,
                )
            } else {
                requestRestoreProgress(novelId = novel.id, paragraphs = paragraphs)
            }
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

    private fun toggleMarker(page: Int) {
        val novel = uiState.value.novel ?: return
        if (uiState.value.markerUpdating || uiState.value.isTranslating) return

        val currentMarkerPage = uiState.value.markerPage
        launchIO(
            onError = { throwable ->
                updateStateForNovel(novel.id) {
                    copy(markerUpdating = false)
                }
                handleError(throwable)
                ToastUtil.safeShortToast(
                    RStrings.novel_marker_update_failed,
                    throwable.message.orEmpty(),
                )
            }
        ) {
            updateStateForNovel(novel.id) {
                copy(markerUpdating = true)
            }
            when (val mutation = resolveNovelMarkerMutation(currentMarkerPage, page)) {
                NovelMarkerMutation.Delete -> {
                    PixivRepository.postNovelMarkerDelete(novel.id)
                    NovelMarkerChanges.notifyChanged(novel.id)
                    updateStateForNovel(novel.id) {
                        copy(
                            markerPage = null,
                            markerUpdating = false,
                        )
                    }
                    ToastUtil.safeShortToast(RStrings.novel_marker_delete_success)
                }

                is NovelMarkerMutation.Save -> {
                    PixivRepository.postNovelMarkerAdd(novel.id, mutation.page)
                    NovelMarkerChanges.notifyChanged(novel.id)
                    updateStateForNovel(novel.id) {
                        copy(
                            markerPage = mutation.page,
                            markerUpdating = false,
                        )
                    }
                    ToastUtil.safeShortToast(RStrings.novel_marker_add_success)
                }
            }
        }
    }

    private fun updateStateForNovel(
        novelId: Long,
        transform: NovelState.() -> NovelState,
    ) {
        updateState {
            if (shouldApplyNovelMarkerUpdate(novel?.id, novelId)) {
                transform()
            } else {
                this
            }
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
            val novel = uiState.value.novel ?: return@launchProcess
            val novelId = novel.id
            if (lastHistoryNovelId == novelId) return@launchProcess
            browsingHistoryRepository.recordNovel(novel)
            lastHistoryNovelId = novelId
        }
    }

    private fun updateFontSize(size: Int) {
        updateState { copy(fontSize = size.coerceIn(10, 32)) }
        if (uiState.value.isTranslating) return
        val currentNovelId = uiState.value.novel?.id ?: return
        requestRestoreProgress(novelId = currentNovelId, paragraphs = uiState.value.paragraphs)
    }

    private fun updateLineSpacing(spacing: Int) {
        updateState { copy(lineSpacingSp = spacing.coerceIn(-10, 10)) }
        if (uiState.value.isTranslating) return
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
                defaultExtension = "txt",
            )
            if (file != null) {
                withIOContext {
                    file.writeString(text)
                }
            }
        }
    }

    private fun translateNovel(forceRefresh: Boolean = false) {
        if (translationJob != null || uiState.value.isTranslating) return

        val novel = uiState.value.novel ?: return
        val sourceText = sourceNovelText.trim().ifBlank { uiState.value.novelText.trim() }
        if (sourceText.isBlank()) return

        val config = requireUserPreferenceValue.aiTranslationConfig.normalized()
        if (!config.isReady()) {
            ToastUtil.safeShortToast(RStrings.ai_translation_config_required)
            return
        }

        val sourceMd5 = buildNovelTranslationSourceHash(
            sourceText = sourceText,
            extraBody = config.extraBody,
        )
        val configFingerprint = buildNovelAiConfigFingerprint(config)
        val targetLanguageTag = resolveTargetLanguageTag()
        val runToken = Any()
        val progressBeforeTranslation =
            progressSession.get(novel.id) ?: uiState.value.restoreProgress
        translationRunToken = runToken
        translationNovelId = novel.id
        translationRestoreProgress = progressBeforeTranslation

        val job = viewModelScope.launch(
            context = Dispatchers.IO,
            start = CoroutineStart.LAZY,
        ) {
            try {
                updateState {
                    withTranslationWaiting()
                }

                val cached = if (!forceRefresh) {
                    translationRepository.getTranslation(
                        novelId = novel.id,
                        targetLanguage = targetLanguageTag,
                    )
                } else {
                    null
                }

                var renderedStreamingBody = false
                val translatedText = if (
                    cached != null &&
                    cached.provider == config.provider &&
                    cached.model == config.model &&
                    cached.configFingerprint == configFingerprint &&
                    cached.sourceMd5 == sourceMd5 &&
                    cached.translatedText.isNotBlank()
                ) {
                    ToastUtil.safeShortToast(RStrings.ai_translation_cache_hit)
                    cached.translatedText
                } else {
                    var completedText: String? = null
                    var renderedFirstDelta = false
                    var lastRenderedCompletedChunks = 0
                    var lastRenderMark = TimeSource.Monotonic.markNow()

                    aiTranslationService.translateStreaming(
                        text = sourceText,
                        targetLanguageTag = targetLanguageTag,
                        config = config,
                    ).collect { progress ->
                        if (progress.text.isNotEmpty()) {
                            val shouldRender = shouldRenderNovelTranslation(
                                progress = progress,
                                renderedFirstDelta = renderedFirstDelta,
                                lastRenderedCompletedChunks = lastRenderedCompletedChunks,
                                renderIntervalElapsed =
                                    lastRenderMark.elapsedNow() >= STREAM_RENDER_INTERVAL,
                            )
                            if (shouldRender && isCurrentTranslation(runToken, novel.id)) {
                                renderStreamingTranslation(progress)
                                renderedStreamingBody = true
                                renderedFirstDelta = true
                                lastRenderedCompletedChunks = progress.completedChunks
                                lastRenderMark = TimeSource.Monotonic.markNow()
                            }
                        }
                        if (progress.isComplete) {
                            completedText = progress.text
                        }
                    }

                    val completed = commitCompletedNovelTranslation(
                        completedText = completedText,
                        isCurrent = isCurrentTranslation(runToken, novel.id),
                    ) { translatedText ->
                        translationRepository.saveTranslation(
                            novelId = novel.id,
                            targetLanguage = targetLanguageTag,
                            provider = config.provider,
                            model = config.model,
                            configFingerprint = configFingerprint,
                            sourceMd5 = sourceMd5,
                            translatedText = translatedText,
                        )
                    }
                    ToastUtil.safeShortToast(RStrings.ai_translation_success)
                    completed
                }

                if (isCurrentTranslation(runToken, novel.id)) {
                    val translatedParagraphs = completeTranslation(translatedText)
                    if (shouldRestoreProgressAfterTranslation(renderedStreamingBody)) {
                        requestRestoreProgress(
                            novelId = novel.id,
                            paragraphs = translatedParagraphs,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                if (isCurrentTranslation(runToken, novel.id)) {
                    restoreOriginalAfterTranslation(
                        novelId = novel.id,
                        sourceText = sourceText,
                        progressToRestore = progressBeforeTranslation,
                    )
                }
                throw cancelled
            } catch (throwable: Throwable) {
                if (isCurrentTranslation(runToken, novel.id)) {
                    restoreOriginalAfterTranslation(
                        novelId = novel.id,
                        sourceText = sourceText,
                        progressToRestore = progressBeforeTranslation,
                    )
                    handleError(throwable)
                    ToastUtil.safeShortToast(
                        RStrings.ai_translation_failed,
                        throwable.message.orEmpty()
                    )
                }
            } finally {
                if (translationRunToken === runToken) {
                    translationJob = null
                    translationRunToken = null
                    translationNovelId = null
                    translationRestoreProgress = null
                }
            }
        }
        translationJob = job
        job.start()
    }

    private suspend fun cancelTranslation(restoreOriginal: Boolean = true) {
        val job = translationJob ?: return
        val novelId = translationNovelId
        val progressToRestore = translationRestoreProgress
        translationJob = null
        translationRunToken = null
        translationNovelId = null
        translationRestoreProgress = null
        job.cancelAndJoin()
        if (!restoreOriginal) return
        novelId ?: return
        val sourceText = sourceNovelText.trim().ifBlank { uiState.value.novelText.trim() }
        restoreOriginalAfterTranslation(
            novelId = novelId,
            sourceText = sourceText,
            progressToRestore = progressToRestore,
        )
    }

    private fun isCurrentTranslation(runToken: Any, novelId: Long): Boolean =
        translationRunToken === runToken && uiState.value.novel?.id == novelId

    private fun renderStreamingTranslation(progress: NovelTranslationStreamProgress) {
        val spans = buildNovelTranslationSpans(
            text = progress.text,
            novelTextResp = uiState.value.novelTextResp,
        )
        updateState {
            withStreamingTranslation(
                translatedSpans = spans,
                completedChunks = progress.completedChunks,
                totalChunks = progress.totalChunks,
            )
        }
    }

    private fun completeTranslation(text: String): ImmutableList<String> {
        val spans = buildNovelTranslationSpans(
            text = text,
            novelTextResp = uiState.value.novelTextResp,
        )
        val paragraphs = spans.toProgressParagraphs()
        translatedNovelText = text
        updateState {
            withCompletedTranslation(
                translatedText = text,
                translatedParagraphs = paragraphs,
                translatedSpans = spans,
            )
        }
        return paragraphs
    }

    private fun restoreOriginalAfterTranslation(
        novelId: Long,
        sourceText: String,
        progressToRestore: NovelReadingProgress?,
    ) {
        if (uiState.value.novel?.id != novelId) return
        val spans = NovelSpanParser
            .buildSpans(sourceText, uiState.value.novelTextResp)
            .toImmutableList()
        val paragraphs = spans.toProgressParagraphs()
        if (progressToRestore != null) {
            progressSession.update(novelId, progressToRestore)
        }
        translatedNovelText = ""
        updateState {
            copy(
                novelText = sourceText,
                paragraphs = paragraphs,
                paragraphSpans = spans,
                isTranslated = false,
                isShowingOriginalText = false,
                translationPresentation = NovelTranslationPresentation.Idle,
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

    private fun applyReadLaterTranslation(targetLanguage: String) {
        val novel = uiState.value.novel ?: return
        val sourceText = sourceNovelText.trim().ifBlank { uiState.value.novelText.trim() }
        if (sourceText.isBlank()) return

        launchIO {
            val cached = readLaterRepository.getReadyTranslation(
                novelId = novel.id,
                targetLanguage = targetLanguage,
                sourceText = sourceText,
            ) ?: return@launchIO
            if (uiState.value.novel?.id != novel.id) return@launchIO

            val translatedSpans = NovelSpanParser
                .buildSpans(cached.translatedText, uiState.value.novelTextResp)
                .toImmutableList()
            val translatedParagraphs = translatedSpans.toProgressParagraphs()
            translatedNovelText = cached.translatedText
            updateState {
                copy(
                    novelText = cached.translatedText,
                    paragraphs = translatedParagraphs,
                    paragraphSpans = translatedSpans,
                    isTranslated = true,
                    isShowingOriginalText = false,
                )
            }
            requestRestoreProgress(
                novelId = novel.id,
                paragraphs = translatedParagraphs,
            )
            ToastUtil.safeShortToast(RStrings.ai_translation_cache_hit)
        }
    }

    fun saveProgress(novelId: Long, progress: NovelReadingProgress) {
        progressSession.update(novelId, progress)
        launchIO {
            readingProgressRepository.saveProgress(novelId, progress)
            Logger.d(tag = "NovelScreen") { "Saved progress for novel $progress" }
        }
    }

    fun clearProgress(novelId: Long) {
        progressSession.clear(novelId)
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
                progressSession.get(novelId)
                    ?: readingProgressRepository.getProgress(novelId)
                    ?: return@launchIO
            val resolved = resolveProgress(saved, paragraphs)
            progressSession.update(novelId, resolved)
            updateState {
                if (!shouldApplyNovelScopedUpdate(novel?.id, novelId)) {
                    this
                } else {
                    copy(
                        restoreProgress = resolved,
                        restoreVersion = restoreVersion + 1
                    )
                }
            }
        }
    }

    private fun requestMarkerRestoreProgress(
        novelId: Long,
        markerPage: Int,
        spans: List<NovelSpanData>,
        paragraphs: List<String>,
    ) {
        if (paragraphs.isEmpty()) return
        val paragraphIndex = paragraphIndexForMarkerPage(
            spans = spans,
            markerPage = markerPage,
        ).coerceIn(0, paragraphs.lastIndex)
        val markerProgress = NovelReadingProgress(
            paragraphIndex = paragraphIndex,
            charIndex = 0,
            paragraphHash = paragraphs[paragraphIndex].hashCode(),
        )
        progressSession.update(novelId, markerProgress)
        updateState {
            copy(
                restoreProgress = markerProgress,
                restoreVersion = restoreVersion + 1,
            )
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

private val STREAM_RENDER_INTERVAL = 50.milliseconds

internal fun shouldRenderNovelTranslation(
    progress: NovelTranslationStreamProgress,
    renderedFirstDelta: Boolean,
    lastRenderedCompletedChunks: Int,
    renderIntervalElapsed: Boolean,
): Boolean = !renderedFirstDelta ||
        progress.isComplete ||
        progress.completedChunks > lastRenderedCompletedChunks ||
        renderIntervalElapsed

internal fun shouldApplyNovelScopedUpdate(
    currentNovelId: Long?,
    requestedNovelId: Long,
): Boolean = currentNovelId == requestedNovelId

internal fun shouldRestoreProgressAfterTranslation(
    renderedStreamingBody: Boolean,
): Boolean = !renderedStreamingBody

internal fun NovelState.withTranslationWaiting(): NovelState = copy(
    translationPresentation = NovelTranslationPresentation.Waiting,
    isTranslated = false,
    isShowingOriginalText = false,
)

internal fun NovelState.withStreamingTranslation(
    translatedSpans: ImmutableList<NovelSpanData>,
    completedChunks: Int,
    totalChunks: Int,
): NovelState = copy(
    translationPresentation = NovelTranslationPresentation.Streaming(
        spans = translatedSpans,
        completedChunks = completedChunks,
        totalChunks = totalChunks,
    ),
    isTranslated = false,
    isShowingOriginalText = false,
)

internal fun NovelState.withCompletedTranslation(
    translatedText: String,
    translatedParagraphs: ImmutableList<String>,
    translatedSpans: ImmutableList<NovelSpanData>,
): NovelState = copy(
    novelText = translatedText,
    paragraphs = translatedParagraphs,
    paragraphSpans = translatedSpans,
    translationPresentation = NovelTranslationPresentation.Idle,
    isTranslated = true,
    isShowingOriginalText = false,
)

internal fun buildNovelTranslationSpans(
    text: String,
    novelTextResp: NovelTextResp?,
): ImmutableList<NovelSpanData> = NovelSpanParser
    .buildSpans(text, novelTextResp)
    .toImmutableList()

internal suspend fun commitCompletedNovelTranslation(
    completedText: String?,
    isCurrent: Boolean,
    saveTranslation: suspend (String) -> Unit,
): String {
    val completed = requireNotNull(completedText) {
        "AI translation stream ended before completion."
    }
    currentCoroutineContext().ensureActive()
    check(isCurrent) {
        "Translation run is no longer active."
    }
    saveTranslation(completed)
    return completed
}

private fun AiTranslationConfig.normalized(): AiTranslationConfig {
    return copy(
        endpoint = endpoint.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
        extraBody = extraBody.trim(),
    )
}

private fun AiTranslationConfig.isReady(): Boolean {
    return isReadyForAiRequest()
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
