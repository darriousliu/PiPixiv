package com.mrl.pixiv.setting.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.LocalToaster
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.setting.SettingViewModel
import com.mrl.pixiv.setting.network.components.BypassSettingEditor
import com.mrl.pixiv.setting.network.components.PictureSourceWidget
import com.mrl.pixiv.strings.network_setting
import com.mrl.pixiv.strings.restart_app_to_take_effect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NetworkSettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.network_setting))
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
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .imePadding()
        ) {
            val itemModifier = Modifier.padding(horizontal = 8.dp)

            BypassSettingEditor(
                modifier = itemModifier,
                bypassSetting = userPreference.bypassSetting,
                onUpdate = { setting -> viewModel.updateBypassSetting(setting) }
            )

            PictureSourceWidget(
                modifier = itemModifier,
                currentSelected = userPreference.imageHost,
                savePictureSourceHost = { host ->
                    viewModel.savePictureSourceHost(host)
                    toaster.show(RStrings.restart_app_to_take_effect)
                }
            )
        }
    }
}




