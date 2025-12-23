plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.picture"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:network"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            // Paging
            implementation(androidx.bundles.paging)

            // Navigation3
            implementation(composes.bundles.navigation3)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            // FileKit
            implementation(libs.filekit.core)
            // Permission
            implementation(libs.calf.permissions)
        }

        androidMain.dependencies {
            // Navigation3
            implementation(composes.bundles.navigation3.android)
        }
    }
}
