plugins {
    id("pixiv.android.library.compose")
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.ktorfit)
}

android {
    namespace = "com.mrl.pixiv.common.datasource.remote"
}

dependencies {
    implementation(project(":common:data"))

    // Serialization
    implementation(kotlinx.bundles.serialization)
    // Ktorfit
    implementation(kotlinx.ktorfit.lite)
}