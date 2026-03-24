package com.mrl.pixiv.setting.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.mrl.pixiv.strings.ai_model
import com.mrl.pixiv.strings.ai_model_suggestions
import com.mrl.pixiv.strings.ai_openai_use_response_api
import com.mrl.pixiv.strings.ai_provider
import com.mrl.pixiv.strings.ai_provider_claude
import com.mrl.pixiv.strings.ai_provider_gemini
import com.mrl.pixiv.strings.ai_provider_openai
import com.mrl.pixiv.strings.ai_translation_setting
import com.mrl.pixiv.strings.save
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

    LaunchedEffect(currentConfig) {
        providerName = currentConfig.provider.name
        endpoint = currentConfig.endpoint
        apiKey = currentConfig.apiKey
        model = currentConfig.model
        responseApi = currentConfig.responseApi
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

            Text(
                text = stringResource(RStrings.ai_model_suggestions),
                style = MaterialTheme.typography.labelLarge,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
