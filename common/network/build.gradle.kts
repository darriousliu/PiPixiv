plugins {
    id("pixiv.multiplatform")
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidLibrary {
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
            implementation(kotlinx.bundles.serialization)
            // Ktor
            implementation(kotlinx.bundles.ktor)

            // DateTime
            implementation(kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(kotlinx.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(kotlinx.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(kotlinx.ktor.client.okhttp)
        }
    }
}