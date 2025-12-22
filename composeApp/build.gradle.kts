plugins {
    id("pixiv.multiplatform.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv"
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":common:data"))
                implementation(project(":common:network"))
                implementation(project(":common:repository"))
                implementation(project(":common:ui"))
                implementation(project(":common:core"))
//                rootDir.resolve("feature").listFiles()?.filter { it.isDirectory }?.forEach {
//                    implementation(project(":feature:${it.name}"))
//                }
                implementation(composes.bundles.navigation3)
                // Coil3
                implementation(project.dependencies.platform(libs.coil3.bom))
                implementation(libs.bundles.coil3)
            }
        }
        androidMain {
            dependencies {
                // Navigation3
                implementation(composes.bundles.navigation3.android)
            }
        }
        iosMain {
            dependencies {

            }
        }
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}