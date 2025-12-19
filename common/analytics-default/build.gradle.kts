plugins {
    id("pixiv.android.library")

}

android {
    namespace = "com.mrl.pixiv.common.analytics.default_"
}

dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    // Kotzilla
    implementation(libs.kotzilla.sdk)
}