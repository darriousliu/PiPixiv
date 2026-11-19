plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.ui"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:core"))
            implementation(project(":common:icons"))
            implementation(libs.html.converter)

            // Paging
            implementation(libs.bundles.androidx.paging)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            // Navigation3
            implementation(libs.bundles.compose.navigation3)
            // Toast
            implementation(libs.sonner)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:${libs.versions.composeMultiplatform.get()}")
            implementation(compose.desktop.currentOs)
        }

        androidMain.dependencies {
            // Navigation3
            implementation(libs.bundles.compose.navigation3.android)
        }
    }
}
