package com.mrl.pixiv.setting.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.Constants.IMAGE_HOST
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.network.NetworkDiagnosticEvent
import com.mrl.pixiv.common.network.NetworkDiagnosticEventStatus
import com.mrl.pixiv.common.network.NetworkDiagnosticFailure
import com.mrl.pixiv.common.network.NetworkDiagnosticResolver
import com.mrl.pixiv.common.network.NetworkDiagnosticResult
import com.mrl.pixiv.common.network.NetworkDiagnosticRoute
import com.mrl.pixiv.common.network.NetworkDiagnosticRouteMode
import com.mrl.pixiv.common.network.NetworkDiagnosticStage
import com.mrl.pixiv.common.network.NetworkDiagnosticStatus
import com.mrl.pixiv.common.network.NetworkDiagnosticTarget
import com.mrl.pixiv.common.network.NetworkSettings
import com.mrl.pixiv.common.network.networkSettings
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.Platform
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.isIOS
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.setting.components.SettingPageColumn
import com.mrl.pixiv.setting.components.SettingRow
import com.mrl.pixiv.setting.network.components.appApiProxyHostLabel
import com.mrl.pixiv.strings.app_api_proxy_title
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.image_source
import com.mrl.pixiv.strings.login_webview_route_unavailable
import com.mrl.pixiv.strings.network_diagnostic_api
import com.mrl.pixiv.strings.network_diagnostic_attempted_route
import com.mrl.pixiv.strings.network_diagnostic_bytes
import com.mrl.pixiv.strings.network_diagnostic_canceled
import com.mrl.pixiv.strings.network_diagnostic_dns
import com.mrl.pixiv.strings.network_diagnostic_dns_addresses_unknown
import com.mrl.pixiv.strings.network_diagnostic_dns_doh
import com.mrl.pixiv.strings.network_diagnostic_dns_fallback
import com.mrl.pixiv.strings.network_diagnostic_dns_host_unknown
import com.mrl.pixiv.strings.network_diagnostic_dns_observed
import com.mrl.pixiv.strings.network_diagnostic_dns_source_unknown
import com.mrl.pixiv.strings.network_diagnostic_dns_system
import com.mrl.pixiv.strings.network_diagnostic_effective_direct
import com.mrl.pixiv.strings.network_diagnostic_failed_stage
import com.mrl.pixiv.strings.network_diagnostic_failure_http
import com.mrl.pixiv.strings.network_diagnostic_failure_incomplete
import com.mrl.pixiv.strings.network_diagnostic_failure_proxy_auth
import com.mrl.pixiv.strings.network_diagnostic_failure_proxy_target
import com.mrl.pixiv.strings.network_diagnostic_failure_rate_limited
import com.mrl.pixiv.strings.network_diagnostic_failure_redirect
import com.mrl.pixiv.strings.network_diagnostic_failure_response
import com.mrl.pixiv.strings.network_diagnostic_failure_sample
import com.mrl.pixiv.strings.network_diagnostic_failure_size
import com.mrl.pixiv.strings.network_diagnostic_failure_timeout
import com.mrl.pixiv.strings.network_diagnostic_failure_transport
import com.mrl.pixiv.strings.network_diagnostic_image
import com.mrl.pixiv.strings.network_diagnostic_ipv6_fallback
import com.mrl.pixiv.strings.network_diagnostic_login
import com.mrl.pixiv.strings.network_diagnostic_not_observed
import com.mrl.pixiv.strings.network_diagnostic_passed
import com.mrl.pixiv.strings.network_diagnostic_pending
import com.mrl.pixiv.strings.network_diagnostic_proxy_dns
import com.mrl.pixiv.strings.network_diagnostic_requires_login
import com.mrl.pixiv.strings.network_diagnostic_retry
import com.mrl.pixiv.strings.network_diagnostic_reused
import com.mrl.pixiv.strings.network_diagnostic_route
import com.mrl.pixiv.strings.network_diagnostic_settings_changed
import com.mrl.pixiv.strings.network_diagnostic_snapshot
import com.mrl.pixiv.strings.network_diagnostic_stage_body
import com.mrl.pixiv.strings.network_diagnostic_stage_connect
import com.mrl.pixiv.strings.network_diagnostic_stage_proxy
import com.mrl.pixiv.strings.network_diagnostic_stage_tls
import com.mrl.pixiv.strings.network_diagnostic_start
import com.mrl.pixiv.strings.network_diagnostic_time
import com.mrl.pixiv.strings.network_diagnostic_title
import com.mrl.pixiv.strings.network_diagnostic_web
import com.mrl.pixiv.strings.network_diagnostic_web_http_only
import com.mrl.pixiv.strings.network_diagnostic_website
import com.mrl.pixiv.strings.network_plan
import com.mrl.pixiv.strings.protocol_http
import com.mrl.pixiv.strings.protocol_socks
import com.mrl.pixiv.strings.status_completed
import com.mrl.pixiv.strings.status_failed
import com.mrl.pixiv.strings.status_running
import com.mrl.pixiv.strings.use_ech
import com.mrl.pixiv.strings.use_none
import com.mrl.pixiv.strings.use_system_proxy
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal enum class NetworkDiagnosticSection { DNS, API, WEB, IMAGE }

@Composable
fun NetworkDiagnosticScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkDiagnosticViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val preference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    NetworkDiagnosticContent(
        state = state,
        currentSettings = preference.networkSettings(),
        currentPlatform = platform,
        expandedSections = expanded,
        onToggleSection = { section ->
            expanded = ArrayList(
                if (section.name in expanded) expanded - section.name else expanded + section.name
            )
        },
        onStart = viewModel::start,
        onCancel = viewModel::cancel,
        onBack = navigationManager::popBackStack,
        modifier = modifier,
    )
}

@Composable
internal fun NetworkDiagnosticContent(
    state: NetworkDiagnosticUiState,
    currentSettings: NetworkSettings,
    currentPlatform: Platform,
    expandedSections: List<String>,
    onToggleSection: (NetworkDiagnosticSection) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings ?: currentSettings
    val api = state.results[NetworkDiagnosticTarget.APP_API]
    val web = state.results[NetworkDiagnosticTarget.WEB]
    val login = state.results[NetworkDiagnosticTarget.LOGIN]
    val image = state.results[NetworkDiagnosticTarget.IMAGE]
    val nextTarget = NetworkDiagnosticTarget.entries.firstOrNull { it !in state.results }
    fun runFor(target: NetworkDiagnosticTarget) =
        if (state.run == NetworkDiagnosticRun.RUNNING && target != nextTarget) {
            NetworkDiagnosticRun.IDLE
        } else {
            state.run
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(RStrings.network_diagnostic_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingPageColumn(innerPadding) {
            DiagnosticRunCard(state, settings, currentSettings, currentPlatform, onStart, onCancel)
            DiagnosticSection(
                title = stringResource(RStrings.network_diagnostic_dns),
                icon = Icons.Rounded.Dns,
                expanded = NetworkDiagnosticSection.DNS.name in expandedSections,
                onToggle = if (state.results.isNotEmpty()) {
                    { onToggleSection(NetworkDiagnosticSection.DNS) }
                } else null,
                summary = { Text(dnsSummary(state)) },
            ) {
                state.results.values.forEach { DnsDetails(it) }
            }
            DiagnosticSection(
                title = stringResource(RStrings.network_diagnostic_api),
                icon = Icons.Rounded.Cloud,
                expanded = NetworkDiagnosticSection.API.name in expandedSections,
                onToggle = api?.let { { onToggleSection(NetworkDiagnosticSection.API) } },
                summary = { ResultSummary(api, runFor(NetworkDiagnosticTarget.APP_API)) },
            ) {
                api?.let { ResultDetails(it) }
            }
            DiagnosticSection(
                title = stringResource(RStrings.network_diagnostic_web),
                icon = Icons.Rounded.Language,
                expanded = NetworkDiagnosticSection.WEB.name in expandedSections,
                onToggle = if (web != null || login != null) {
                    { onToggleSection(NetworkDiagnosticSection.WEB) }
                } else null,
                summary = {
                    ResultSummary(
                        web,
                        runFor(NetworkDiagnosticTarget.WEB),
                        stringResource(RStrings.network_diagnostic_website),
                    )
                    ResultSummary(
                        login,
                        runFor(NetworkDiagnosticTarget.LOGIN),
                        stringResource(RStrings.network_diagnostic_login),
                    )
                    Text(stringResource(RStrings.network_diagnostic_web_http_only))
                    if (webViewRouteUnavailable(settings.bypassSetting, currentPlatform)) {
                        Text(stringResource(RStrings.login_webview_route_unavailable))
                    }
                },
            ) {
                web?.let { ResultDetails(it) }
                login?.let {
                    if (web != null) HorizontalDivider()
                    ResultDetails(it)
                }
            }
            DiagnosticSection(
                title = stringResource(RStrings.network_diagnostic_image),
                icon = Icons.Rounded.Photo,
                expanded = NetworkDiagnosticSection.IMAGE.name in expandedSections,
                onToggle = image?.let { { onToggleSection(NetworkDiagnosticSection.IMAGE) } },
                summary = { ResultSummary(image, runFor(NetworkDiagnosticTarget.IMAGE)) },
            ) {
                image?.let { ResultDetails(it) }
            }
        }
    }
}

@Composable
private fun DiagnosticRunCard(
    state: NetworkDiagnosticUiState,
    settings: NetworkSettings,
    currentSettings: NetworkSettings,
    currentPlatform: Platform,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    val running = state.run == NetworkDiagnosticRun.RUNNING
    val iosEch = currentPlatform.isIOS() && settings.bypassSetting is BypassSetting.ECH
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.settings == null) RStrings.network_plan else RStrings.network_diagnostic_snapshot
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(configuredRouteLabel(settings.bypassSetting), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(RStrings.image_source) + "  " + settings.imageHost.ifEmpty { IMAGE_HOST },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(RStrings.app_api_proxy_title), style = MaterialTheme.typography.bodyMedium)
                Text(appApiProxyHostLabel(settings.appApiProxyHost), style = MaterialTheme.typography.bodyMedium)
            }
            if (iosEch) {
                Text(stringResource(RStrings.network_diagnostic_effective_direct))
            }
            if (settings != currentSettings) {
                Text(
                    text = stringResource(RStrings.network_diagnostic_settings_changed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            if (running) {
                LinearProgressIndicator(
                    progress = { state.results.size.toFloat() / NetworkDiagnosticTarget.entries.size },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(RStrings.cancel))
                }
            } else {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(
                            if (state.run == NetworkDiagnosticRun.IDLE) {
                                RStrings.network_diagnostic_start
                            } else {
                                RStrings.network_diagnostic_retry
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
    summary: @Composable ColumnScope.() -> Unit,
    details: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            SettingRow(
                title = title,
                icon = icon,
                modifier = Modifier.semantics {
                    if (onToggle != null) {
                        val toggle = {
                            onToggle()
                            true
                        }
                        if (expanded) collapse(action = toggle) else expand(action = toggle)
                    }
                },
                onClick = onToggle,
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = summary)
                },
                trailingContent = onToggle?.let {
                    {
                        Icon(
                            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                        )
                    }
                },
            )
            if (expanded && onToggle != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = details,
                )
            }
        }
    }
}

@Composable
private fun ResultSummary(
    result: NetworkDiagnosticResult?,
    run: NetworkDiagnosticRun,
    title: String? = null,
) {
    val status = when (result?.status) {
        NetworkDiagnosticStatus.SUCCESS -> stringResource(
            if (result.target == NetworkDiagnosticTarget.IMAGE) RStrings.status_completed
            else RStrings.network_diagnostic_passed
        )
        NetworkDiagnosticStatus.REQUIRES_LOGIN -> stringResource(RStrings.network_diagnostic_requires_login)
        NetworkDiagnosticStatus.FAILED -> result.failure?.let { failureLabel(it) }
            ?: stringResource(RStrings.status_failed)
        null -> if (run == NetworkDiagnosticRun.COMPLETED) {
            stringResource(RStrings.network_diagnostic_not_observed)
        } else {
            pendingLabel(run)
        }
    }
    val color = if (result?.status == NetworkDiagnosticStatus.FAILED) {
        MaterialTheme.colorScheme.error
    } else {
        Color.Unspecified
    }
    val label = listOfNotNull(title, status, result?.httpStatus?.let { "HTTP $it" }).joinToString(" · ")
    Text(label, color = color)
    if (result == null) return
    if (result.status == NetworkDiagnosticStatus.FAILED) {
        Text(
            stringResource(
                RStrings.network_diagnostic_failed_stage,
                result.failureStage?.let { stageLabel(it) }
                    ?: stringResource(RStrings.network_diagnostic_not_observed),
            ),
        )
    }
    Text(
        stringResource(
            RStrings.network_diagnostic_route,
            result.actualConnection()?.route?.let { routeLabel(it) }
                ?: stringResource(RStrings.network_diagnostic_not_observed),
        ),
    )
    Text(
        listOfNotNull(
            result.downloadedBytes?.let { stringResource(RStrings.network_diagnostic_bytes, it.toString()) },
            stringResource(RStrings.network_diagnostic_time, result.elapsedMillis.toString()),
        ).joinToString(" · "),
    )
}

@Composable
private fun ResultDetails(result: NetworkDiagnosticResult) {
    Text(result.host, style = MaterialTheme.typography.labelLarge)
    if (result.events.isEmpty()) {
        Text(stringResource(RStrings.network_diagnostic_not_observed))
    }
    if (result.ipv6FailedBeforeIpv4()) {
        Text(stringResource(RStrings.network_diagnostic_ipv6_fallback))
    }
    result.events.forEach { EventDetails(it) }
}

@Composable
private fun EventDetails(event: NetworkDiagnosticEvent) {
    val status = when (event.status) {
        NetworkDiagnosticEventStatus.STARTED -> null
        NetworkDiagnosticEventStatus.SUCCEEDED -> stringResource(RStrings.status_completed)
        NetworkDiagnosticEventStatus.FAILED -> stringResource(RStrings.status_failed)
    }
    val eventLabel = listOfNotNull(
        stageLabel(event.stage),
        event.host ?: if (event.stage == NetworkDiagnosticStage.DNS) {
            stringResource(RStrings.network_diagnostic_dns_host_unknown)
        } else null,
        status,
        event.httpStatus?.let { "HTTP $it" },
        event.elapsedMillis?.let { stringResource(RStrings.network_diagnostic_time, it.toString()) },
        if (event.reusedConnection == true) stringResource(RStrings.network_diagnostic_reused) else null,
    ).joinToString(" · ")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(eventLabel, style = MaterialTheme.typography.bodyMedium)
        event.route?.let { route ->
            val label = if (event.stage == NetworkDiagnosticStage.CONNECT &&
                event.status == NetworkDiagnosticEventStatus.SUCCEEDED
            ) {
                RStrings.network_diagnostic_route
            } else {
                RStrings.network_diagnostic_attempted_route
            }
            Text(stringResource(label, routeLabel(route)), style = MaterialTheme.typography.bodySmall)
        }
        DnsEvidence(event)
    }
}

@Composable
private fun DnsDetails(result: NetworkDiagnosticResult) {
    val events = result.events.filter { it.stage == NetworkDiagnosticStage.DNS }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(result.host, style = MaterialTheme.typography.labelLarge)
        if (events.none { it.host == result.host }) {
            val connection = result.actualConnection()
            val reason = when {
                connection?.reusedConnection == true -> RStrings.network_diagnostic_reused
                connection?.route?.isProxy() == true -> RStrings.network_diagnostic_proxy_dns
                else -> RStrings.network_diagnostic_not_observed
            }
            Text(stringResource(reason), style = MaterialTheme.typography.bodyMedium)
        }
        events.forEach { EventDetails(it) }
    }
}

@Composable
private fun DnsEvidence(event: NetworkDiagnosticEvent) {
    if (event.stage != NetworkDiagnosticStage.DNS || event.status == NetworkDiagnosticEventStatus.STARTED) return
    if (event.resolvers.isNotEmpty()) {
        Text(
            event.resolvers.map { resolverLabel(it) }.distinct().joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        Text(
            stringResource(RStrings.network_diagnostic_dns_source_unknown),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (event.addresses.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            event.addresses.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    } else {
        Text(
            stringResource(RStrings.network_diagnostic_dns_addresses_unknown),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun dnsSummary(state: NetworkDiagnosticUiState): String = when {
    state.results.values.any { it.failureStage == NetworkDiagnosticStage.DNS } ->
        stringResource(RStrings.status_failed)
    state.results.values.any { result ->
        result.events.any {
            it.host != null && it.stage == NetworkDiagnosticStage.DNS &&
                it.status != NetworkDiagnosticEventStatus.STARTED
        }
    } -> stringResource(RStrings.network_diagnostic_dns_observed)
    state.results.isEmpty() -> pendingLabel(state.run)
    state.results.values.all { it.actualConnection()?.route?.isProxy() == true } ->
        stringResource(RStrings.network_diagnostic_proxy_dns)
    else -> stringResource(RStrings.network_diagnostic_not_observed)
}

private fun NetworkDiagnosticResult.actualConnection(): NetworkDiagnosticEvent? =
    events.lastOrNull {
        it.host == host && it.stage == NetworkDiagnosticStage.CONNECT &&
            it.status == NetworkDiagnosticEventStatus.SUCCEEDED && it.route != null
    }

private fun NetworkDiagnosticRoute.isProxy() = mode != NetworkDiagnosticRouteMode.DIRECT

private fun NetworkDiagnosticResult.ipv6FailedBeforeIpv4(): Boolean {
    val failedIpv6Hosts = mutableSetOf<String>()
    for (event in events) {
        val host = event.host ?: continue
        val route = event.route ?: continue
        val peer = route.peer ?: continue
        if (event.stage != NetworkDiagnosticStage.CONNECT || route.isProxy()) continue
        if (':' in peer && event.status == NetworkDiagnosticEventStatus.FAILED) {
            failedIpv6Hosts.add(host)
        }
        if (event.status == NetworkDiagnosticEventStatus.SUCCEEDED &&
            isIpv4Address(peer) && host in failedIpv6Hosts
        ) return true
    }
    return false
}

private fun isIpv4Address(value: String) =
    value.count { it == '.' } == 3 && value.all { it.isDigit() || it == '.' }

private fun webViewRouteUnavailable(setting: BypassSetting, currentPlatform: Platform): Boolean =
    when (currentPlatform) {
        is Platform.Android -> false
        is Platform.Desktop -> setting != BypassSetting.System
        is Platform.Apple -> setting == BypassSetting.Direct || setting is BypassSetting.ECH
    }

@Composable
private fun configuredRouteLabel(setting: BypassSetting): String = when (setting) {
    BypassSetting.System -> stringResource(RStrings.use_system_proxy)
    BypassSetting.Direct -> stringResource(RStrings.use_none)
    is BypassSetting.ECH -> stringResource(RStrings.use_ech)
    is BypassSetting.Proxy -> {
        val protocol = stringResource(
            when (setting.proxyType) {
                BypassSetting.Proxy.ProxyType.HTTP -> RStrings.protocol_http
                BypassSetting.Proxy.ProxyType.SOCKS -> RStrings.protocol_socks
            }
        )
        listOf(protocol, endpointLabel(setting.host, setting.port)).joinToString(" · ")
    }
}

@Composable
private fun routeLabel(route: NetworkDiagnosticRoute): String {
    val mode = stringResource(
        when (route.mode) {
            NetworkDiagnosticRouteMode.DIRECT -> RStrings.use_none
            NetworkDiagnosticRouteMode.HTTP -> RStrings.protocol_http
            NetworkDiagnosticRouteMode.SOCKS -> RStrings.protocol_socks
            NetworkDiagnosticRouteMode.PROXY -> RStrings.network_diagnostic_stage_proxy
        }
    )
    return listOfNotNull(mode, route.peer?.let { endpointLabel(it, route.port) }).joinToString(" · ")
}

private fun endpointLabel(host: String, port: Int?): String {
    if (port == null) return host
    val address = if (':' in host && !host.startsWith('[')) "[$host]" else host
    return "$address:$port"
}

@Composable
private fun pendingLabel(run: NetworkDiagnosticRun): String = stringResource(
    when (run) {
        NetworkDiagnosticRun.IDLE -> RStrings.network_diagnostic_pending
        NetworkDiagnosticRun.RUNNING -> RStrings.status_running
        NetworkDiagnosticRun.CANCELED -> RStrings.network_diagnostic_canceled
        NetworkDiagnosticRun.COMPLETED -> RStrings.network_diagnostic_not_observed
    }
)

@Composable
private fun stageLabel(stage: NetworkDiagnosticStage): String = stringResource(
    when (stage) {
        NetworkDiagnosticStage.DNS -> RStrings.network_diagnostic_dns
        NetworkDiagnosticStage.CONNECT -> RStrings.network_diagnostic_stage_connect
        NetworkDiagnosticStage.PROXY -> RStrings.network_diagnostic_stage_proxy
        NetworkDiagnosticStage.TLS -> RStrings.network_diagnostic_stage_tls
        NetworkDiagnosticStage.HTTP -> RStrings.protocol_http
        NetworkDiagnosticStage.BODY -> RStrings.network_diagnostic_stage_body
    }
)

@Composable
private fun resolverLabel(resolver: NetworkDiagnosticResolver): String = stringResource(
    when (resolver) {
        NetworkDiagnosticResolver.SYSTEM -> RStrings.network_diagnostic_dns_system
        NetworkDiagnosticResolver.DOH -> RStrings.network_diagnostic_dns_doh
        NetworkDiagnosticResolver.FALLBACK -> RStrings.network_diagnostic_dns_fallback
    }
)

@Composable
private fun failureLabel(failure: NetworkDiagnosticFailure): String = stringResource(
    when (failure) {
        NetworkDiagnosticFailure.TIMEOUT -> RStrings.network_diagnostic_failure_timeout
        NetworkDiagnosticFailure.TRANSPORT -> RStrings.network_diagnostic_failure_transport
        NetworkDiagnosticFailure.PROXY_AUTH -> RStrings.network_diagnostic_failure_proxy_auth
        NetworkDiagnosticFailure.PROXY_UNSUPPORTED_TARGET -> RStrings.network_diagnostic_failure_proxy_target
        NetworkDiagnosticFailure.HTTP_STATUS -> RStrings.network_diagnostic_failure_http
        NetworkDiagnosticFailure.RATE_LIMITED -> RStrings.network_diagnostic_failure_rate_limited
        NetworkDiagnosticFailure.UNEXPECTED_RESPONSE -> RStrings.network_diagnostic_failure_response
        NetworkDiagnosticFailure.REDIRECT -> RStrings.network_diagnostic_failure_redirect
        NetworkDiagnosticFailure.SAMPLE_NOT_FOUND -> RStrings.network_diagnostic_failure_sample
        NetworkDiagnosticFailure.SIZE_LIMIT -> RStrings.network_diagnostic_failure_size
        NetworkDiagnosticFailure.INCOMPLETE_BODY -> RStrings.network_diagnostic_failure_incomplete
    }
)
