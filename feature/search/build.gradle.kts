plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.search"
}

dependencies {
    implementation(project(":common:repository"))
    implementation(project(":common:core"))

}