plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.home"
}

dependencies {
    implementation(project(":common:repository"))
    implementation(project(":lib_common"))

}