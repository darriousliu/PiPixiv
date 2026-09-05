plugins {
    id("pixiv.multiplatform.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.setting"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":lib_strings"))
            implementation(project(":common:ai"))
            implementation(project(":common:data"))
            implementation(project(":common:datasource-local"))
            implementation(project(":common:repository"))
            implementation(project(":common:ui"))
            implementation(project(":common:core"))

            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
            implementation(libs.androidx.room.runtime)
            implementation(libs.html.converter)
            implementation(libs.bundles.filekit)
            implementation(libs.bundles.markdown)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
