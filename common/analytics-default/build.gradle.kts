plugins {
    id("pixiv.multiplatform")
    alias(libs.plugins.kotzilla)
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.analytics.default_"
    }

    sourceSets {
        commonMain.dependencies {
            // Kotzilla
            implementation(libs.kotzilla.sdk)
            // Sentry
            implementation(libs.sentry.multiplatform)
        }
        androidMain.dependencies {
            // Firebase
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.bundles.firebase)
        }
        iosMain.dependencies {

        }
        jvmMain.dependencies {

        }
    }
}

kotzilla {
    versionName = properties["versionName"]!!.toString()
    uploadMappingFile = false
    autoAddDependencies = false
}

tasks.matching { it.name == "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("generateKotzillaConfig")
}