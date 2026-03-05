import com.mrl.pixiv.buildsrc.commonDependencies
import com.mrl.pixiv.buildsrc.configureKotlinMultiplatform
import com.mrl.pixiv.buildsrc.configureSortKoinKspGeneration
import com.mrl.pixiv.buildsrc.optIns
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-multiplatform`
    com.android.kotlin.multiplatform.library
    id("com.google.devtools.ksp")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(optIns)
    }

    androidLibrary {
        compileSdk = 36
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        configureKotlinMultiplatform()
        configureSortKoinKspGeneration()
    }

    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_22)
        }
    }

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }

    commonDependencies()
}

androidComponents {

}