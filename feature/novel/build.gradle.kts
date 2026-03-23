plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.novel"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            // Paging
            implementation(androidx.bundles.paging)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            implementation(kotlinx.serialization.json)
            implementation(libs.filekit.dialogs)
            implementation(libs.okio)
        }
    }
}
