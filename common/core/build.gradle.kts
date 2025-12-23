import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    id("pixiv.multiplatform.compose")
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.parcelize)
    alias(libs.plugins.build.konfig)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.common"

        androidResources {
            enable = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val androidJvmMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)

        commonMain.dependencies {
            if (project.findProperty("applyFirebasePlugins") == "true") {
                api(project(":common:analytics-default"))
            } else {
                api(project(":common:analytics-foss"))
            }
            implementation(project(":common:data"))
            implementation(composes.jetbrains.compose.resources)
            implementation(androidx.annotation)
            implementation(composes.bundles.navigation3)
            // Ktor
            implementation(kotlinx.bundles.ktor)
            // Serialization
            implementation(kotlinx.bundles.serialization)
            // DateTime
            implementation(kotlinx.datetime)
            // Coil3
            implementation(project.dependencies.platform(libs.coil3.bom))
            implementation(libs.bundles.coil3)
            // MMKV
            implementation(libs.mmkv.kotlin)
            // Toast
            implementation(libs.sonner)
            // FileKit
            implementation(libs.bundles.filekit)
            implementation(libs.mp.stools)
        }
        androidMain.dependencies {
            implementation(libs.material)
            implementation(androidx.lifecycle.process)
            implementation(composes.bundles.navigation3.android)
            implementation(kotlinx.ktor.client.okhttp)
            implementation(libs.coil3.gif)
            implementation(libs.mmkv)
        }
        jvmMain.dependencies {
            // MMKV
            val osName = System.getProperty("os.name")
            when {
                osName == "Mac OS X" -> implementation(libs.mmkv.kotlin.nativelib.macos)
                osName.startsWith("Win") -> implementation(libs.mmkv.kotlin.nativelib.windows)
                osName.startsWith("Linux") -> implementation(libs.mmkv.kotlin.nativelib.linux)
                else -> error("Unsupported OS: $osName")
            }
        }
    }
}

buildkonfig {
    packageName = "com.mrl.pixiv.common"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.BOOLEAN, "DEBUG", properties["debug"].toString())
        buildConfigField(FieldSpec.Type.INT, "versionCode", properties["versionCode"].toString())
        buildConfigField(FieldSpec.Type.STRING, "versionName", properties["versionName"].toString())
    }
}