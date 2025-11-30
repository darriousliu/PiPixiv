plugins {
    id("pixiv.android.library.compose")
    alias(kotlinx.plugins.serialization)
}

android {
    namespace = "com.mrl.pixiv.common.datasource.local"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":lib_common"))

    // Serialization
    implementation(kotlinx.bundles.serialization)
}