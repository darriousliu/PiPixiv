plugins {
    id("pixiv.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.ai"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))
            implementation(project(":common:network"))

            // Serialization
            implementation(libs.bundles.kotlinx.serialization)
            // Ktor
            implementation(libs.bundles.kotlinx.ktor)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.ktor.client.mock)
        }
    }
}
