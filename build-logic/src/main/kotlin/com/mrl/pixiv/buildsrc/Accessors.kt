package com.mrl.pixiv.buildsrc

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

// coreLibraryDesugaring
fun DependencyHandler.coreLibraryDesugaring(dependencyNotation: Any): Dependency? =
    add("coreLibraryDesugaring", dependencyNotation)
