package com.mrl.pixiv.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.compose.ui.ContentErrorState
import com.mrl.pixiv.common.icons.Icons
import com.mrl.pixiv.common.icons.rounded.Storage
import com.mrl.pixiv.common.logs.AppLogStore
import com.mrl.pixiv.common.repository.recovery.MigrationReason
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.applog.exportAppLogs
import com.mrl.pixiv.strings.migration_database_failure
import com.mrl.pixiv.strings.migration_export_logs
import com.mrl.pixiv.strings.migration_failure
import com.mrl.pixiv.strings.migration_progress
import com.mrl.pixiv.strings.migration_storage_failure
import com.mrl.pixiv.strings.retry
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MigrationSplash(
    state: MigrationState,
    viewModel: SplashViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    if (state == MigrationState.Slow) {
        LaunchedEffect(viewModel) { viewModel.markSlowUiShown() }
    }
    MigrationSplashContent(
        state = state,
        onRetry = { viewModel.dispatch(SplashAction.RetryMigration) },
        onExport = {
            scope.launch {
                val logs = AppLogStore.state.value
                exportAppLogs(logs.previous + logs.current)
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun MigrationSplashContent(
    state: MigrationState,
    onRetry: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state) {
            MigrationState.Slow -> Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(RStrings.migration_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            is MigrationState.Blocked -> Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                ContentErrorState(
                    onRetry = onRetry,
                    message = stringResource(RStrings.migration_failure),
                    description = when (state.reason) {
                        MigrationReason.Unknown -> null
                        MigrationReason.StorageSpace -> stringResource(RStrings.migration_storage_failure)
                        else -> stringResource(RStrings.migration_database_failure)
                    },
                    icon = Icons.Rounded.Storage,
                    action = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledTonalButton(onClick = onRetry) { Text(stringResource(RStrings.retry)) }
                            TextButton(onClick = onExport) { Text(stringResource(RStrings.migration_export_logs)) }
                        }
                    },
                )
            }

            else -> Unit
        }
    }
}
