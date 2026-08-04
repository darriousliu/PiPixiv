@file:OptIn(
    org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class,
    org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl::class,
)

plugins {
    alias(kotlinx.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(25)

    iosArm64()
    iosSimulatorArm64()

    swiftExport {
        moduleName = "PiPixivKit"
        flattenPackage = "com.mrl.pixiv.ios"

        export(project(":common:platform-api")) {
            moduleName = "PiPixivPlatform"
            flattenPackage = "com.mrl.pixiv.common"
        }
    }

    sourceSets {
        iosMain.dependencies {
            implementation(project(":composeApp"))
            api(project(":common:platform-api"))
        }
    }
}
