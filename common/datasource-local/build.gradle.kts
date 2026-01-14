plugins {
    id("pixiv.multiplatform.compose")
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.common.datasource.local"
    }
    sourceSets {
        commonMain.dependencies {
            // Serialization
            implementation(kotlinx.bundles.serialization)
            // Room
            implementation(androidx.room.runtime)
            implementation(androidx.sqlite.bundled)
            // Koin
            implementation(libs.bundles.koin)
            // FileKit
            implementation(libs.filekit.core)
        }
    }

}

dependencies {
    kspAndroid(androidx.room.compiler)
    kspIosArm64(androidx.room.compiler)
    kspIosSimulatorArm64(androidx.room.compiler)
    kspJvm(androidx.room.compiler)
}