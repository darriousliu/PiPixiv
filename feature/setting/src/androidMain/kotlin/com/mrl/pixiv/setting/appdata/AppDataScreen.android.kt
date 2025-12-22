package com.mrl.pixiv.setting.appdata

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.SideEffect
import com.mrl.pixiv.strings.cancel
import com.mrl.pixiv.strings.confirm
import com.mrl.pixiv.strings.migrate_confirm_desc
import com.mrl.pixiv.strings.migrate_confirm_title
import com.mrl.pixiv.strings.migrate_data_desc
import com.mrl.pixiv.strings.migrate_data_title
import com.mrl.pixiv.strings.migrating
import com.mrl.pixiv.strings.permission_rationale
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun MigrationCard(
    state: AppDataState,
    migrateData: () -> Unit,
    viewModel: AppDataViewModel,
) {
    var showMigrationConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            migrateData()
        } else {
            ToastUtil.safeShortToast(RStrings.permission_rationale)
        }
    }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            migrateData()
        } else {
            ToastUtil.safeShortToast(RStrings.permission_rationale)
        }
    }


    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is RequestPermissionEffect -> {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(effect.intentSender).build()
                    intentSenderLauncher.launch(intentSenderRequest)
                }

                is SideEffect.Error -> {
                    effect.throwable.printStackTrace()
                }
            }
        }
    }

    AnimatedVisibility(
        visible = state.oldImageCount > 0,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { showMigrationConfirmDialog = true },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = stringResource(RStrings.migrate_data_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(RStrings.migrate_data_desc, state.oldImageCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    if (showMigrationConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMigrationConfirmDialog = false },
            title = { Text(text = stringResource(RStrings.migrate_confirm_title)) },
            text = { Text(text = stringResource(RStrings.migrate_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMigrationConfirmDialog = false
                        // Check permission before migration
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                ) {
                    Text(text = stringResource(RStrings.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMigrationConfirmDialog = false }) {
                    Text(text = stringResource(RStrings.cancel))
                }
            }
        )
    }

    if (state.isMigrating) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(
                            RStrings.migrating,
                            state.migratedCount,
                            state.oldImageCount
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearWavyProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (state.isLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.loadingMessage != null) {
                        Text(
                            text = stringResource(state.loadingMessage),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

}