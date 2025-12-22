package com.mrl.pixiv.setting.appdata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.adaptiveFileSize1
import com.mrl.pixiv.common.util.calculateSize
import com.mrl.pixiv.common.util.deleteRecursively
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.strings.app_data
import com.mrl.pixiv.strings.cache_cleared
import com.mrl.pixiv.strings.clear_cache
import com.mrl.pixiv.strings.export_data
import com.mrl.pixiv.strings.import_data
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.list
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
fun AppDataScreen(
    modifier: Modifier = Modifier,
    navigationManager: NavigationManager = koinInject(),
    viewModel: AppDataViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val state = viewModel.asState()

    var trigger by remember { mutableIntStateOf(0) }
    val cacheDirSize = remember(trigger) { FileKit.cacheDir.calculateSize().adaptiveFileSize1() }

    val exportLauncher = rememberFileSaverLauncher { file ->
        file?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("zip"))
    ) { file ->
        file?.let { viewModel.importData(it) }
    }


    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.app_data))
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationManager::popBackStack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            MigrationCard(
                state = state,
                migrateData = viewModel::migrateData,
                viewModel = viewModel
            )

            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.export_data))
                },
                modifier = Modifier.clickable {
                    val fileName = "pixiv_data_backup_${
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            .format(LocalDateTime.Formats.ISO)
                    }"
                    exportLauncher.launch(fileName, "zip")
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Upload,
                        contentDescription = null
                    )
                }
            )

            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.import_data))
                },
                modifier = Modifier.clickable {
                    importLauncher.launch()
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null
                    )
                }
            )

            ListItem(
                headlineContent = {
                    Text(text = stringResource(RStrings.clear_cache, cacheDirSize))
                },
                modifier = Modifier.clickable {
                    scope.launch(Dispatchers.IO) {
                        val dirSize = FileKit.cacheDir.calculateSize().adaptiveFileSize1()
                        FileKit.cacheDir.list().forEach {
                            it.deleteRecursively()
                        }
                        ToastUtil.safeShortToast(RStrings.cache_cleared, dirSize)
                        trigger++
                    }
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null
                    )
                }
            )
        }
    }


}

@Composable
expect fun MigrationCard(
    state: AppDataState,
    migrateData: () -> Unit,
    viewModel: AppDataViewModel,
)
