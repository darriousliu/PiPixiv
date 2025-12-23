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
    val compose = project.extensions.getByType<VersionCatalogsExtension>().named("composes")
    "androidRuntimeClasspath"(compose.findLibrary("ui-tooling").get())
}
