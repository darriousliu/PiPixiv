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
        val androidJvmMain = create("androidJvmMain") {
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
            implementation(libs.kotlinx.ktor.client.okhttp)
            // 共享 Java API；Android 运行时使用 AAR 中对应 ABI 的原生库。
            compileOnly(libs.conscrypt.openjdk)
        }

        androidMain.dependencies {
            implementation(libs.conscrypt.android)
        }

        jvmMain.dependencies {
            implementation(libs.conscrypt.openjdk)
        }

        iosMain.dependencies {
            implementation(libs.kotlinx.ktor.client.darwin)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.okhttp.mockwebserver)
            implementation(libs.okhttp.tls)
        }
    }
}
