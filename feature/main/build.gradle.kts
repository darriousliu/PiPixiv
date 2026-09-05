plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.main"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))
            implementation(project(":feature:collection"))
            implementation(project(":feature:follow"))

            // Paging
            implementation(libs.bundles.androidx.paging)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            implementation(libs.kotlinx.datetime)
            implementation(libs.html.converter)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
