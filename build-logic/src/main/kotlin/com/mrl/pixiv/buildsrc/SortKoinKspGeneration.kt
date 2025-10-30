package com.mrl.pixiv.buildsrc

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.io.File

abstract class SortKoinModulesTask : DefaultTask() {
    // 📥 输入目录（带 @InputDirectory 标注）
    @get:InputDirectory
    abstract val kspOutputDir: DirectoryProperty

    @TaskAction
    fun sortModules() {
        val outputDir = kspOutputDir.get().asFile
        logger.quiet("  📂 处理目录: ${outputDir.absolutePath}")

        if (!outputDir.exists()) {
            logger.warn("  ⚠️ 目录不存在，跳过排序")
            return
        }

        // 直接在 @TaskAction 中执行逻辑（不捕获 Project）
        val moduleFiles = outputDir.listFiles { file ->
            file.isFile && file.name.endsWith(".kt")
        } ?: return

        logger.quiet("  📋 发现 ${moduleFiles.size} 个文件: ${moduleFiles.joinToString(", ") { it.name }}")

        var sortedCount = 0
        moduleFiles.forEach { moduleFile ->
            val originalContent = moduleFile.readText(Charsets.UTF_8)
            val sortedContent = sortKoinModuleContent(originalContent)

            if (originalContent != sortedContent) {
                moduleFile.writeText(sortedContent, Charsets.UTF_8)
                logger.quiet("      ✅ Sorted: ${moduleFile.name}")
                sortedCount++
            } else {
                logger.quiet("      ⏭️  Already sorted: ${moduleFile.name}")
            }
        }
        logger.lifecycle("  📊 共排序 $sortedCount 个文件")
    }
}

fun Project.configureSortKoinKspGeneration() {
    val taskBlock: (variant: String) -> Unit = { variant ->
        tasks.register<SortKoinModulesTask>("sort${variant.uppercaseFirstChar()}KoinModules") {
            group = "koin"
            description = "Sort Koin KSP generated modules for variant: $variant"

            // 2️⃣ 通过 Property 注入路径（支持配置缓存）
            val outputDir =
                layout.buildDirectory.dir("generated/ksp/$variant/kotlin/org/koin/ksp/generated")
            kspOutputDir.set(outputDir)
            onlyIf {
                outputDir.get().asFile.exists()
            }
        }
        tasks.whenTaskAdded {
            if (name == "ksp${variant.uppercaseFirstChar()}Kotlin") {
                finalizedBy("sort${variant.uppercaseFirstChar()}KoinModules")
                logger.quiet("✅ 已链接: $name -> sort${variant.uppercaseFirstChar()}KoinModules")
            } else if (name == "compile${variant.uppercaseFirstChar()}Kotlin") {
                dependsOn("sort${variant.uppercaseFirstChar()}KoinModules")
            }
        }
    }
    if (plugins.hasPlugin("com.android.application")) {
        extensions.configure<BaseAppModuleExtension> {
            applicationVariants.all {
                val variant = name
                taskBlock(variant)
            }
        }
    } else {
        extensions.configure<LibraryExtension> {
            libraryVariants.all {
                val variant = name
                taskBlock(variant)
            }
        }
    }
}

/**
 * ⭐ 排序逻辑（可复用）
 */
fun sortKoinModuleContent(content: String): String {
    val moduleRegex = Regex(
        pattern = """module\s*\{([\s\S]*?)\n\s*\}""",
        options = setOf(RegexOption.MULTILINE)
    )

    return moduleRegex.replace(content) { matchResult ->
        val blockContent = matchResult.groupValues[1]

        val lines = blockContent.split("\n").filter { it.isNotBlank() }.sorted()

        val rebuiltContent = lines.joinToString("\n")
        """module {
$rebuiltContent
}"""
    }
}

/**
 * ⭐ 处理单个 KSP 输出目录
 */
fun Project.processKspDirectory(dir: File) {
    val moduleFiles = dir.listFiles { file ->
        file.isFile && file.name.endsWith(".kt")
    } ?: return
    logger.quiet(moduleFiles.joinToString(", ") { it.name })

    moduleFiles.forEach { moduleFile ->
        val originalContent = moduleFile.readText(Charsets.UTF_8)
        val sortedContent = sortKoinModuleContent(originalContent)

        if (originalContent != sortedContent) {
            moduleFile.writeText(sortedContent, Charsets.UTF_8)
            println("    ✅ Sorted: ${moduleFile.name}")
        }
    }
}