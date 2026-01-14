package com.mrl.pixiv.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.mrl.pixiv.common.util.VIEW_CLICK_INTERVAL_TIME
import com.mrl.pixiv.common.util.currentTimeMillis

@Composable
fun rememberThrottleClick(
    intervalTime: Long = VIEW_CLICK_INTERVAL_TIME,
    onClick: () -> Unit
): () -> Unit {
    // 使用remember函数记录上次点击的时间
    var lastClickTime by remember { mutableLongStateOf(value = 0L) }
    val updatedOnClick by rememberUpdatedState {
        val currentTimeMillis = currentTimeMillis()
        // 判断点击间隔,如果在间隔内则不回调
        if (currentTimeMillis - intervalTime >= lastClickTime) {
            onClick()
            lastClickTime = currentTimeMillis
        }
    }
    return updatedOnClick
}