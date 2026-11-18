package com.mrl.pixiv.setting.network

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.data.setting.UserPreference.BypassSetting
import com.mrl.pixiv.common.repository.requireUserPreferenceFlow
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.router.currentNavigationManager
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.isIOS
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.setting.SettingViewModel
import com.mrl.pixiv.setting.components.SettingPageColumn
import com.mrl.pixiv.setting.network.components.BypassSettingEditor
import com.mrl.pixiv.setting.network.components.PictureSourceWidget
import com.mrl.pixiv.strings.network_setting
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NetworkSettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
    navigationManager: NavigationManager = currentNavigationManager(),
) {
    val userPreference by requireUserPreferenceFlow.collectAsStateWithLifecycle()
    NetworkSettingContent(
        bypassSetting = userPreference.bypassSetting,
        pictureSourceHost = userPreference.imageHost,
        onBypassSettingChange = viewModel::updateBypassSetting,
        onPictureSourceHostChange = viewModel::savePictureSourceHost,
        onBack = navigationManager::popBackStack,
        supportsEch = !platform.isIOS(),
        modifier = modifier,
    )
}

@Composable
internal fun NetworkSettingContent(
    bypassSetting: BypassSetting,
    pictureSourceHost: String,
    onBypassSettingChange: (BypassSetting) -> Unit,
    onPictureSourceHostChange: (String) -> Unit,
    onBack: () -> Unit,
    supportsEch: Boolean,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(RStrings.network_setting))
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingPageColumn(innerPadding) {
            BypassSettingEditor(
                bypassSetting = bypassSetting,
                onUpdate = onBypassSettingChange,
                supportsEch = supportsEch,
            )
            PictureSourceWidget(
                currentSelected = pictureSourceHost,
                savePictureSourceHost = onPictureSourceHostChange,
            )
        }
    }
}
