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
    implementation(project(":common:datasource-local"))
    implementation(project(":common:datasource-remote"))
    implementation(project(":lib_common"))

    // Paging
    implementation(androidx.bundles.paging)
    // Serialization
    implementation(kotlinx.bundles.serialization)
    // Ktor
    implementation(kotlinx.bundles.ktor)
}