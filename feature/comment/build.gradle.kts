plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.comment"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            // Paging
            implementation(androidx.bundles.paging)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            implementation(kotlinx.datetime)
        }
    }
}
