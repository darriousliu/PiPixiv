plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.report"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))
        }
    }
}
