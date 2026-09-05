plugins {
    id("pixiv.multiplatform.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.data"
    }

    sourceSets {
        commonMain.dependencies {
            // Serialization
            implementation(libs.bundles.kotlinx.serialization)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
