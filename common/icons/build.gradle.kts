import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvmToolchain(25)

    android {
        namespace = "com.mrl.pixiv.common.icons"
        compileSdk {
            version = release(37) {
                minorApiLevel = 2
            }
        }
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    sourceSets.commonMain.dependencies {
        api(libs.compose.jetbrains.ui)
    }
}
