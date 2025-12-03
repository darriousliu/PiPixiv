plugins {
    id("pixiv.android.library.compose")
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.ktorfit)
}

android {
    namespace = "com.mrl.pixiv.common.repository"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:network"))
    api(project(":common:datasource-local"))
    implementation(project(":common:datasource-remote"))
    implementation(project(":common:core"))

    // Paging
    implementation(androidx.bundles.paging)
    // Serialization
    implementation(kotlinx.bundles.serialization)
    // Ktor
    implementation(kotlinx.bundles.ktor)

    // WorkManager
    implementation(androidx.bundles.workmanager)
    // Coil
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    // GIF encoder
    implementation(libs.gifkt)
}
