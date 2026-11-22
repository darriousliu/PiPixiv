plugins {
    id("pixiv.multiplatform")
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.common.analytics.default_"
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)

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
            implementation(libs.filekit.core)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
