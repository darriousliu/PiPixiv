plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.login"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            // Navigation3
            implementation(composes.androidx.navigation3.runtime)
            implementation(libs.compose.webview.multiplatform)
            implementation(libs.okio)
            implementation(kotlinx.ktor.client.core)
        }
    }
}
