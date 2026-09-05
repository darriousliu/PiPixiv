plugins {
    id("pixiv.multiplatform.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.datasource.local"
    }
    sourceSets {
        commonMain.dependencies {
            // Serialization
            implementation(libs.bundles.kotlinx.serialization)
            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            // Koin
            implementation(libs.bundles.koin)
            // FileKit
            implementation(libs.filekit.core)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(project.dependencies.platform(libs.kotlinx.coroutines.bom))
            implementation(libs.kotlinx.coroutines.test)
        }
    }

}

dependencies {
    kspAndroid(libs.androidx.room.compiler)
    kspIosArm64(libs.androidx.room.compiler)
    kspIosSimulatorArm64(libs.androidx.room.compiler)
    kspJvm(libs.androidx.room.compiler)
}
