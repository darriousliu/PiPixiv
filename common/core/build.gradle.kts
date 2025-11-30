plugins {
    id("pixiv.android.library.compose")
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.ktorfit)
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
    implementation(project(":common:data"))

    implementation(libs.material)
    implementation(compose.bundles.navigation3)
    // Ktor
    implementation(kotlinx.bundles.ktor)
    // Serialization
    implementation(kotlinx.bundles.serialization)
    // DateTime
    implementation(kotlinx.datetime)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    // Firebase
    defaultImplementation(platform(libs.firebase.bom))
    defaultImplementation(libs.bundles.firebase)
    // MMKV
    implementation(libs.mmkv)
    implementation(libs.mmkv.kotlin)
    // Kotzilla
    defaultImplementation(libs.kotzilla.sdk)
}