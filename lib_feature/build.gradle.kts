plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.feature"
}

dependencies {
    implementation(project(":lib_common"))

    ksp(libs.koin.ksp.compiler)
}