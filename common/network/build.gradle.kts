plugins {
    id("pixiv.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.network"
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val androidJvmMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:core"))

            // Serialization
            implementation(libs.bundles.kotlinx.serialization)
            // Ktor
            implementation(libs.bundles.kotlinx.ktor)

            // DateTime
            implementation(libs.kotlinx.datetime)
        }

        androidJvmMain.dependencies {
            implementation(project.dependencies.platform(libs.okhttp.bom))
            implementation(libs.okhttp)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.kotlinx.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.ktor.client.okhttp)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.okhttp.mockwebserver)
            implementation(libs.okhttp.tls)
        }
    }
}
