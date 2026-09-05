import com.android.build.api.artifact.SingleArtifact
import com.mrl.pixiv.buildsrc.CopyApk
import io.sentry.android.gradle.extensions.SentryPluginExtension
import org.gradle.internal.extensions.stdlib.capitalized

plugins {
    id("pixiv.android.application")
//    alias(libs.plugins.baselineprofile)
}

val enableHotSwanCompiler = providers.gradleProperty("hotswan.enabled")
    .map(String::toBoolean)
    .getOrElse(false)

if (enableHotSwanCompiler) {
    pluginManager.apply(libs.plugins.hotswan.compiler.get().pluginId)
}

if (project.findProperty("applyFirebasePlugins") == "true") {
    pluginManager.apply(libs.plugins.sentry.android.get().pluginId)
    pluginManager.apply(libs.plugins.google.services.get().pluginId)
    pluginManager.apply(libs.plugins.firebase.crashlytics.get().pluginId)
}

android {
    namespace = "com.mrl.pixiv"
    ndkVersion = "28.2.13676358"

    lint {
        disable.add("Instantiatable")
    }

    defaultConfig {
        applicationId = "com.mrl.pixiv"
        versionCode = findProperty("versionCode").toString().toInt()
        versionName = findProperty("versionName").toString()

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
}

androidComponents {
    onVariants { variant ->
        if (variant.name.contains("debug", true)) return@onVariants
        // create a task that will be responsible for copying the APKs
        val copyTask = project.tasks.register<CopyApk>("copyApksFor${variant.name.capitalized()}") {
//            dependsOn("create${variant.name.capitalized()}ApkListingFileRedirect")
            // set the output only. the input will be automatically provided via the
            // wiring mechanism
            output.set(
                project.layout.projectDirectory.dir(variant.name).dir(
                    if (project.findProperty("applyFirebasePlugins") == "true") {
                        "default"
                    } else {
                        "foss"
                    }
                )
            )

            // provide an instance of the artifact loader. This is necessary for
            // some artifacts. See Artifact.ContainsMany
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())

        }

        // Wire the task to respond to artifact creation
        variant.artifacts.use(copyTask).wiredWith {
            it.input
        }.toListenTo(SingleArtifact.APK)
    }
}

dependencies {
//    baselineProfile(project(":baselineprofile"))
    implementation(project(":common:data"))
    implementation(project(":common:network"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":common:core"))
    implementation(project(":composeApp"))

    // splash screen
    implementation(libs.androidx.splashscreen)
    // ProfileInstaller
    implementation(libs.androidx.profileinstaller)
    // Navigation3
    implementation(libs.bundles.compose.navigation3.android)
    // Coil3
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    implementation(libs.coil3.gif)
    // MMKV
    implementation(libs.mmkv)
    implementation(libs.mmkv.kotlin)
}

if (pluginManager.hasPlugin(libs.plugins.sentry.android.get().pluginId)) {
    configure<SentryPluginExtension> {
        debug.set(true)
        org.set("pipixiv")
        projectName.set("pipixiv")
        authToken.set(System.getenv("SENTRY_AUTH_TOKEN"))
        url = null
        includeProguardMapping.set(true)
        autoUploadProguardMapping.set(true)
        uploadNativeSymbols.set(false)
        autoUploadNativeSymbols.set(true)
        includeNativeSources.set(false)
        includeSourceContext.set(false)
        tracingInstrumentation {
            enabled.set(false)
        }
        autoInstallation {
            enabled.set(false)
        }
        includeDependenciesReport.set(false)
        telemetry.set(false)
    }
}
