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
import org.gradle.work.DisableCachingByDefault

fun Project.configureSortKoinKspGeneration() {
    val taskBlock: (variant: String) -> Unit = { variant ->
        val kspOutputDirPath = layout.buildDirectory.dir(
            "generated/ksp/$variant/kotlin/org/koin/ksp/generated"
        )

        // 1️⃣ 注册任务
        tasks.register<SortKoinModulesTask>("sort${variant.uppercaseFirstChar()}KoinModules") {
            group = "koin"
            description = "Sort Koin KSP generated modules for variant: $variant"
            kspOutputDir.set(kspOutputDirPath)

            // 2️⃣ ⭐ 关键：使用 onlyIf 谓词决定是否执行
            onlyIf {
                val dir = kspOutputDir.orNull?.asFile
                val shouldRun = dir != null && dir.exists()

                if (!shouldRun) {
                    logger.quiet("⏭️  Task '${this.name}' 被跳过（KSP 输出目录不存在）")
                }
                shouldRun
            }
        }

        // 3️⃣ 延迟绑定任务依赖
        tasks.whenTaskAdded {
            if (name == "ksp${variant.uppercaseFirstChar()}Kotlin") {
                finalizedBy("sort${variant.uppercaseFirstChar()}KoinModules")
                logger.quiet("✅ 已链接: $name → sort${variant.uppercaseFirstChar()}KoinModules")
            }
        }
    }

    if (plugins.hasPlugin("com.android.application")) {
        extensions.configure<BaseAppModuleExtension> {
            applicationVariants.all {
                taskBlock(name)
            }
        }
    } else {
        extensions.configure<LibraryExtension> {
            libraryVariants.all {
                taskBlock(name)
            }
        }
    }
}

/**
 * ⭐ 自定义任务类：支持配置缓存 + 条件跳过
 */
@DisableCachingByDefault(because = "KSP 生成文件不稳定，不启用任务缓存")
abstract class SortKoinModulesTask : DefaultTask() {

    // 📥 可选的输入目录（不存在时自动跳过）
    @get:InputDirectory
    abstract val kspOutputDir: DirectoryProperty

    @TaskAction
    fun sortModules() {
        // 1️⃣ 检查目录是否存在
        val outputDir = kspOutputDir.orNull?.asFile

        if (outputDir == null || !outputDir.exists()) {
            logger.quiet("⏭️  KSP 输出目录不存在，跳过排序: ${outputDir?.absolutePath ?: "null"}")
            return  // 直接返回，不执行排序逻辑
        }

        logger.quiet("  📂 处理目录: ${outputDir.absolutePath}")

        // 2️⃣ 查找 .kt 文件
        val moduleFiles = outputDir.listFiles { file ->
            file.isFile && file.name.endsWith(".kt")
        } ?: run {
            logger.quiet("  ⚠️ 目录为空或不可读")
            return
        }

        if (moduleFiles.isEmpty()) {
            logger.quiet("  ⚠️ 未找到 .kt 文件，跳过排序")
            return
        }

        logger.quiet("  📋 发现 ${moduleFiles.size} 个文件: ${moduleFiles.joinToString(", ") { it.name }}")

        // 3️⃣ 执行排序
        var sortedCount = 0
        moduleFiles.forEach { moduleFile ->
            val originalContent = moduleFile.readText(Charsets.UTF_8)
            val sortedContent = sortKoinModuleContent(originalContent)

            if (originalContent != sortedContent) {
                moduleFile.writeText(sortedContent, Charsets.UTF_8)
                logger.quiet("      ✅ Sorted: ${moduleFile.name}")
                sortedCount++
            }
        }

        logger.lifecycle("  📊 共排序 $sortedCount 个文件")
    }
}

/**
 * ⭐ 排序核心逻辑
 */
private fun sortKoinModuleContent(content: String): String {
    val moduleRegex = Regex(
        pattern = """module\s*\{([\s\S]*?)\n\s*\}""",
        options = setOf(RegexOption.MULTILINE)
    )

    return moduleRegex.replace(content) { matchResult ->
        val blockContent = matchResult.groupValues[1]
        val lines = blockContent.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .sorted()

        val rebuiltContent = lines.joinToString("\n    ")
        """module {
    $rebuiltContent
}"""
    }
}