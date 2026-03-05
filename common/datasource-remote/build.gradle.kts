plugins {
    id("pixiv.multiplatform.compose")
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.ktorfit)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.common.datasource.remote"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":common:data"))

            // Serialization
            implementation(kotlinx.bundles.serialization)
            // Ktorfit
            implementation(kotlinx.ktorfit.lite)
        }
    }
}

ktorfit {
    compilerPluginVersion.set("2.3.3")
}