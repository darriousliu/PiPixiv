plugins {
    id("pixiv.multiplatform")

}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.common.analytics.default_"
    }

    sourceSets {
        commonMain.dependencies {
            // Kotzilla
            implementation(libs.kotzilla.sdk)
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
