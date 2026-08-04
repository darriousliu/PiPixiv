import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    id("pixiv.multiplatform.compose")
    alias(kotlinx.plugins.serialization)
    alias(kotlinx.plugins.parcelize)
    alias(libs.plugins.build.konfig)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common"

        androidResources {
            enable = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)

        commonMain.dependencies {
            api(project(":common:platform-api"))
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
    }
}

buildkonfig {
    val props = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val sentryDsn = if (findProperty("applyFirebasePlugins") == "true") {
        props.getProperty("sentryDsn") ?: System.getenv("SENTRY_DSN")
    } else {
        "unused"
    }
    packageName = "com.mrl.pixiv.common"

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.BOOLEAN,
            "DEBUG",
            findProperty("debug").toString(),
            const = true
        )
        buildConfigField(
            FieldSpec.Type.INT,
            "versionCode",
            findProperty("versionCode").toString(),
            const = true
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "versionName",
            findProperty("versionName").toString(),
            const = true
        )
        buildConfigField(FieldSpec.Type.STRING, "sentryDsn", sentryDsn.orEmpty(), const = true)
    }
}
