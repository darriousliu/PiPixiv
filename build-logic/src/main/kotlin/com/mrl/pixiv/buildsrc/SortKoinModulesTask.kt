package com.mrl.pixiv.buildsrc

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.work.DisableCachingByDefault

fun Project.configureSortKoinKspGeneration() {
    val taskBlock: (variant: String) -> Unit = { variant ->
        val cap = variant.uppercaseFirstChar()

        val kspOutputDirPath = layout.buildDirectory.dir(
            "generated/ksp/$variant/kotlin/org/koin/ksp/generated"
        )

        val sortTask = tasks.register<SortKoinModulesTask>("sort${cap}KoinModules") {
            group = "koin"
            description = "Sort Koin KSP generated modules for variant: $variant"
            kspOutputDir.set(kspOutputDirPath)

            onlyIf {
                val dir = kspOutputDir.orNull?.asFile
                dir != null && dir.exists()
            }
        }

        val kspTaskName = "ksp${cap}Kotlin"
        val compileTaskName = "compile${cap}Kotlin"

        // 关键：平铺式建立依赖，不要嵌套 configure
        // sort <- ksp
        sortTask.configure {
            dependsOn(tasks.named(kspTaskName))
        }

        // compile <- sort
        tasks.named(compileTaskName).configure {
            dependsOn(sortTask)
        }

        logger.quiet("✅ 已链接: $kspTaskName → ${sortTask.name} → $compileTaskName")
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
        }?.sortedBy { it.name } ?: run {
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
private fun Task.sortKoinModuleContent(content: String): String {
    val moduleRegex = Regex(
        pattern = """module\s*\{([\s\S]*?)\n\s*\}""",
        options = setOf(RegexOption.MULTILINE)
    )

    var result = moduleRegex.replace(content) { matchResult ->
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
    // 2️⃣ 排序 @ExternalDefinition 函数定义
    result = sortDefinitionsByExternalDefinition(result)

    return result
}

/**
 * ⭐ 对 @ExternalDefinition 函数定义按照 ExternalDefinition 值进行排序
 */
private fun Task.sortDefinitionsByExternalDefinition(content0: String): String {
    // 1) 统一换行，避免 CRLF/LF 导致的边界不一致
    val content = content0.replace("\r\n", "\n").replace("\r", "\n")

    // 2) 抽离固定 footer 注释，避免被上一个 block 吞掉
    val footerRegex = Regex("""(?s)\n\s*//DefaultModule generation is disabled\..*""")
    val footerMatch = footerRegex.find(content)
    val footer = footerMatch?.value.orEmpty()
    val contentNoFooter = footerMatch?.let { content.removeRange(it.range) } ?: content

    // 3) 仅匹配“行首”的 ExternalDefinition block，block 结束于下一个 ExternalDefinition（行首）或 EOF
    val defRegex = Regex(
        pattern = """(?ms)^\s*@ExternalDefinition\([^)]*\)\s*public fun Module\..*?(?=^\s*@ExternalDefinition|\Z)"""
    )

    val matches = defRegex.findAll(contentNoFooter).toList()
    if (matches.isEmpty()) return content

    val header = contentNoFooter.substring(0, matches.first().range.first)
    val tail = contentNoFooter.substring(matches.last().range.last + 1)

    val sortedBlocks = matches
        .map { it.value }
        .sorted()

    return buildString {
        append(header)
        sortedBlocks.forEach { b ->
            append(b.trimEnd())
            append("\n")
        }
        append(tail)
        if (footer.isNotBlank()) {
            if (!endsWith("\n")) append("\n")
            append(footer.trimStart('\n'))
        }
    }.also { rebuiltContent ->
        logger.quiet("before:\n$content\n\nafter:\n$rebuiltContent")
    }
}