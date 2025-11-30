package com.mrl.pixiv.common.util

import android.widget.Toast
import androidx.annotation.StringRes
import com.mrl.pixiv.common.coroutine.launchCatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

object ToastUtil : CoroutineScope by MainScope() {
    fun safeShortToast(@StringRes strId: Int) {
        launchCatch {
            val text = AppUtil.appContext.getString(strId)
            Toast.makeText(AppUtil.appContext, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun safeLongToast(@StringRes strId: Int) {
        launchCatch {
            val text = AppUtil.appContext.getString(strId)
            Toast.makeText(AppUtil.appContext, text, Toast.LENGTH_LONG).show()
        }
    }

    fun safeShortToast(text: String) {
        launchCatch {
            Toast.makeText(AppUtil.appContext, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun safeLongToast(text: String) {
        launchCatch {
            Toast.makeText(AppUtil.appContext, text, Toast.LENGTH_LONG).show()
        }
    }
}