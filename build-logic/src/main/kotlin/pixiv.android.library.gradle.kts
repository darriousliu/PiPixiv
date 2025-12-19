import com.mrl.pixiv.buildsrc.configureKotlinAndroid
import com.mrl.pixiv.buildsrc.disableUnnecessaryAndroidTests

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp")
}

android {
    configureKotlinAndroid(this)

    androidComponents {
        disableUnnecessaryAndroidTests(project)
    }
}