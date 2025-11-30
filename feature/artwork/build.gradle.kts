plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.artwork"
}

dependencies {
    implementation(project(":common:data"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":lib_common"))

    // Paging
    implementation(androidx.bundles.paging)
}