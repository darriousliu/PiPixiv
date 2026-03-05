package com.mrl.pixiv.buildsrc

import org.gradle.api.Project

fun Project.configureRemoveKoinMeta() {
    // 在kspCommonMainMetadata后移除
    tasks.whenTaskAdded {
        if (name == "kspCommonMainKotlinMetadata") {
            val koinMetaDir =
                file("build/generated/ksp/metadata/commonMain/kotlin/org/koin/ksp/generated")
            doLast {
                if (koinMetaDir.exists()) {
                    koinMetaDir.listFiles().forEach {
                        if (it.name.contains("KoinMeta")) {
                            it.delete()
                        }
                    }
                    println("🗑️Removed Koin metadata directory: ${koinMetaDir.path}")
                } else {
                    println("Koin metadata directory does not exist: ${koinMetaDir.path}")
                }
            }
        }
    }
}