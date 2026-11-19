plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.follow"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))
            implementation(project(":common:icons"))

            // Paging
            implementation(libs.bundles.androidx.paging)
        }
    }
}
