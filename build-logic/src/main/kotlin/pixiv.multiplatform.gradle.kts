import com.mrl.pixiv.buildsrc.commonDependencies
import com.mrl.pixiv.buildsrc.configureKotlinMultiplatform
import com.mrl.pixiv.buildsrc.optIns
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-multiplatform`
    com.android.kotlin.multiplatform.library
    id("com.google.devtools.ksp")
    id("io.insert-koin.compiler.plugin")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(optIns)
    }

    android {
        compileSdk = 37
        minSdk = 26

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        configureKotlinMultiplatform()
    }

    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    commonDependencies()
}

androidComponents {

}
