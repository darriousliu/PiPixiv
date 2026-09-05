plugins {
    id("pixiv.multiplatform.compose")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktorfit)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.datasource.remote"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))

            // Serialization
            implementation(libs.bundles.kotlinx.serialization)
            // Ktorfit
            implementation(libs.kotlinx.ktorfit.lite)
        }
    }
}

ktorfit {

}