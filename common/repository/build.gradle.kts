plugins {
    id("pixiv.multiplatform.compose")
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.common.repository"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:network"))
            api(project(":common:datasource-local"))
            implementation(project(":common:datasource-remote"))
            implementation(project(":common:core"))

            // Paging
            implementation(androidx.bundles.paging)
            // Serialization
            implementation(kotlinx.bundles.serialization)
            // Ktor
            implementation(kotlinx.bundles.ktor)

            // Coil
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            // GIF encoder
            implementation(libs.gifkt)
            implementation(libs.filekit.core)
        }

        androidMain.dependencies {
            // WorkManager
            implementation(androidx.bundles.workmanager)
        }
    }
}
