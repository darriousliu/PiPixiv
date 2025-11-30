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
    implementation(project(":lib_common"))

    // Paging
    implementation(androidx.bundles.paging)
    // GIF encoder
    implementation(libs.gifkt)
    // Permission
    implementation(compose.bundles.accompanist)
    // Navigation3
    implementation(compose.bundles.navigation3)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
}