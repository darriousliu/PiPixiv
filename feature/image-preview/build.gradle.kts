plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.image.preview"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:core"))
            implementation(project(":common:ui"))

            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            implementation(libs.zoomimage.compose.coil3)
            // Navigation3
            implementation(libs.bundles.compose.navigation3)
        }

        androidMain.dependencies {
            // Navigation3
            implementation(libs.bundles.compose.navigation3.android)
        }
    }
}
