package com.mrl.pixiv.setting.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.AiProvider
import com.mrl.pixiv.common.data.setting.AiTranslationConfig
import com.mrl.pixiv.common.repository.SettingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.setting.components.DropDownSelector
import com.mrl.pixiv.strings.ai_api_key
import com.mrl.pixiv.strings.ai_endpoint
import com.mrl.pixiv.strings.ai_extra_body
import com.mrl.pixiv.strings.ai_extra_body_hint
import com.mrl.pixiv.strings.ai_extra_body_invalid
import com.mrl.pixiv.strings.ai_extra_body_presets
import com.mrl.pixiv.strings.ai_model
import com.mrl.pixiv.strings.ai_model_suggestions
import com.mrl.pixiv.strings.ai_openai_use_response_api
import com.mrl.pixiv.strings.ai_provider
import com.mrl.pixiv.strings.ai_provider_claude
import com.mrl.pixiv.strings.ai_provider_gemini
import com.mrl.pixiv.strings.ai_provider_openai
import com.mrl.pixiv.strings.ai_translation_setting
import com.mrl.pixiv.strings.save
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AiTranslationSettingScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
) {
    val userPreference by SettingRepository.userPreferenceFlow.collectAsStateWithLifecycle()
    val currentConfig = userPreference.aiTranslationConfig

    var providerName by rememberSaveable { mutableStateOf(currentConfig.provider.name) }
    var endpoint by rememberSaveable { mutableStateOf(currentConfig.endpoint) }
    var apiKey by rememberSaveable { mutableStateOf(currentConfig.apiKey) }
    var model by rememberSaveable { mutableStateOf(currentConfig.model) }
    var responseApi by rememberSaveable { mutableStateOf(currentConfig.responseApi) }
    var extraBody by rememberSaveable { mutableStateOf(currentConfig.extraBody) }
    var extraBodyError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentConfig) {
        providerName = currentConfig.provider.name
        endpoint = currentConfig.endpoint
        apiKey = currentConfig.apiKey
        model = currentConfig.model
        responseApi = currentConfig.responseApi
        extraBody = currentConfig.extraBody
        extraBodyError = false
    }

    val selectedProvider = remember(providerName) {
        runCatching { enumValueOf<AiProvider>(providerName) }
            .getOrDefault(AiProvider.OPENAI)
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
                            if (!extraBody.isValidExtraBodyJson()) {
                                extraBodyError = true
                                return@TextButton
                            }
                            SettingRepository.setAiTranslationConfig(
                                AiTranslationConfig(
                                    provider = selectedProvider,
                                    endpoint = endpoint.trim().ifEmpty {
                                        AiTranslationConfig.defaultEndpoint(selectedProvider)
                                    },
                                    apiKey = apiKey.trim(),
                                    model = model.trim().ifEmpty {
                                        AiTranslationConfig.defaultModel(selectedProvider).modelId
                                    },
                                    responseApi = selectedProvider == AiProvider.OPENAI && responseApi,
                                    extraBody = extraBody.trim(),
                                )
                            )
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
                    providerName = nextProvider.name
                    endpoint = AiTranslationConfig.defaultEndpoint(nextProvider)
                    model = AiTranslationConfig.defaultModel(nextProvider).modelId
                    apiKey = ""
                }
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text(text = stringResource(RStrings.ai_endpoint)) },
                singleLine = true,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = apiKey,
                onValueChange = { apiKey = it },
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
                extraBodyPresets.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val merged = extraBody.mergeExtraBodyPreset(preset.json)
                            if (merged == null) {
                                extraBodyError = true
                            } else {
                                extraBody = merged
                                extraBodyError = false
                            }
                        },
                        label = { Text(text = preset.label) },
                    )
                }
            }

            Text(
                text = stringResource(RStrings.ai_model_suggestions),
                style = MaterialTheme.typography.labelLarge,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiTranslationConfig.suggestedModels(selectedProvider).forEach { modelName ->
                    val modelId = modelName.modelId
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

private data class ExtraBodyPreset(
    val label: String,
    val json: String,
)

private val extraBodyPresets = listOf(
    ExtraBodyPreset(
        label = "DeepSeek: Disable Thinking",
        json = """{"thinking":{"type":"disabled"}}""",
    ),
    ExtraBodyPreset(
        label = "Reasoning: high",
        json = """{"reasoning_effort":"high"}""",
    ),
    ExtraBodyPreset(
        label = "Reasoning: max",
        json = """{"reasoning_effort":"max"}""",
    ),
    ExtraBodyPreset(
        label = "Temperature: 0.2",
        json = """{"temperature":0.2}""",
    ),
    ExtraBodyPreset(
        label = "Top P: 0.9",
        json = """{"top_p":0.9}""",
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

private fun String.mergeExtraBodyPreset(preset: String): String? {
    val current = if (isBlank()) {
        JsonObject(emptyMap())
    } else {
        runCatching { extraBodyJson.parseToJsonElement(this) as? JsonObject }
            .getOrNull()
            ?: return null
    }
    val presetObject = runCatching { extraBodyJson.parseToJsonElement(preset) as? JsonObject }
        .getOrNull()
        ?: return null

    return extraBodyJson.encodeToString(
        JsonObject.serializer(),
        current.mergeWith(presetObject),
    )
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
