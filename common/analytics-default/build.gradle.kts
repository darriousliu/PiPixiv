plugins {
    id("pixiv.multiplatform")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.analytics.default_"
    }

    sourceSets {
        commonMain.dependencies {
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
