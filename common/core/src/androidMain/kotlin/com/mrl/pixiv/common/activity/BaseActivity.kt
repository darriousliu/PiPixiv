package com.mrl.pixiv.common.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable

abstract class BaseActivity : AppCompatActivity() {
    protected val TAG = this::class.simpleName

    @Composable
    abstract fun BuildContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BuildContent() }
    }
}