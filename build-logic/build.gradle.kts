plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(androidx.android.gradlePlugin)
    implementation(kotlinx.kotlin.gradlePlugin)
    implementation(kotlinx.ksp.gradlePlugin)
    implementation(kotlinx.compose.compiler.gradlePlugin)
    implementation(composes.compose.graldePlugin)
}