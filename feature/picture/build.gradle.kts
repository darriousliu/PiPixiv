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
            implementation(project(":feature:manga"))
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
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.ktor.client.mock)
            implementation(libs.kotlinx.ktor.client.content.negotiation)
            implementation(libs.kotlinx.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.mmkv.kotlin)
            implementation("org.jetbrains.compose.ui:ui-test:${libs.versions.composeMultiplatform.get()}")
            implementation(compose.desktop.currentOs)
            runtimeOnly(when {
                System.getProperty("os.name") == "Mac OS X" -> libs.mmkv.kotlin.nativelib.macos
                System.getProperty("os.name").startsWith("Windows") -> libs.mmkv.kotlin.nativelib.windows
                else -> libs.mmkv.kotlin.nativelib.linux
            })
        }
    }
}
