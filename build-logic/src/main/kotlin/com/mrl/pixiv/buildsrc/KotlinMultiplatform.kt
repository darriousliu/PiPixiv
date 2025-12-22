package com.mrl.pixiv.buildsrc

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun KotlinMultiplatformAndroidLibraryTarget.configureKotlinMultiplatform() {
    compileSdk {
        version = release(36)
    }
    minSdk = 26

    withDeviceTest {
        instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

internal fun KotlinMultiplatformExtension.commonDependencies() {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val kotlinx = project.extensions.getByType<VersionCatalogsExtension>().named("kotlinx")
    val androidx = project.extensions.getByType<VersionCatalogsExtension>().named("androidx")
    sourceSets.apply {
        commonMain.dependencies {
            // Lifecycle
            implementation(libs.findBundle("lifecycle").get())
            // Coroutines
            implementation(
                project.dependencies.platform(
                    kotlinx.findLibrary("coroutines-bom").get()
                )
            )
            implementation(kotlinx.findLibrary("coroutines-core").get())
            // Koin
            implementation(libs.findBundle("koin").get())

            // Logger
            implementation(libs.findLibrary("kermit").get())
        }
        androidMain.dependencies {
            implementation(androidx.findBundle("androidx").get())
            // Coroutines
            implementation(kotlinx.findLibrary("coroutines-android").get())
        }
        iosMain.dependencies {

        }
        jvmMain.dependencies {
            // Coroutines
            implementation(kotlinx.findLibrary("coroutines-swing").get())
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
    val compose = project.extensions.getByType<VersionCatalogsExtension>().named("composes")
    val kotlinx = project.extensions.getByType<VersionCatalogsExtension>().named("kotlinx")
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    sourceSets.apply {
        commonMain.dependencies {
            // Compose
            implementation(compose.findBundle("baselibs").get())
            implementation(compose.findLibrary("jetbrains-compose-resources").get())
            // KotlinX Collections Immutable
            implementation(kotlinx.findLibrary("collections-immutable").get())
            // Toast
            implementation(libs.findLibrary("sonner").get())
        }
        androidMain.dependencies {

        }
        iosMain.dependencies {

        }
        jvmMain.dependencies {

        }
    }
}