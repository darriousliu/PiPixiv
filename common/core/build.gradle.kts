import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    id("pixiv.multiplatform.compose")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
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
            implementation(libs.compose.jetbrains.compose.resources)
            implementation(libs.androidx.annotation)
            implementation(libs.bundles.compose.navigation3)
            // Ktor
            implementation(libs.bundles.kotlinx.ktor)
            // Serialization
            implementation(libs.bundles.kotlinx.serialization)
            // DateTime
            implementation(libs.kotlinx.datetime)
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
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.bundles.compose.navigation3.android)
            implementation(libs.kotlinx.ktor.client.okhttp)
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
