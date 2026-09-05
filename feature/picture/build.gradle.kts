plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.picture"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:data"))
            implementation(project(":common:datasource-local"))
            implementation(project(":common:network"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            // Paging
            implementation(libs.bundles.androidx.paging)

            // Navigation3
            implementation(libs.bundles.compose.navigation3)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            // FileKit
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.html.converter)
            implementation(libs.compose.navigationevent.compose)
        }
        androidMain.dependencies {
            // Navigation3
            implementation(libs.bundles.compose.navigation3.android)
            // Permission
            implementation(libs.compose.accompanist.permissions)
        }
    }
}
