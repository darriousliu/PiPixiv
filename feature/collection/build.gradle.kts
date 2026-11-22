plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.collection"
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
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.bundles.compose.navigation3)
            implementation(libs.compose.jetbrains.ui.test)
        }
    }
}
