@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.*


val localProperties = Properties()
val localFile = file("local.properties")
if (localFile.exists()) {
    localProperties.load(FileInputStream(localFile))
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://www.jitpack.io")
    }
}
dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinx") {
            from(files("gradle/kotlinx.versions.toml"))
        }
        create("androidx") {
            from(files("gradle/androidx.versions.toml"))
        }
        create("compose") {
            from(files("gradle/compose.versions.toml"))
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
        maven(url = "https://androidx.dev/storage/compose-compiler/repository/")
        maven {
            url = uri("https://maven.pkg.github.com/master-lzh/MMKV")
            credentials {
                username =
                    localProperties.getProperty("github.user") ?: System.getenv("GH_USERNAME")
                password = localProperties.getProperty("github.package.token")
                    ?: System.getenv("GH_PACKAGE_TOKEN")
            }
        }
        maven {
            // You can find the maven URL for other artifacts (e.g. KMP, METALAVA) on their
            // build pages.
            url = uri("https://androidx.dev/snapshots/builds/13570461/artifacts/repository")
        }
    }
}
rootProject.name = "PiPixiv"
include(":app")
include(":lib_common")
include(":lib_feature")
include(":lib_strings")
include(":baselineprofile")
