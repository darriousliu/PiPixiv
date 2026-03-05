plugins {
    id("pixiv.multiplatform.compose")
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.common.data"
    }

    sourceSets {
        commonMain.dependencies {
            // Serialization
            implementation(kotlinx.bundles.serialization)
            implementation(kotlinx.datetime)
        }
    }
}
