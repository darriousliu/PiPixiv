package com.mrl.pixiv.buildsrc

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? =
    add("implementation", dependencyNotation)

fun DependencyHandler.ksp(dependencyNotation: Any): Dependency? =
    add("ksp", dependencyNotation)

fun DependencyHandlerScope.kspCommonMainMetadata(dependencyNotation: Any): Dependency? =
    add("kspCommonMainMetadata", dependencyNotation)

fun DependencyHandlerScope.kspAndroid(dependencyNotation: Any): Dependency? =
    add("kspAndroid", dependencyNotation)

fun DependencyHandlerScope.kspIos(dependencyNotation: Any) {
    "kspIosArm64"(dependencyNotation)
    "kspIosSimulatorArm64"(dependencyNotation)
}

fun DependencyHandlerScope.kspJvm(dependencyNotation: Any): Dependency? =
    add("kspJvm", dependencyNotation)