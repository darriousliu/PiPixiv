package com.mrl.pixiv.buildsrc

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlinMultiplatform(
    kmpTarget: KotlinMultiplatformAndroidLibraryTarget,
) {
    with(kmpTarget) {
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

    configureKotlin()


//    configureSortKoinKspGeneration()
}

internal fun KotlinMultiplatformExtension.commonDependencies() {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val kotlinx = project.extensions.getByType<VersionCatalogsExtension>().named("kotlinx")
    val androidx = project.extensions.getByType<VersionCatalogsExtension>().named("androidx")
    sourceSets.apply {
        commonMain.dependencies {
            // Lifecycle
//        implementation(androidx.findBundle("lifecycle").get())
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
        kspCommonMainMetadata(libs.findLibrary("koin-ksp-compiler").get())
    }
}