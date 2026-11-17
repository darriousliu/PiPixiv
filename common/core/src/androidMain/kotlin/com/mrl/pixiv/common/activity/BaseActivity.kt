package com.mrl.pixiv.common.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.mrl.pixiv.common.util.LegacyStoragePermission
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

abstract class BaseActivity : AppCompatActivity() {
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        LegacyStoragePermission::onResult,
    )
    protected val TAG = this::class.simpleName

    @Composable
    abstract fun BuildContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        LegacyStoragePermission.bind(storagePermissionLauncher)
        enableEdgeToEdge()
        setContent { BuildContent() }
    }

    override fun onDestroy() {
        LegacyStoragePermission.unbind(storagePermissionLauncher, isChangingConfigurations)
        super.onDestroy()
    }
}
