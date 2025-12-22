plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.follow"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            // Paging
            implementation(androidx.bundles.paging)
        }
    }
}
