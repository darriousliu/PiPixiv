package com.mrl.pixiv.buildsrc

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun KotlinMultiplatformAndroidLibraryTarget.configureKotlinMultiplatform() {
    compileSdk {
        version = release(37)
    }
    minSdk = 26

    withDeviceTest {
        instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

internal fun KotlinMultiplatformExtension.commonDependencies() {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    sourceSets.apply {
        commonMain.dependencies {
            // Lifecycle
            implementation(libs.findBundle("compose-lifecycle").get())
            // Coroutines
            implementation(
                project.dependencies.platform(
                    libs.findLibrary("kotlinx-coroutines-bom").get()
                )
            )
            implementation(libs.findLibrary("kotlinx-coroutines-core").get())
            // Koin
            implementation(libs.findBundle("koin").get())

            // Logger
            implementation(libs.findLibrary("kermit").get())
        }
        androidMain.dependencies {
            implementation(libs.findBundle("androidx").get())
            // Coroutines
            implementation(libs.findLibrary("kotlinx-coroutines-android").get())
        }
        iosMain.dependencies {

        }
        jvmMain.dependencies {
            // Coroutines
            implementation(libs.findLibrary("kotlinx-coroutines-swing").get())
        }
    }
    project.dependencies {
        kspAndroid(libs.findLibrary("koin-ksp-compiler").get())
        kspIos(libs.findLibrary("koin-ksp-compiler").get())
        kspJvm(libs.findLibrary("koin-ksp-compiler").get())
        kspCommonMainMetadata(libs.findLibrary("koin-ksp-compiler").get())
    }
    project.tasks.configureEach {
        if (name.startsWith("ksp") && name != "kspCommonMainKotlinMetadata") {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}

internal fun KotlinMultiplatformExtension.composeDependencies() {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    sourceSets.apply {
        commonMain.dependencies {
            // Compose
            implementation(libs.findBundle("compose-baselibs").get())
            implementation(libs.findLibrary("compose-jetbrains-compose-resources").get())
            implementation(libs.findLibrary("compose-jetbrains-ui-tooling-preview").get())
            // KotlinX Collections Immutable
            implementation(libs.findLibrary("kotlinx-collections-immutable").get())
            // Toast
            implementation(libs.findLibrary("sonner").get())
        }
        androidMain.dependencies {
            implementation(libs.findBundle("compose-baselibs-android").get())
        }
        iosMain.dependencies {

        }
        jvmMain.dependencies {

        }
    }
}
