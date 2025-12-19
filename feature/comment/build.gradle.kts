plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.comment"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))

    // Paging
    implementation(androidx.bundles.paging)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    implementation(kotlinx.datetime)
}