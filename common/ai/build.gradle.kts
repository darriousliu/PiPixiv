plugins {
    id("pixiv.multiplatform")
    alias(kotlinx.plugins.serialization)
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
            implementation(kotlinx.bundles.serialization)
            // Ktor
            implementation(kotlinx.bundles.ktor)
        }
    }
}
