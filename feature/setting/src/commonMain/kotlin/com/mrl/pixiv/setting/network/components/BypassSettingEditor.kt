package com.mrl.pixiv.setting.network.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting.Proxy.ProxyType
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.components.SettingRow
import com.mrl.pixiv.setting.components.SettingSectionTitle
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.ech_doh_url
import com.mrl.pixiv.strings.network_advanced_settings
import com.mrl.pixiv.strings.network_advanced_settings_desc
import com.mrl.pixiv.strings.network_plan
import com.mrl.pixiv.strings.protocol_http
import com.mrl.pixiv.strings.protocol_socks
import com.mrl.pixiv.strings.proxy_host
import com.mrl.pixiv.strings.proxy_port
import com.mrl.pixiv.strings.use_ech
import com.mrl.pixiv.strings.use_ech_desc
import com.mrl.pixiv.strings.use_none
import com.mrl.pixiv.strings.use_none_desc
import com.mrl.pixiv.strings.use_proxy
import com.mrl.pixiv.strings.use_proxy_desc
import com.mrl.pixiv.strings.use_system_proxy
import com.mrl.pixiv.strings.use_system_proxy_desc
import io.ktor.http.URLProtocol
import io.ktor.http.parseUrl
import org.jetbrains.compose.resources.stringResource

/**
 * IPv4：0.0.0.0 ~ 255.255.255.255
 */
private val IPV4 = Regex(
    pattern = """^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$"""
)

/**
 * 域名（ASCII，不含协议、不含端口）：
 * - 标准域名：至少一个点，TLD 2~63 字母
 * - 或者：单段主机名（无点），1~63，规则同 label（不以 - 开头/结尾）
 * - 总长度 1~253（正则里不好精确卡死，这里更偏实用）
 */
private val DOMAIN = Regex(
    pattern = """^(?=.{1,253}$)((?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}|[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)$"""
)

/**
 * “合法 IP 或域名”（不含端口）
 */
private val HOST = Regex(
    pattern = """^(?:${IPV4.pattern.drop(1).dropLast(1)}|${DOMAIN.pattern.drop(1).dropLast(1)})$"""
)

@Composable
fun BypassSettingEditor(
    bypassSetting: BypassSetting,
    onUpdate: (BypassSetting) -> Unit,
    supportsEch: Boolean,
    modifier: Modifier = Modifier
) {
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var showEditDoh by rememberSaveable { mutableStateOf(false) }
    var showEditProxy by rememberSaveable { mutableStateOf(false) }

    if (supportsEch && showEditDoh && bypassSetting is BypassSetting.ECH) {
        EchDohEditDialog(
            setting = bypassSetting,
            onUpdate = onUpdate,
            onDismiss = { showEditDoh = false },
        )
    }

    if (showEditProxy) {
        ProxyEditDialog(
            setting = bypassSetting as? BypassSetting.Proxy ?: BypassSetting.Proxy(),
            onConfirm = { setting ->
                onUpdate(setting)
                showEditProxy = false
            },
            onDismiss = { showEditProxy = false },
        )
    }

    BypassSettingContent(
        bypassSetting = bypassSetting,
        onUpdate = onUpdate,
        advancedExpanded = advancedExpanded,
        onToggleAdvanced = { advancedExpanded = !advancedExpanded },
        onEditDoh = { showEditDoh = true },
        onEditProxy = { showEditProxy = true },
        supportsEch = supportsEch,
        modifier = modifier,
    )
}

@Composable
internal fun BypassSettingContent(
    bypassSetting: BypassSetting,
    onUpdate: (BypassSetting) -> Unit,
    advancedExpanded: Boolean,
    onToggleAdvanced: () -> Unit,
    onEditDoh: () -> Unit,
    onEditProxy: () -> Unit,
    supportsEch: Boolean,
    modifier: Modifier = Modifier,
) {
    val effectiveSetting = if (!supportsEch && bypassSetting is BypassSetting.ECH) {
        BypassSetting.Direct
    } else {
        bypassSetting
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingSectionTitle(stringResource(RStrings.network_plan))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = remember(supportsEch) {
                        listOfNotNull(
                            BypassSetting.System,
                            BypassSetting.Direct,
                            BypassSetting.Proxy(),
                            BypassSetting.ECH().takeIf { supportsEch },
                        )
                    }

                    types.forEach { type ->
                        val selected = when (effectiveSetting) {
                            BypassSetting.System -> type == BypassSetting.System
                            BypassSetting.Direct -> type == BypassSetting.Direct
                            is BypassSetting.Proxy -> type is BypassSetting.Proxy
                            is BypassSetting.ECH -> type is BypassSetting.ECH
                        }

                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (!selected || effectiveSetting != bypassSetting) {
                                    if (type is BypassSetting.Proxy) onEditProxy() else onUpdate(type)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(
                                        when (type) {
                                            BypassSetting.System -> RStrings.use_system_proxy
                                            BypassSetting.Direct -> RStrings.use_none
                                            is BypassSetting.Proxy -> RStrings.use_proxy
                                            is BypassSetting.ECH -> RStrings.use_ech
                                        }
                                    ),
                                )
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                    }
                }
                Text(
                    text = stringResource(
                        when (effectiveSetting) {
                            BypassSetting.System -> RStrings.use_system_proxy_desc
                            BypassSetting.Direct -> RStrings.use_none_desc
                            is BypassSetting.Proxy -> RStrings.use_proxy_desc
                            is BypassSetting.ECH -> RStrings.use_ech_desc
                        }
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        AnimatedContent(
            targetState = effectiveSetting::class,
            transitionSpec = { slideInVertically() togetherWith slideOutVertically() },
            label = "bypass_setting_content",
        ) {
            when (effectiveSetting) {
                BypassSetting.System, BypassSetting.Direct -> {}
                is BypassSetting.Proxy -> {
                    SettingRow(
                        title = stringResource(RStrings.proxy_host),
                        icon = Icons.Rounded.Router,
                        onClick = onEditProxy,
                        supportingContent = {
                            val type = stringResource(effectiveSetting.proxyType.label)
                            Text("${effectiveSetting.host}:${effectiveSetting.port} · $type")
                        },
                    )
                }

                is BypassSetting.ECH -> {
                    EchEditor(
                        dohUrl = effectiveSetting.url,
                        advancedExpanded = advancedExpanded,
                        onToggleAdvanced = onToggleAdvanced,
                        onEditDoh = onEditDoh,
                    )
                }
            }
        }
    }
}

private val ProxyType.label
    get() = when (this) {
        ProxyType.HTTP -> RStrings.protocol_http
        ProxyType.SOCKS -> RStrings.protocol_socks
    }

@Composable
private fun ProxyEditDialog(
    setting: BypassSetting.Proxy,
    onConfirm: (BypassSetting.Proxy) -> Unit,
    onDismiss: () -> Unit,
) {
    var host by rememberSaveable(setting.host) { mutableStateOf(setting.host) }
    var portText by rememberSaveable(setting.port) {
        mutableStateOf(setting.port.takeIf { it != 0 }?.toString().orEmpty())
    }
    var proxyType by rememberSaveable(setting.proxyType) { mutableStateOf(setting.proxyType) }
    val normalizedHost = host.trim()
    val port = portText.toIntOrNull()?.takeIf { it in 1..65535 }
    val validHost = HOST.matches(normalizedHost)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RStrings.use_proxy)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProxyType.entries.forEach { type ->
                        FilterChip(
                            selected = proxyType == type,
                            onClick = { proxyType = type },
                            label = { Text(stringResource(type.label)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(RStrings.proxy_host)) },
                    singleLine = true,
                    isError = host.isNotBlank() && !validHost,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(RStrings.proxy_port)) },
                    singleLine = true,
                    isError = portText.isNotBlank() && port == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = validHost && port != null,
                onClick = {
                    if (validHost && port != null) {
                        onConfirm(BypassSetting.Proxy(normalizedHost, port, proxyType))
                    }
                },
            ) {
                Text(stringResource(RStrings.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RStrings.cancel)) }
        },
    )
}

@Composable
private fun EchEditor(
    dohUrl: String,
    advancedExpanded: Boolean,
    onToggleAdvanced: () -> Unit,
    onEditDoh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingRow(
            title = stringResource(RStrings.network_advanced_settings),
            icon = Icons.Rounded.Tune,
            modifier = Modifier.semantics {
                if (advancedExpanded) {
                    collapse {
                        onToggleAdvanced()
                        true
                    }
                } else {
                    expand {
                        onToggleAdvanced()
                        true
                    }
                }
            },
            onClick = onToggleAdvanced,
            supportingContent = {
                Text(text = stringResource(RStrings.network_advanced_settings_desc))
            },
            trailingContent = {
                Icon(
                    imageVector = if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                )
            },
        )
        // 沿用 Column 中 AnimatedVisibility 的默认纵向展开/收起效果
        CapturedAnimatedVisibility(
            visible = advancedExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SettingRow(
                title = stringResource(RStrings.ech_doh_url),
                icon = Icons.Rounded.Dns,
                onClick = onEditDoh,
                supportingContent = { Text(text = dohUrl) },
            )
        }
    }
}

@Composable
internal fun EchDohEditDialog(
    setting: BypassSetting.ECH,
    onUpdate: (BypassSetting) -> Unit,
    onDismiss: () -> Unit,
) {
    EditDialog(
        title = stringResource(RStrings.ech_doh_url),
        initialValue = setting.url,
        onConfirm = {
            onUpdate(setting.copy(url = it.trim()))
            onDismiss()
        },
        isValid = {
            parseUrl(it.trim())?.let { url ->
                url.protocol == URLProtocol.HTTPS && url.host.isNotBlank()
            } ?: false
        },
        onDismiss = onDismiss,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
}
