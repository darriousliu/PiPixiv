plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.asm)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.compose.compiler.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    testImplementation(kotlin("test-junit"))
    testImplementation(gradleTestKit())
}
