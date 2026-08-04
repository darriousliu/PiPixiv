import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(kotlinx.plugins.kotlinMultiplatform)
    alias(androidx.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvmToolchain(25)

    android {
        namespace = "com.mrl.pixiv.common.platform.api"
        compileSdk = 37
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
}
