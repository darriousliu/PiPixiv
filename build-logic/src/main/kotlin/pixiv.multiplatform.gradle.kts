import com.android.build.api.dsl.androidLibrary
import com.mrl.pixiv.buildsrc.commonDependencies
import com.mrl.pixiv.buildsrc.configureKotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-multiplatform`
    com.android.kotlin.multiplatform.library
    id("com.google.devtools.ksp")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    @Suppress("UnstableApiUsage")
    androidLibrary {
        compileSdk = 36
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        configureKotlinMultiplatform(this)
    }

    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    commonDependencies()
}

androidComponents {

}