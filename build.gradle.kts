buildscript {

}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(androidx.plugins.android.application) apply false
    alias(androidx.plugins.android.library) apply false
    alias(androidx.plugins.android.test) apply false
    alias(kotlinx.plugins.android) apply false
    alias(kotlinx.plugins.serialization) apply false
    alias(kotlinx.plugins.compose.compiler) apply false
    alias(kotlinx.plugins.ktorfit) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(kotlinx.plugins.parcelize) apply false
    alias(libs.plugins.kotzilla) apply false
}