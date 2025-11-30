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
    implementation(project(":lib_common"))

    implementation(libs.compose.webview.multiplatform)
    implementation(libs.okio)
}