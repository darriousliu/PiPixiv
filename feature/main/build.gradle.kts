plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.main"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))
    implementation(project(":feature:collection"))
    implementation(project(":feature:follow"))

    // Paging
    implementation(androidx.bundles.paging)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    // Navigation3
    implementation(compose.bundles.navigation3)
}