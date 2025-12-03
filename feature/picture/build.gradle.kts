plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.picture"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:network"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))

    // Paging
    implementation(androidx.bundles.paging)
    // Permission
    implementation(compose.bundles.accompanist)
    // Navigation3
    implementation(compose.bundles.navigation3)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
}