plugins {
    alias(kotlinx.plugins.kotlinMultiplatform)
    alias(androidx.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv"
        compileSdk = 36
        minSdk = 26
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
            }
        }
        androidMain {
            dependencies {

            }
        }
        iosMain {
            dependencies {

            }
        }
    }
}