import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    id("pixiv.android.application")
    alias(androidx.plugins.baselineprofile)
}
if (project.findProperty("applyFirebasePlugins") == "true") {
    pluginManager.apply(libs.plugins.google.services.get().pluginId)
    pluginManager.apply(libs.plugins.firebase.crashlytics.get().pluginId)
    pluginManager.apply(libs.plugins.kotzilla.get().pluginId)
}

android {
    namespace = "com.mrl.pixiv"

    lint {
        disable.add("Instantiatable")
    }

    defaultConfig {
        applicationId = "com.mrl.pixiv"
        versionCode = properties["versionCode"].toString().toInt()
        versionName = properties["versionName"].toString()

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    signingConfigs {
        create("release") {
            storeFile = file("../pipixiv.jks")
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEYSTORE_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            versionNameSuffix = "-debug"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    applicationVariants.configureEach {
        outputs.configureEach {
            val name = if (project.findProperty("applyFirebasePlugins") == "true") {
                "default"
            } else {
                "foss"
            }
            val releaseType = if (buildType.isDebuggable) "debug" else "release"
            (this as? ApkVariantOutputImpl)?.outputFileName =
                "${rootProject.name}-v${defaultConfig.versionName}-$name-$releaseType.apk"
        }
    }
}

dependencies {
    baselineProfile(project(":baselineprofile"))
    implementation(project(":common:data"))
    implementation(project(":common:network"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))
    rootDir.resolve("feature").listFiles()?.filter { it.isDirectory }?.forEach {
        implementation(project(":feature:${it.name}"))
    }

    // splash screen
    implementation(androidx.splashscreen)
    // ProfileInstaller
    implementation(androidx.profileinstaller)
    // Navigation3
    implementation(composes.bundles.navigation3.android)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    implementation(libs.coil3.gif)
    // MMKV
    implementation(libs.mmkv)
    implementation(libs.mmkv.kotlin)
}
