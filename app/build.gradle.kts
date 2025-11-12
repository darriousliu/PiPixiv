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
        versionCode = 10201
        versionName = "1.2.1"

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

    flavorDimensions += "version"
    productFlavors {
        create("default") {
            isDefault = true
            dimension = flavorDimensionList[0]
        }
        create("foss") {
            dimension = flavorDimensionList[0]
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


    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    applicationVariants.configureEach {
        outputs.configureEach {
            (this as? ApkVariantOutputImpl)?.outputFileName =
                "${rootProject.name}-v${defaultConfig.versionName}-$name.apk"
        }
    }
}

dependencies {
    implementation(project(":lib_common"))
    implementation(project(":lib_feature"))

    baselineProfile(project(":baselineprofile"))
    ksp(libs.koin.ksp.compiler)
}
