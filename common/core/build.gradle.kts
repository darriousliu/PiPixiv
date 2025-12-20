plugins {
    id("pixiv.android.library.compose")
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.parcelize)
}

android {
    namespace = "com.mrl.pixiv.common"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":lib_strings"))
    if (project.findProperty("applyFirebasePlugins") == "true") {
        api(project(":common:analytics-default"))
    } else {
        api(project(":common:analytics-foss"))
    }
    implementation(project(":common:data"))

    implementation(libs.material)
    implementation(composes.bundles.navigation3.android)
    // Ktor
    implementation(kotlinx.bundles.ktor)
    // Serialization
    implementation(kotlinx.bundles.serialization)
    // DateTime
    implementation(kotlinx.datetime)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    // MMKV
    implementation(libs.mmkv)
    implementation(libs.mmkv.kotlin)
}