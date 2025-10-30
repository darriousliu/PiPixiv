plugins {
    id("pixiv.android.library.compose")
}

android {
    namespace = "com.mrl.pixiv.feature"
}

dependencies {
    implementation(project(":lib_common"))

    if (project.findProperty("applyFirebasePlugins") == "true") {
        ksp(libs.koin.ksp.compiler)
    }
}