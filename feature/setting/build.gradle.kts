plugins {
    id("pixiv.multiplatform.compose")
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.setting"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)

            implementation(kotlinx.serialization.json)
            implementation(kotlinx.datetime)
            implementation(libs.okio)
            implementation(androidx.room.runtime)
            implementation(libs.html.converter)
        }
    }
}

