plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.common.ui"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:core"))

            // Paging
            implementation(androidx.bundles.paging)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            // Navigation3
            implementation(composes.bundles.navigation3)
            // Toast
            implementation(libs.sonner)
        }

        androidMain.dependencies {
            // Navigation3
            implementation(composes.bundles.navigation3.android)
        }
    }
}