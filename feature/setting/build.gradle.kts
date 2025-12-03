plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.setting"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))

    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
}
