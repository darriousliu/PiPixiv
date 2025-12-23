package com.mrl.pixiv.picture

import android.os.Build
import com.mohamedrejeb.calf.permissions.Permission

actual val permission: Permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Permission.ReadImage
} else {
    Permission.ReadStorage
}