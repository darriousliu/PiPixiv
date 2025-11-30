plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.profile"
}

dependencies {
    implementation(project(":lib_common"))

}