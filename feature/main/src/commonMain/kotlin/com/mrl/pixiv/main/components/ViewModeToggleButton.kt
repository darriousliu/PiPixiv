package com.mrl.pixiv.main.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mrl.pixiv.common.data.AppViewMode
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.switch_to_illust_mode
import com.mrl.pixiv.strings.switch_to_novel_mode
import org.jetbrains.compose.resources.stringResource

/**
 * 视图模式切换按钮
 * 用于在插画模式和小说模式之间切换
 *
 * @param currentMode 当前的视图模式
 * @param onModeChange 模式切换回调
 * @param modifier Modifier
 */
@Composable
fun ViewModeToggleButton(
    currentMode: AppViewMode,
    onModeChange: (AppViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = {
            val newMode = when (currentMode) {
                AppViewMode.ILLUST -> AppViewMode.NOVEL
                AppViewMode.NOVEL -> AppViewMode.ILLUST
            }
            onModeChange(newMode)
        },
        modifier = modifier
    ) {
        when (currentMode) {
            AppViewMode.ILLUST -> {
                // 当前是插画模式，显示切换到小说模式的图标
                Icon(
                    imageVector = Icons.Rounded.Book,
                    contentDescription = stringResource(RStrings.switch_to_novel_mode)
                )
            }

            AppViewMode.NOVEL -> {
                // 当前是小说模式，显示切换到插画模式的图标
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = stringResource(RStrings.switch_to_illust_mode)
                )
            }
        }
    }
}
