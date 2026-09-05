plugins {
    id("pixiv.multiplatform.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.repository"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:ai"))
            implementation(project(":common:data"))
            implementation(project(":common:network"))
            implementation(project(":common:datasource-local"))
            implementation(project(":common:datasource-remote"))
            implementation(project(":common:core"))

            // Paging
            implementation(libs.bundles.androidx.paging)
            // Serialization
            implementation(libs.bundles.kotlinx.serialization)
            // Ktor
            implementation(libs.bundles.kotlinx.ktor)

            // Coil
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            // GIF encoder
            implementation(libs.gifkt)
            implementation(libs.filekit.core)
            implementation(libs.okio)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project.dependencies.platform(libs.kotlinx.coroutines.bom))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.ktor.client.mock)
        }

        androidMain.dependencies {
            // WorkManager
            implementation(libs.bundles.androidx.workmanager)
        }
    }
}
