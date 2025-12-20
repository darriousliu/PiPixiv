import com.mrl.pixiv.buildsrc.composeDependencies

plugins {
    id("pixiv.multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
}

kotlin {
    composeDependencies()
}
