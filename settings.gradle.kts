@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties


val localProperties = Properties()
val localFile = file("local.properties")
if (localFile.exists()) {
    localProperties.load(FileInputStream(localFile))
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
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
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}
rootProject.name = "PiPixiv"
include(":app")
include(":lib_strings")
include(":baselineprofile")

file("./common").listFiles()?.filter { it.isDirectory }?.forEach {
    include(":common:${it.name}")
}

file("./feature").listFiles()?.filter { it.isDirectory }?.forEach {
    include(":feature:${it.name}")
}
