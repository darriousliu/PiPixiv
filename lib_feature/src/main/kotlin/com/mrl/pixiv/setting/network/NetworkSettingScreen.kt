package com.mrl.pixiv.setting.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.compose.ui.SettingItem
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.viewmodel.activityKoinViewModel
import com.mrl.pixiv.setting.SettingAction
import com.mrl.pixiv.setting.SettingViewModel
import com.mrl.pixiv.setting.network.components.PictureSourceWidget
import org.koin.compose.koinInject

@Composable
fun NetworkSettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = activityKoinViewModel(),
    navigationManager: NavigationManager = koinInject(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RString.network_setting))
                },
                navigationIcon = {
                    IconButton(onClick = navigationManager::popBackStack) {
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
                .padding(horizontal = 16.dp)
                .imePadding()
        ) {
            SettingItem(content = {
                Column {
                    Text(
                        text = stringResource(RString.enable_bypass_sniffing),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(RString.close_to_use_ip_directly),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = userPreference.enableBypassSniffing,
                    onCheckedChange = { viewModel.dispatch(SettingAction.SwitchBypassSniffing) }
                )
            })
            PictureSourceWidget(
                currentSelected = userPreference.imageHost,
                savePictureSourceHost = {
                    viewModel.dispatch(SettingAction.SavePictureSourceHost(it))
                    ToastUtil.safeShortToast(RString.restart_app_to_take_effect)
                }
            )
        }
    }
}
