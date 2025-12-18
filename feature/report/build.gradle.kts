plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.report"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))
}