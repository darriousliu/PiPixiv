plugins {
    id("pixiv.android.library.compose")
    alias(kotlinx.plugins.serialization)
}

android {
    namespace = "com.mrl.pixiv.common.data"
}

dependencies {
    // Serialization
    implementation(kotlinx.bundles.serialization)
    implementation(kotlinx.datetime)
}