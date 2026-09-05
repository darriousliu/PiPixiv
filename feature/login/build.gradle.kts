plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.login"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            // Navigation3
            implementation(libs.compose.androidx.navigation3.runtime)
            implementation(libs.compose.webview.multiplatform)
            implementation(libs.okio)
            implementation(libs.kotlinx.ktor.client.core)
            implementation(libs.kotlinx.ktor.client.content.negotiation)
            implementation(libs.kotlinx.ktor.serialization.kotlinx.json)
            implementation(libs.mp.stools)
            implementation(libs.html.converter)
        }

        jvmMain.dependencies {
            implementation(libs.filekit.core)
        }
    }
}
