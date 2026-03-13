package com.mrl.pixiv.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.configureSortKoinKspGeneration() {
    val registerSortTask =
        { variant: String, kspOutputDirPaths: List<Provider<Directory>>, kspTaskName: String, compileTaskName: String ->
            val cap = variant.uppercaseFirstChar()

            val sortTaskName = "sort${cap}KoinModules"
            // 避免重复注册
            if (tasks.names.contains(sortTaskName).not()) {
                tasks.whenTaskAdded {
                    if (name == kspTaskName) {
                        val sortTask = tasks.register<SortKoinModulesTask>(sortTaskName) {
                            group = "koin"
                            description = "Sort Koin KSP generated modules for variant: $variant"
                            kspOutputDirs.from(kspOutputDirPaths)

                            onlyIf {
                                kspOutputDirs.files.any { it.exists() && it.isDirectory }
                            }
                        }

                        // 关键：平铺式建立依赖，不要嵌套 configure
                        // sort <- ksp
                        try {
                            sortTask.configure {
                                dependsOn(tasks.named(kspTaskName))
                            }

                            // compile <- sort
                            tasks.named(compileTaskName).configure {
                                dependsOn(sortTask)
                            }

                            logger.quiet("✅ 已链接: $kspTaskName → ${sortTask.name} → $compileTaskName")
                        } catch (e: Exception) {
                            logger.info("⚠️ 无法链接任务 $variant: ${e.message}")
                        }
                    }
                }
            }
        }

    if (plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        extensions.configure<KotlinMultiplatformExtension> {
            targets.configureEach {
                val mainCompilation = compilations.findByName("main")
                if (mainCompilation != null) {
                    val platform = name
                    val cap = platform.uppercaseFirstChar()
                    // KSP 可能输出到多个目录，如:
                    // build/generated/ksp/{platform}/{platform}Main/kotlin/org/koin/ksp/generated
                    // build/generated/ksp/{platform}/commonMain/kotlin/org/koin/ksp/generated
                    val kspOutputDirPaths = listOf(
                        layout.buildDirectory.dir(
                            "generated/ksp/$platform/${platform}Main/kotlin/org/koin/ksp/generated"
                        ),
                        layout.buildDirectory.dir(
                            "generated/ksp/metadata/commonMain/kotlin/org/koin/ksp/generated"
                        )
                    )

                    registerSortTask(
                        platform,
                        kspOutputDirPaths,
                        if (platform == "android") "ksp${cap}Main" else "kspKotlin${cap}",
                        if (platform == "android") "compile${cap}Main" else "compileKotlin${cap}"
                    )
                }
            }
        }
    }
}

/**
 * ⭐ 自定义任务类：支持配置缓存 + 条件跳过
 */
@DisableCachingByDefault(because = "KSP 生成文件不稳定，不启用任务缓存")
abstract class SortKoinModulesTask : DefaultTask() {

    // 📥 可选的输入目录集合（不存在时自动跳过）
    @get:InputFiles
    abstract val kspOutputDirs: ConfigurableFileCollection

    @TaskAction
    fun sortModules() {
        val outputDirs = kspOutputDirs.files
            .filter { it.exists() && it.isDirectory }
            .sortedBy { it.absolutePath }

        if (outputDirs.isEmpty()) {
            logger.quiet("⏭️  KSP 输出目录不存在，跳过排序")
            return  // 直接返回，不执行排序逻辑
        }

        outputDirs.forEach { outputDir ->
            logger.quiet("  📂 处理目录: ${outputDir.absolutePath}")

            // 2️⃣ 查找 .kt 文件
            val moduleFiles = outputDir.listFiles { file ->
                file.isFile && file.name.endsWith(".kt")
            }?.sortedBy { it.name } ?: run {
                logger.quiet("  ⚠️ 目录为空或不可读")
                return@forEach
            }

            if (moduleFiles.isEmpty()) {
                logger.quiet("  ⚠️ 未找到 .kt 文件，跳过排序")
                return@forEach
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

            logger.lifecycle("  📊 [${outputDir.name}] 共排序 $sortedCount 个文件")
        }
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
