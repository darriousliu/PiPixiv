import com.mrl.pixiv.buildsrc.configureAndroidCompose
import com.mrl.pixiv.buildsrc.configureKotlinAndroid

plugins {
    id("com.android.application")
    kotlin("plugin.compose")
    id("io.insert-koin.compiler.plugin")
}

android {
    defaultConfig {
        targetSdk = 37
    }
    configureKotlinAndroid(this)
    configureAndroidCompose(this)
}
