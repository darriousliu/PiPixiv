plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.latest"
}

dependencies {
    implementation(project(":lib_common"))

}