package com.mrl.pixiv.common.repository

import com.mrl.pixiv.common.ai.AiHttpStatusException
import com.mrl.pixiv.common.ai.AiLocalNetworkAccessGate
import com.mrl.pixiv.common.ai.AiLocalNetworkAccessState
import com.mrl.pixiv.common.ai.isReadyForAiRequest
import com.mrl.pixiv.common.ai.validateAiEndpoint
import com.mrl.pixiv.common.data.Novel
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.data.setting.BrowsingSettings
import com.mrl.pixiv.common.datasource.local.AccountDatabaseProvider
import com.mrl.pixiv.common.datasource.local.AccountHandle
import com.mrl.pixiv.common.datasource.local.dao.NovelReadLaterDao
import com.mrl.pixiv.common.datasource.local.entity.NovelReadLaterEntity
import com.mrl.pixiv.common.repository.util.hasDisallowedLongNovelTag
import com.mrl.pixiv.common.util.currentTimeMillis
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.sse.SSEClientException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import org.koin.core.annotation.Single

enum class NovelReadLaterState {
    PENDING,
    RUNNING,
    READY,
    FAILED,
}

data class NovelReadLaterItem(
    val novelId: Long,
    val targetLanguage: String,
    val novelTitle: String,
    val novelCaption: String,
    val novelAuthorName: String,
    val coverUrl: String,
    val novelTags: List<String>,
    val addedAtMillis: Long,
    val provider: AiProvider,
    val model: String,
    val configFingerprint: String,
    val sourceMd5: String,
    val state: NovelReadLaterState,
    val retryCount: Int,
    val lastError: String?,
)

data class NovelReadLaterKey(
    val userId: Long,
    val novelId: Long,
    val targetLanguage: String,
)

private data class ActiveTask(
    val key: NovelReadLaterKey,
    val attemptToken: String,
    val job: Job,
)

private data class AccountQueue(
    val userId: Long,
    val job: Job,
)

data class NovelReadLaterFailureTransition(
    val state: NovelReadLaterState,
    val retryCount: Int,
)

object NovelReadLaterQueuePolicy {
    const val MAX_AUTOMATIC_RETRIES = 3

    fun failureTransition(
        retryCount: Int,
        isTransient: Boolean,
    ): NovelReadLaterFailureTransition {
        return if (isTransient && retryCount < MAX_AUTOMATIC_RETRIES) {
            NovelReadLaterFailureTransition(
                state = NovelReadLaterState.PENDING,
                retryCount = retryCount + 1,
            )
        } else {
            NovelReadLaterFailureTransition(
                state = NovelReadLaterState.FAILED,
                retryCount = retryCount,
            )
        }
    }

    fun retryDelayMillis(retryCount: Int): Long {
        if (retryCount <= 0) return 0L
        return 1_000L * (1L shl (retryCount - 1).coerceAtMost(2))
    }

    fun canClaimEndpoint(
        endpoint: String,
        localNetworkAccessState: AiLocalNetworkAccessState,
    ): Boolean {
        val validation = validateAiEndpoint(endpoint)
        return validation.isValid &&
                (!validation.isLocalNetwork ||
                localNetworkAccessState == AiLocalNetworkAccessState.GRANTED
                )
    }

    fun firstClaimableEndpointIndex(
        endpoints: List<String>,
        localNetworkAccessState: AiLocalNetworkAccessState,
    ): Int? {
        return endpoints.indexOfFirst { endpoint ->
            canClaimEndpoint(endpoint, localNetworkAccessState)
        }.takeIf { it >= 0 }
    }

    fun needsLocalNetworkAccess(
        endpoints: List<String>,
        localNetworkAccessState: AiLocalNetworkAccessState,
    ): Boolean {
        return endpoints.any { endpoint ->
            val validation = validateAiEndpoint(endpoint)
            validation.isValid &&
                    validation.isLocalNetwork &&
                    !canClaimEndpoint(endpoint, localNetworkAccessState)
        }
    }

    fun isExactReadyCache(
        state: NovelReadLaterState,
        taskConfigFingerprint: String,
        currentConfigFingerprint: String,
        cacheConfigFingerprint: String,
        taskSourceMd5: String,
        currentSourceMd5: String,
        cacheSourceMd5: String,
    ): Boolean {
        return state == NovelReadLaterState.READY &&
                taskConfigFingerprint == currentConfigFingerprint &&
                cacheConfigFingerprint == currentConfigFingerprint &&
                taskSourceMd5.isNotBlank() &&
                taskSourceMd5 == currentSourceMd5 &&
                cacheSourceMd5 == currentSourceMd5
    }
}

@Single(createdAtStart = true)
class NovelReadLaterRepository(
    private val databases: AccountDatabaseProvider,
    private val translationRepository: NovelTranslationRepository,
    private val translationService: NovelAiTranslationService,
    private val source: NovelReadLaterSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskOwnershipMutex = Mutex()
    private var activeTask: ActiveTask? = null
    private var activeQueue: AccountQueue? = null

    suspend fun withQueueClaimsPaused(block: suspend () -> Unit) {
        taskOwnershipMutex.withLock { block() }
    }

    suspend fun cancelAccountWork(userId: Long) {
        val job = taskOwnershipMutex.withLock {
            check(databases.current.value?.userId != userId) { "The account queue is still active." }
            activeQueue?.takeIf { it.userId == userId }?.job
        }
        // The outer queue also restores the interrupted attempt before it finishes.
        job?.cancelAndJoin()
    }

    init {
        scope.launch {
            databases.current.collectLatest { account ->
                if (account == null) return@collectLatest
                val queue = AccountQueue(account.userId, currentCoroutineContext().job)
                taskOwnershipMutex.withLock {
                    if (databases.current.value !== account) return@collectLatest
                    activeQueue = queue
                }
                try {
                    val dao = account.db.novelReadLaterDao()
                    dao.restoreInterrupted(currentTimeMillis())
                    combine(
                        dao.observePendingCount(account.userId),
                        AiLocalNetworkAccessGate.state,
                    ) { pendingCount, _ -> pendingCount }
                        .collect { pendingCount ->
                            if (pendingCount > 0) drainQueue(account)
                        }
                } finally {
                    withContext(NonCancellable) {
                        taskOwnershipMutex.withLock { activeQueue = null }
                    }
                }
            }
        }
    }

    fun observeItems(): Flow<List<NovelReadLaterItem>> {
        return databases.current.flatMapLatest { account ->
            if (account != null) {
                filterNovelReadLaterEntries(
                    entries = account.db.novelReadLaterDao().observeByUserId(account.userId),
                    browsingSettings = requireUserPreferenceFlow
                        .map { it.browsingSettings }
                        .distinctUntilChanged(),
                ).map { entries -> entries.map { it.toDomain() } }
            } else {
                flowOf(emptyList())
            }
        }
    }

    fun observeItem(
        novelId: Long,
        targetLanguage: String,
    ): Flow<NovelReadLaterItem?> {
        return databases.current.flatMapLatest { account ->
            if (account != null) {
                account.db.novelReadLaterDao().observeByKey(
                    userId = account.userId,
                    novelId = novelId,
                    targetLanguage = targetLanguage,
                ).map { it?.toDomain() }
            } else {
                flowOf(null)
            }
        }
    }

    suspend fun enqueue(
        novel: Novel,
        targetLanguage: String,
        config: AiTranslationConfig = requireUserPreferenceValue.aiTranslationConfig,
    ) {
        val account = databases.requireCurrent()
        val normalizedConfig = config.normalizedForQueue()
        require(normalizedConfig.isReadyForQueue()) {
            "AI translation configuration is incomplete."
        }
        val now = currentTimeMillis()
        val entity = NovelReadLaterEntity(
            novelId = novel.id,
            userId = account.userId,
            targetLanguage = targetLanguage,
            novelTitle = novel.title,
            novelCaption = novel.caption,
            novelAuthorName = novel.user.name,
            coverUrl = novel.imageUrls.medium,
            novelTagsJson = encodeNovelReadLaterTags(novel.tags.map { it.name }),
            addedAtMillis = now,
            provider = normalizedConfig.provider.name,
            model = normalizedConfig.model,
            endpoint = normalizedConfig.endpoint,
            responseApi = normalizedConfig.responseApi,
            extraBody = normalizedConfig.extraBody,
            configFingerprint = buildNovelAiConfigFingerprint(normalizedConfig),
            sourceMd5 = "",
            state = NovelReadLaterState.PENDING.name,
            attemptToken = "",
            retryCount = 0,
            lastError = null,
            updatedAtMillis = now,
        )
        taskOwnershipMutex.withLock {
            account.db.novelReadLaterDao().insert(entity)
        }
    }

    suspend fun remove(
        novelId: Long,
        targetLanguage: String,
    ) {
        val account = databases.requireCurrent()
        val key = NovelReadLaterKey(
            userId = account.userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
        )
        val jobToCancel = taskOwnershipMutex.withLock {
            val ownedJob = activeTask
                ?.takeIf { it.key == key }
                ?.job
            ownedJob?.cancel()
            account.db.novelReadLaterDao().deleteByKey(
                userId = key.userId,
                novelId = key.novelId,
                targetLanguage = key.targetLanguage,
            )
            ownedJob
        }
        jobToCancel?.join()
    }

    suspend fun retry(
        novelId: Long,
        targetLanguage: String,
        config: AiTranslationConfig = requireUserPreferenceValue.aiTranslationConfig,
    ) {
        val normalizedConfig = config.normalizedForQueue()
        require(normalizedConfig.isReadyForQueue()) {
            "AI translation configuration is incomplete."
        }
        val account = databases.requireCurrent()
        taskOwnershipMutex.withLock {
            account.db.novelReadLaterDao().retry(
                userId = account.userId,
                novelId = novelId,
                targetLanguage = targetLanguage,
                provider = normalizedConfig.provider.name,
                model = normalizedConfig.model,
                endpoint = normalizedConfig.endpoint,
                responseApi = normalizedConfig.responseApi,
                extraBody = normalizedConfig.extraBody,
                configFingerprint = buildNovelAiConfigFingerprint(normalizedConfig),
                updatedAtMillis = currentTimeMillis(),
            )
        }
    }

    suspend fun getReadyTranslation(
        novelId: Long,
        targetLanguage: String,
        sourceText: String,
        config: AiTranslationConfig = requireUserPreferenceValue.aiTranslationConfig,
    ): NovelTranslationCache? {
        val account = databases.current.value ?: return null
        val dao = account.db.novelReadLaterDao()
        val userId = account.userId
        val normalizedConfig = config.normalizedForQueue()
        val item = dao.getByKey(
            userId = userId,
            novelId = novelId,
            targetLanguage = targetLanguage,
        ) ?: return null
        val state = runCatching { enumValueOf<NovelReadLaterState>(item.state) }
            .getOrDefault(NovelReadLaterState.FAILED)
        if (state != NovelReadLaterState.READY) return null
        val sourceMd5 = buildNovelTranslationSourceHash(
            sourceText = sourceText,
            extraBody = normalizedConfig.extraBody,
        )
        val currentConfigFingerprint = buildNovelAiConfigFingerprint(normalizedConfig)
        val cached = translationRepository.getTranslation(
            novelId = novelId,
            targetLanguage = targetLanguage,
        )
        val isExact = cached != null &&
                NovelReadLaterQueuePolicy.isExactReadyCache(
                    state = state,
                    taskConfigFingerprint = item.configFingerprint,
                    currentConfigFingerprint = currentConfigFingerprint,
                    cacheConfigFingerprint = cached.configFingerprint,
                    taskSourceMd5 = item.sourceMd5,
                    currentSourceMd5 = sourceMd5,
                    cacheSourceMd5 = cached.sourceMd5,
                ) &&
                cached.provider == normalizedConfig.provider &&
                cached.model == normalizedConfig.model &&
                cached.translatedText.isNotBlank()
        if (!isExact) {
            dao.invalidateReady(
                userId = userId,
                novelId = novelId,
                targetLanguage = targetLanguage,
                lastError = READY_CACHE_INVALID_ERROR,
                updatedAtMillis = currentTimeMillis(),
            )
            return null
        }
        return cached
    }

    private suspend fun drainQueue(account: AccountHandle) {
        val dao = account.db.novelReadLaterDao()
        while (databases.current.value === account) {
            val pending = dao.getPending(account.userId)
            if (pending.isEmpty()) return
            val accessState = AiLocalNetworkAccessGate.state.value
            val endpoints = pending.map { it.endpoint }
            val claimableIndex = NovelReadLaterQueuePolicy.firstClaimableEndpointIndex(
                endpoints = endpoints,
                localNetworkAccessState = accessState,
            )
            if (NovelReadLaterQueuePolicy.needsLocalNetworkAccess(
                    endpoints = endpoints,
                    localNetworkAccessState = accessState,
                )
            ) {
                AiLocalNetworkAccessGate.requestAccess()
            }
            val item = claimableIndex?.let(pending::get) ?: return
            val key = item.key()
            val attemptToken = Uuid.random().toHexString()
            coroutineScope {
                var ownedJob: Job? = null
                taskOwnershipMutex.withLock {
                    val claimedAt = currentTimeMillis()
                    if (dao.claimPending(
                            userId = item.userId,
                            novelId = item.novelId,
                            targetLanguage = item.targetLanguage,
                            attemptToken = attemptToken,
                            updatedAtMillis = claimedAt,
                        ) != 0
                    ) {
                        val claimedItem = item.copy(
                            state = NovelReadLaterState.RUNNING.name,
                            attemptToken = attemptToken,
                            updatedAtMillis = claimedAt,
                        )
                        ownedJob = launch(start = CoroutineStart.LAZY) {
                            if (claimedItem.retryCount > 0) {
                                delay(
                                    NovelReadLaterQueuePolicy.retryDelayMillis(claimedItem.retryCount).milliseconds,
                                )
                            }
                            if (databases.current.value !== account) {
                                throw CancellationException("The queue account is no longer active.")
                            }
                            processItem(dao, claimedItem)
                        }
                        activeTask = ActiveTask(
                            key = key,
                            attemptToken = attemptToken,
                            job = requireNotNull(ownedJob),
                        )
                    }
                }
                val job = ownedJob ?: return@coroutineScope
                job.start()
                try {
                    job.join()
                } finally {
                    withContext(NonCancellable) {
                        taskOwnershipMutex.withLock {
                            if (job.isCancelled) {
                                dao.restoreRunningAttempt(
                                    userId = key.userId,
                                    novelId = key.novelId,
                                    targetLanguage = key.targetLanguage,
                                    attemptToken = attemptToken,
                                    updatedAtMillis = currentTimeMillis(),
                                )
                            }
                            if (activeTask?.attemptToken == attemptToken) {
                                activeTask = null
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun processItem(dao: NovelReadLaterDao, item: NovelReadLaterEntity) {
        try {
            val currentConfig = requireUserPreferenceValue.aiTranslationConfig
            val config = item.toExecutionConfig(currentConfig)
            require(item.configFingerprint == buildNovelAiConfigFingerprint(config)) {
                "AI translation configuration changed. Retry the task to use the new configuration."
            }

            val sourceContent = withContext(RequestAccount(item.userId)) { source.load(item.novelId) }
            val sourceText = sourceContent.text.trim()
            val sourceMd5 = buildNovelTranslationSourceHash(
                sourceText = sourceText,
                extraBody = config.extraBody,
            )
            val metadataSourceMd5 = buildNovelTranslationMetadataSourceHash(
                title = sourceContent.novel.title,
                caption = sourceContent.novel.caption,
                extraBody = config.extraBody,
            )
            val cached = translationRepository.getTranslation(
                novelId = item.novelId,
                targetLanguage = item.targetLanguage,
            )
            val matchingConfiguration = cached != null &&
                    cached.provider == config.provider &&
                    cached.model == config.model &&
                    cached.configFingerprint == item.configFingerprint
            val bodyCacheHit = cached != null &&
                    matchingConfiguration &&
                    cached.sourceMd5 == sourceMd5 &&
                    cached.translatedText.isNotBlank()
            val metadataCacheHit = cached != null &&
                    matchingConfiguration &&
                    cached.metadataSourceMd5 == metadataSourceMd5 &&
                    cached.translatedTitle.isNotBlank()

            coroutineScope {
                val body = async {
                    if (bodyCacheHit) {
                        requireNotNull(cached).translatedText
                    } else {
                        translationService.translate(
                            text = sourceText,
                            targetLanguageTag = item.targetLanguage,
                            config = config,
                        ).also { translatedText ->
                            translationRepository.saveTranslation(
                                novelId = item.novelId,
                                targetLanguage = item.targetLanguage,
                                provider = config.provider,
                                model = config.model,
                                configFingerprint = item.configFingerprint,
                                sourceMd5 = sourceMd5,
                                translatedText = translatedText,
                            )
                        }
                    }
                }
                val metadata = async {
                    if (metadataCacheHit) {
                        val cachedMetadata = requireNotNull(cached)
                        NovelMetadataTranslation(
                            title = cachedMetadata.translatedTitle,
                            caption = cachedMetadata.translatedCaption,
                        )
                    } else {
                        translationService.translateMetadata(
                            title = sourceContent.novel.title,
                            caption = sourceContent.novel.caption,
                            targetLanguageTag = item.targetLanguage,
                            config = config,
                        ).also { translatedMetadata ->
                            translationRepository.saveMetadataTranslation(
                                novelId = item.novelId,
                                targetLanguage = item.targetLanguage,
                                provider = config.provider,
                                model = config.model,
                                configFingerprint = item.configFingerprint,
                                metadataSourceMd5 = metadataSourceMd5,
                                translatedTitle = translatedMetadata.title,
                                translatedCaption = translatedMetadata.caption,
                            )
                        }
                    }
                }
                body.await()
                metadata.await()
            }
            coroutineContext.ensureActive()
            dao.updateResult(
                userId = item.userId,
                novelId = item.novelId,
                targetLanguage = item.targetLanguage,
                attemptToken = item.attemptToken,
                state = NovelReadLaterState.READY.name,
                retryCount = item.retryCount,
                lastError = null,
                sourceMd5 = sourceMd5,
                updatedAtMillis = currentTimeMillis(),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            val transition = NovelReadLaterQueuePolicy.failureTransition(
                retryCount = item.retryCount,
                isTransient = throwable.isTransientQueueFailure(),
            )
            dao.updateResult(
                userId = item.userId,
                novelId = item.novelId,
                targetLanguage = item.targetLanguage,
                attemptToken = item.attemptToken,
                state = transition.state.name,
                retryCount = transition.retryCount,
                lastError = throwable.message?.take(MAX_ERROR_LENGTH)
                    ?: throwable::class.simpleName
                    ?: "Unknown error",
                sourceMd5 = item.sourceMd5,
                updatedAtMillis = currentTimeMillis(),
            )
        }
    }

    private companion object {
        const val MAX_ERROR_LENGTH = 500
        const val READY_CACHE_INVALID_ERROR =
            "Cached translation no longer matches the current configuration or source."
    }
}

fun buildNovelAiConfigFingerprint(config: AiTranslationConfig): String {
    val normalized = config.normalizedForQueue()
    return buildString {
        append(normalized.provider.name)
        append('\n')
        append(normalized.endpoint)
        append('\n')
        append(normalized.apiKey)
        append('\n')
        append(normalized.model)
        append('\n')
        append(normalized.responseApi)
        append('\n')
        append(normalized.extraBody)
    }.encodeToByteArray().toByteString().sha256().hex()
}

fun buildNovelTranslationSourceHash(
    sourceText: String,
    extraBody: String,
): String {
    val cacheInput = if (extraBody.isBlank()) {
        sourceText
    } else {
        "$sourceText\n\n[ai_extra_body]\n${extraBody.trim()}"
    }
    return cacheInput.encodeToByteArray().toByteString().md5().hex()
}

fun buildNovelTranslationMetadataSourceHash(
    title: String,
    caption: String,
    extraBody: String,
): String {
    val metadataSource = buildString {
        append(title.length)
        append(':')
        append(title)
        append(caption.length)
        append(':')
        append(caption)
    }
    return buildNovelTranslationSourceHash(
        sourceText = metadataSource,
        extraBody = extraBody,
    )
}

private fun AiTranslationConfig.normalizedForQueue(): AiTranslationConfig {
    return copy(
        endpoint = endpoint.trim().trimEnd('/'),
        apiKey = apiKey.trim(),
        model = model.trim(),
        extraBody = extraBody.trim(),
    )
}

private fun AiTranslationConfig.isReadyForQueue(): Boolean {
    return isReadyForAiRequest()
}

internal fun NovelReadLaterEntity.toExecutionConfig(
    currentConfig: AiTranslationConfig,
): AiTranslationConfig {
    return AiTranslationConfig(
        provider = runCatching { enumValueOf<AiProvider>(provider) }
            .getOrDefault(AiProvider.OPENAI),
        endpoint = endpoint,
        apiKey = currentConfig.apiKey.trim(),
        model = model,
        responseApi = responseApi,
        extraBody = extraBody,
        generationTimeoutSeconds = currentConfig.generationTimeoutSeconds,
        maxConcurrentRequests = currentConfig.maxConcurrentRequests,
    )
}

private fun NovelReadLaterEntity.key(): NovelReadLaterKey {
    return NovelReadLaterKey(
        userId = userId,
        novelId = novelId,
        targetLanguage = targetLanguage,
    )
}

private fun NovelReadLaterEntity.toDomain(): NovelReadLaterItem {
    return NovelReadLaterItem(
        novelId = novelId,
        targetLanguage = targetLanguage,
        novelTitle = novelTitle,
        novelCaption = novelCaption,
        novelAuthorName = novelAuthorName,
        coverUrl = coverUrl,
        novelTags = decodeNovelReadLaterTags(novelTagsJson),
        addedAtMillis = addedAtMillis,
        provider = runCatching { enumValueOf<AiProvider>(provider) }
            .getOrDefault(AiProvider.OPENAI),
        model = model,
        configFingerprint = configFingerprint,
        sourceMd5 = sourceMd5,
        state = runCatching { enumValueOf<NovelReadLaterState>(state) }
            .getOrDefault(NovelReadLaterState.FAILED),
        retryCount = retryCount,
        lastError = lastError,
    )
}

internal fun Throwable.isTransientQueueFailure(): Boolean {
    var current: Throwable? = this
    val visited = mutableSetOf<Throwable>()
    while (current != null && visited.add(current)) {
        if (current is IOException) return true
        val status = when (current) {
            is AiHttpStatusException -> current.statusCode
            is ResponseException -> current.response.status.value
            is SSEClientException -> current.response?.status?.value
            else -> null
        }
        if (status != null && (status == 408 || status == 429 || status in 500..599)) {
            return true
        }
        current = current.cause
    }
    return false
}

internal fun filterNovelReadLaterEntries(
    entries: Flow<List<NovelReadLaterEntity>>,
    browsingSettings: Flow<BrowsingSettings>,
): Flow<List<NovelReadLaterEntity>> {
    return combine(entries, browsingSettings) { queueEntries, settings ->
        queueEntries.filterNot { entry ->
            decodeNovelReadLaterTags(entry.novelTagsJson)
                .asSequence()
                .hasDisallowedLongNovelTag(settings)
        }
    }
}

internal fun encodeNovelReadLaterTags(tags: List<String>): String =
    Json.encodeToString(tags)

internal fun decodeNovelReadLaterTags(tagsJson: String): List<String> =
    runCatching { Json.decodeFromString<List<String>>(tagsJson) }
        .getOrDefault(emptyList())
