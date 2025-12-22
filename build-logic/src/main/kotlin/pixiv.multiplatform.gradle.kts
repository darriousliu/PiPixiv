import com.android.build.api.dsl.androidLibrary
import com.mrl.pixiv.buildsrc.commonDependencies
import com.mrl.pixiv.buildsrc.configureKotlin
import com.mrl.pixiv.buildsrc.configureKotlinMultiplatform
import com.mrl.pixiv.buildsrc.optIns
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-multiplatform`
    com.android.kotlin.multiplatform.library
    id("com.google.devtools.ksp")
}

kotlin {
    jvmToolchain(22)
    compilerOptions {
        freeCompilerArgs.addAll(optIns)
    }

    @Suppress("UnstableApiUsage")
    androidLibrary {
        compileSdk = 36
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        configureKotlinMultiplatform()
    }

    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }

    commonDependencies()
    configureKotlin()
}

androidComponents {

}