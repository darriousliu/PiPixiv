import com.mrl.pixiv.buildsrc.composeDependencies

plugins {
    id("pixiv.multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
}

kotlin {
    composeDependencies()
}

dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    "androidRuntimeClasspath"(libs.findLibrary("compose-ui-tooling").get())
}
