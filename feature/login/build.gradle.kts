plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.login"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))

    // Navigation3
    implementation(composes.androidx.navigation3.runtime)
    implementation(libs.compose.webview.multiplatform)
    implementation(libs.okio)
}