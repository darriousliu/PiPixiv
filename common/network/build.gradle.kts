plugins {
    id("pixiv.android.library")
    alias(kotlinx.plugins.serialization)
}

android {
    namespace = "com.mrl.pixiv.common.network"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":lib_common"))

    // Serialization
    implementation(kotlinx.bundles.serialization)
    // Ktor
    implementation(kotlinx.bundles.ktor)
    // DateTime
    implementation(kotlinx.datetime)
}