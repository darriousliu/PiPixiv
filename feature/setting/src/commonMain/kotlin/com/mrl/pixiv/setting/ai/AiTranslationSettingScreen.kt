package com.mrl.pixiv.setting.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.ai.AiEndpointError
import com.mrl.pixiv.common.ai.AiLocalNetworkAccessGate
import com.mrl.pixiv.common.ai.AiModelCatalogService
import com.mrl.pixiv.common.ai.validateAiEndpoint
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.setting.components.DropDownSelector
import com.mrl.pixiv.strings.ai_api_key
import com.mrl.pixiv.strings.ai_endpoint
import com.mrl.pixiv.strings.ai_endpoint_credentials_not_allowed
import com.mrl.pixiv.strings.ai_endpoint_invalid
import com.mrl.pixiv.strings.ai_endpoint_public_http_not_allowed
import com.mrl.pixiv.strings.ai_extra_body
import com.mrl.pixiv.strings.ai_extra_body_hint
import com.mrl.pixiv.strings.ai_extra_body_invalid
import com.mrl.pixiv.strings.ai_extra_body_presets
import com.mrl.pixiv.strings.ai_extra_preset_claude_adaptive_thinking
import com.mrl.pixiv.strings.ai_extra_preset_claude_effort
import com.mrl.pixiv.strings.ai_extra_preset_deepseek_disable_thinking
import com.mrl.pixiv.strings.ai_extra_preset_deepseek_reasoning_max
import com.mrl.pixiv.strings.ai_extra_preset_gemini_disable_thinking
import com.mrl.pixiv.strings.ai_extra_preset_gemini_dynamic_thinking
import com.mrl.pixiv.strings.ai_extra_preset_gemini_thinking_budget
import com.mrl.pixiv.strings.ai_extra_preset_reasoning_effort
import com.mrl.pixiv.strings.ai_extra_preset_temperature
import com.mrl.pixiv.strings.ai_extra_preset_top_p
import com.mrl.pixiv.strings.ai_generation_timeout_desc
import com.mrl.pixiv.strings.ai_generation_timeout_invalid
import com.mrl.pixiv.strings.ai_generation_timeout_seconds
import com.mrl.pixiv.strings.ai_max_concurrent_requests
import com.mrl.pixiv.strings.ai_max_concurrent_requests_desc
import com.mrl.pixiv.strings.ai_max_concurrent_requests_invalid
import com.mrl.pixiv.strings.ai_model
import com.mrl.pixiv.strings.ai_model_suggestions
import com.mrl.pixiv.strings.ai_openai_use_response_api
import com.mrl.pixiv.strings.ai_provider
import com.mrl.pixiv.strings.ai_provider_claude
import com.mrl.pixiv.strings.ai_provider_gemini
import com.mrl.pixiv.strings.ai_provider_openai
import com.mrl.pixiv.strings.ai_refresh_models
import com.mrl.pixiv.strings.ai_translation_setting
import com.mrl.pixiv.strings.load_failed
import com.mrl.pixiv.strings.save
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AiTranslationSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
    modelCatalogService: AiModelCatalogService = koinInject(),
) {
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()
    val currentConfig = userPreference.aiTranslationConfig

    var providerName by rememberSaveable { mutableStateOf(currentConfig.provider.name) }
    var endpoint by rememberSaveable { mutableStateOf(currentConfig.endpoint) }
    var apiKey by rememberSaveable { mutableStateOf(currentConfig.apiKey) }
    var model by rememberSaveable { mutableStateOf(currentConfig.model) }
    var generationTimeoutInput by rememberSaveable {
        mutableStateOf(currentConfig.generationTimeoutSeconds.toString())
    }
    var maxConcurrentRequestsInput by rememberSaveable {
        mutableStateOf(currentConfig.maxConcurrentRequests.toString())
    }
    var responseApi by rememberSaveable { mutableStateOf(currentConfig.responseApi) }
    var extraBody by rememberSaveable { mutableStateOf(currentConfig.extraBody) }
    var extraBodyError by rememberSaveable { mutableStateOf(false) }
    var endpointError by remember { mutableStateOf<AiEndpointError?>(null) }
    var fetchedModels by remember { mutableStateOf<List<String>?>(null) }
    var modelRefreshError by remember { mutableStateOf<String?>(null) }
    var isRefreshingModels by remember { mutableStateOf(false) }
    var modelRefreshJob by remember { mutableStateOf<Job?>(null) }
    var modelRefreshVersion by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    fun resetModelCatalog() {
        modelRefreshVersion += 1L
        modelRefreshJob?.cancel()
        modelRefreshJob = null
        fetchedModels = null
        modelRefreshError = null
        isRefreshingModels = false
    }

    LaunchedEffect(currentConfig) {
        resetModelCatalog()
        providerName = currentConfig.provider.name
        endpoint = currentConfig.endpoint
        apiKey = currentConfig.apiKey
        model = currentConfig.model
        generationTimeoutInput = currentConfig.generationTimeoutSeconds.toString()
        maxConcurrentRequestsInput = currentConfig.maxConcurrentRequests.toString()
        responseApi = currentConfig.responseApi
        extraBody = currentConfig.extraBody
        extraBodyError = false
        endpointError = null
    }

    val selectedProvider = remember(providerName) {
        runCatching { enumValueOf<AiProvider>(providerName) }
            .getOrDefault(AiProvider.OPENAI)
    }
    val generationTimeoutSeconds = remember(generationTimeoutInput) {
        parseGenerationTimeoutSeconds(generationTimeoutInput)
    }
    val maxConcurrentRequests = remember(maxConcurrentRequestsInput) {
        maxConcurrentRequestsInput.toIntOrNull()?.takeIf {
            it in AiTranslationConfig.MAX_CONCURRENT_REQUESTS_MIN..
                AiTranslationConfig.MAX_CONCURRENT_REQUESTS_MAX
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.ai_translation_setting))
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (
                                generationTimeoutSeconds == null ||
                                maxConcurrentRequests == null
                            ) {
                                return@TextButton
                            }
                            val endpointValidation = validateAiEndpoint(endpoint)
                            if (!endpointValidation.isValid) {
                                endpointError = endpointValidation.error
                                return@TextButton
                            }
                            if (!extraBody.isValidExtraBodyJson()) {
                                extraBodyError = true
                                return@TextButton
                            }
                            SettingRepository.setAiTranslationConfig(
                                AiTranslationConfig(
                                    provider = selectedProvider,
                                    endpoint = requireNotNull(
                                        endpointValidation.normalizedEndpoint
                                    ),
                                    apiKey = apiKey.trim(),
                                    model = model.trim().ifEmpty {
                                        AiTranslationConfig.defaultModel(selectedProvider).modelId
                                    },
                                    responseApi = selectedProvider == AiProvider.OPENAI && responseApi,
                                    extraBody = extraBody.trim(),
                                    generationTimeoutSeconds = generationTimeoutSeconds,
                                    maxConcurrentRequests = maxConcurrentRequests,
                                )
                            )
                            if (endpointValidation.isLocalNetwork) {
                                AiLocalNetworkAccessGate.requestAccess(retryDenied = true)
                            }
                            navigationManager.popBackStack()
                        }
                    ) {
                        Text(text = stringResource(RStrings.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProviderItem(
                provider = selectedProvider,
                onProviderChange = change@{ nextProvider ->
                    if (nextProvider == selectedProvider) return@change
                    resetModelCatalog()
                    providerName = nextProvider.name
                    endpoint = AiTranslationConfig.defaultEndpoint(nextProvider)
                    model = AiTranslationConfig.defaultModel(nextProvider).modelId
                    apiKey = ""
                    extraBody = ""
                    extraBodyError = false
                }
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = endpoint,
                onValueChange = {
                    resetModelCatalog()
                    endpoint = it
                    endpointError = null
                },
                label = { Text(text = stringResource(RStrings.ai_endpoint)) },
                supportingText = endpointError?.let { error ->
                    {
                        Text(text = error.label())
                    }
                },
                isError = endpointError != null,
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = apiKey,
                onValueChange = {
                    resetModelCatalog()
                    apiKey = it
                },
                label = { Text(text = stringResource(RStrings.ai_api_key)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(RStrings.ai_model)) },
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = generationTimeoutInput,
                onValueChange = { generationTimeoutInput = it },
                label = {
                    Text(text = stringResource(RStrings.ai_generation_timeout_seconds))
                },
                supportingText = {
                    Text(
                        text = stringResource(
                            if (generationTimeoutSeconds == null) {
                                RStrings.ai_generation_timeout_invalid
                            } else {
                                RStrings.ai_generation_timeout_desc
                            },
                            AiTranslationConfig.GENERATION_TIMEOUT_MIN_SECONDS,
                            AiTranslationConfig.GENERATION_TIMEOUT_MAX_SECONDS,
                        )
                    )
                },
                isError = generationTimeoutSeconds == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = maxConcurrentRequestsInput,
                onValueChange = { maxConcurrentRequestsInput = it },
                label = {
                    Text(text = stringResource(RStrings.ai_max_concurrent_requests))
                },
                supportingText = {
                    Text(
                        text = stringResource(
                            if (maxConcurrentRequests == null) {
                                RStrings.ai_max_concurrent_requests_invalid
                            } else {
                                RStrings.ai_max_concurrent_requests_desc
                            },
                            AiTranslationConfig.MAX_CONCURRENT_REQUESTS_MIN,
                            AiTranslationConfig.MAX_CONCURRENT_REQUESTS_MAX,
                        )
                    )
                },
                isError = maxConcurrentRequests == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            if (selectedProvider == AiProvider.OPENAI) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .throttleClick {
                            responseApi = !responseApi
                        },
                    headlineContent = {
                        Text(text = stringResource(RStrings.ai_openai_use_response_api))
                    },
                    trailingContent = {
                        Checkbox(
                            checked = responseApi,
                            onCheckedChange = { checked ->
                                responseApi = checked
                            }
                        )
                    }
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = extraBody,
                onValueChange = {
                    extraBody = it
                    extraBodyError = false
                },
                label = { Text(text = stringResource(RStrings.ai_extra_body)) },
                placeholder = { Text(text = stringResource(RStrings.ai_extra_body_hint)) },
                supportingText = {
                    if (extraBodyError) {
                        Text(text = stringResource(RStrings.ai_extra_body_invalid))
                    }
                },
                isError = extraBodyError,
                minLines = 6,
                maxLines = 12,
            )

            Text(
                text = stringResource(RStrings.ai_extra_body_presets),
                style = MaterialTheme.typography.labelLarge,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                extraBodyPresets(
                    provider = selectedProvider,
                    responseApi = responseApi,
                ).forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val merged = extraBody.mergeExtraBodyPreset(preset)
                            if (merged == null) {
                                extraBodyError = true
                            } else {
                                extraBody = merged
                                extraBodyError = false
                            }
                        },
                        label = { Text(text = preset.label()) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(RStrings.ai_model_suggestions),
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(
                    enabled = !isRefreshingModels,
                    onClick = {
                        val endpointValidation = validateAiEndpoint(endpoint)
                        if (!endpointValidation.isValid) {
                            endpointError = endpointValidation.error
                            return@TextButton
                        }
                        endpointError = null
                        modelRefreshError = null
                        isRefreshingModels = true
                        modelRefreshVersion += 1L
                        val refreshVersion = modelRefreshVersion
                        val refreshConfig = AiTranslationConfig(
                            provider = selectedProvider,
                            endpoint = requireNotNull(endpointValidation.normalizedEndpoint),
                            apiKey = apiKey.trim(),
                        )
                        modelRefreshJob?.cancel()
                        modelRefreshJob = coroutineScope.launch {
                            try {
                                val models = modelCatalogService.fetchModels(refreshConfig)
                                if (modelRefreshVersion == refreshVersion) {
                                    fetchedModels = models
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Throwable) {
                                if (modelRefreshVersion == refreshVersion) {
                                    modelRefreshError = error.message ?: error.toString()
                                }
                            } finally {
                                if (modelRefreshVersion == refreshVersion) {
                                    isRefreshingModels = false
                                    modelRefreshJob = null
                                }
                            }
                        }
                    },
                ) {
                    if (isRefreshingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                        )
                    }
                    Text(text = stringResource(RStrings.ai_refresh_models))
                }
            }

            modelRefreshError?.let { error ->
                Text(
                    text = stringResource(RStrings.load_failed, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val modelSuggestions = fetchedModels
                    ?: AiTranslationConfig.suggestedModels(selectedProvider).map { it.modelId }
                modelSuggestions.forEach { modelId ->
                    FilterChip(
                        selected = modelId == model,
                        onClick = { model = modelId },
                        label = { Text(text = modelId) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun AiEndpointError.label(): String = when (this) {
    AiEndpointError.CREDENTIALS_NOT_ALLOWED ->
        stringResource(RStrings.ai_endpoint_credentials_not_allowed)

    AiEndpointError.PUBLIC_HTTP_NOT_ALLOWED ->
        stringResource(RStrings.ai_endpoint_public_http_not_allowed)

    AiEndpointError.EMPTY,
    AiEndpointError.INVALID_URL,
    AiEndpointError.UNSUPPORTED_SCHEME ->
        stringResource(RStrings.ai_endpoint_invalid)
}

private data class ExtraBodyPreset(
    val label: @Composable () -> String,
    val json: String,
    val replaceKeys: Set<String> = emptySet(),
)

private fun extraBodyPresets(
    provider: AiProvider,
    responseApi: Boolean,
): List<ExtraBodyPreset> = when (provider) {
    AiProvider.OPENAI -> openAiExtraBodyPresets(responseApi)
    AiProvider.CLAUDE -> claudeExtraBodyPresets
    AiProvider.GEMINI -> geminiExtraBodyPresets
}

private fun openAiExtraBodyPresets(responseApi: Boolean): List<ExtraBodyPreset> {
    val reasoningKey = if (responseApi) "reasoning" else "reasoning_effort"
    fun reasoningPreset(effort: String) = ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_reasoning_effort, effort) },
        json = if (responseApi) {
            """{"reasoning":{"effort":"$effort"}}"""
        } else {
            """{"reasoning_effort":"$effort"}"""
        },
        replaceKeys = setOf("thinking", reasoningKey),
    )

    return listOf(
        ExtraBodyPreset(
            label = { stringResource(RStrings.ai_extra_preset_deepseek_disable_thinking) },
            json = """{"thinking":{"type":"disabled"}}""",
            replaceKeys = setOf("reasoning", "reasoning_effort", "thinking"),
        ),
        reasoningPreset("low"),
        reasoningPreset("medium"),
        reasoningPreset("high"),
        ExtraBodyPreset(
            label = { stringResource(RStrings.ai_extra_preset_deepseek_reasoning_max) },
            json = """{"reasoning_effort":"max"}""",
            replaceKeys = setOf("thinking", "reasoning", "reasoning_effort"),
        ),
        ExtraBodyPreset(
            label = { stringResource(RStrings.ai_extra_preset_temperature, "0.2") },
            json = """{"temperature":0.2}""",
            replaceKeys = setOf("temperature"),
        ),
        ExtraBodyPreset(
            label = { stringResource(RStrings.ai_extra_preset_top_p, "0.9") },
            json = """{"top_p":0.9}""",
            replaceKeys = setOf("top_p"),
        ),
    )
}

private val claudeExtraBodyPresets = listOf(
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_claude_effort, "low") },
        json = """{"output_config":{"effort":"low"}}""",
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_claude_effort, "medium") },
        json = """{"output_config":{"effort":"medium"}}""",
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_claude_effort, "high") },
        json = """{"output_config":{"effort":"high"}}""",
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_claude_effort, "xhigh") },
        json = """{"output_config":{"effort":"xhigh"}}""",
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_claude_adaptive_thinking) },
        json = """{"thinking":{"type":"adaptive"}}""",
        replaceKeys = setOf("thinking", "reasoning", "reasoning_effort", "generationConfig"),
    ),
)

private val geminiExtraBodyPresets = listOf(
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_gemini_disable_thinking) },
        json = """{"generationConfig":{"thinkingConfig":{"thinkingBudget":0}}}""",
        replaceKeys = setOf("thinking", "reasoning", "reasoning_effort"),
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_gemini_dynamic_thinking) },
        json = """{"generationConfig":{"thinkingConfig":{"thinkingBudget":-1}}}""",
        replaceKeys = setOf("thinking", "reasoning", "reasoning_effort"),
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_gemini_thinking_budget, "1024") },
        json = """{"generationConfig":{"thinkingConfig":{"thinkingBudget":1024}}}""",
        replaceKeys = setOf("thinking", "reasoning", "reasoning_effort"),
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_temperature, "0.2") },
        json = """{"generationConfig":{"temperature":0.2}}""",
    ),
    ExtraBodyPreset(
        label = { stringResource(RStrings.ai_extra_preset_top_p, "0.9") },
        json = """{"generationConfig":{"topP":0.9}}""",
    ),
)

private val extraBodyJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    prettyPrint = true
}

private fun String.isValidExtraBodyJson(): Boolean {
    if (isBlank()) return true
    return runCatching { extraBodyJson.parseToJsonElement(this) as? JsonObject }
        .getOrNull() != null
}

private fun String.mergeExtraBodyPreset(preset: ExtraBodyPreset): String? {
    val current = if (isBlank()) {
        JsonObject(emptyMap())
    } else {
        runCatching { extraBodyJson.parseToJsonElement(this) as? JsonObject }
            .getOrNull()
            ?: return null
    }
    val presetObject = runCatching { extraBodyJson.parseToJsonElement(preset.json) as? JsonObject }
        .getOrNull()
        ?: return null

    return extraBodyJson.encodeToString(
        JsonObject.serializer(),
        current.withoutKeys(preset.replaceKeys).mergeWith(presetObject),
    )
}

private fun JsonObject.withoutKeys(keys: Set<String>): JsonObject {
    if (keys.isEmpty()) return this
    return JsonObject(filterKeys { key -> key !in keys })
}

private fun JsonObject.mergeWith(other: JsonObject): JsonObject {
    val merged = toMutableMap()
    other.forEach { (key, value) ->
        val currentValue = merged[key]
        merged[key] = if (currentValue is JsonObject && value is JsonObject) {
            currentValue.mergeWith(value)
        } else {
            value
        }
    }
    return JsonObject(merged)
}

@Composable
private fun ProviderItem(
    provider: AiProvider,
    onProviderChange: (AiProvider) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(text = stringResource(RStrings.ai_provider))
        },
        leadingContent = {
            Icon(imageVector = Icons.Rounded.Translate, contentDescription = null)
        },
        trailingContent = {
            DropDownSelector(
                modifier = Modifier.throttleClick {
                    expanded = !expanded
                },
                expanded = expanded,
                onDismissRequest = { expanded = false },
                current = provider.toDisplayName(),
            ) {
                AiProvider.entries.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(text = item.toDisplayName())
                        },
                        onClick = {
                            onProviderChange(item)
                            expanded = false
                        },
                        trailingIcon = {
                            if (item == provider) {
                                Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun AiProvider.toDisplayName(): String {
    return when (this) {
        AiProvider.OPENAI -> stringResource(RStrings.ai_provider_openai)
        AiProvider.CLAUDE -> stringResource(RStrings.ai_provider_claude)
        AiProvider.GEMINI -> stringResource(RStrings.ai_provider_gemini)
    }
}
