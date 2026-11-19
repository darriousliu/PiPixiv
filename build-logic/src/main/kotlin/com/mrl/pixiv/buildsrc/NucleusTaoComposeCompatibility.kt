package com.mrl.pixiv.buildsrc

import org.gradle.api.Project
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private val nucleusTaoPatched = Attribute.of("com.mrl.pixiv.nucleus-tao-patched", Boolean::class.javaObjectType)

/**
 * Backports the Compose 1.13 JVM changes from Nucleus #934 to the published Tao 2.5.18 runtime.
 * The application still uses Nucleus 2.5.18's JVM API, Gradle plugin, and packaged native libraries.
 */
fun Project.configureNucleusTaoComposeCompatibility() {
    val osName = System.getProperty("os.name")
    val os = when {
        osName == "Mac OS X" -> "macos"
        osName.startsWith("Windows") -> "windows"
        osName.startsWith("Linux") -> "linux"
        else -> error("Unsupported desktop OS: $osName")
    }
    val archName = System.getProperty("os.arch")
    val arch = when (archName) {
        "aarch64", "arm64" -> "arm64"
        "amd64", "x86_64" -> "x64"
        else -> error("Unsupported desktop architecture: $archName")
    }

    dependencies.registerTransform(PatchNucleusTao::class.java) {
        from.attribute(nucleusTaoPatched, false)
        to.attribute(nucleusTaoPatched, true)
    }
    dependencies.artifactTypes.configureEach {
        if (name == "jar") {
            attributes.attribute(nucleusTaoPatched, false)
        }
    }
    configurations.configureEach {
        if (name.endsWith("RuntimeClasspath", ignoreCase = true)) {
            attributes.attribute(nucleusTaoPatched, true)
        }
        // Nucleus 2.5.18's jpackage support recognizes the host-specific Skiko artifact.
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.skiko" && requested.name == "skiko-awt-runtime-all") {
                useTarget("org.jetbrains.skiko:skiko-awt-runtime-$os-$arch:${requested.version}")
            }
        }
    }
}

@CacheableTransform
abstract class PatchNucleusTao : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (!input.name.startsWith("nucleus.decorated-window-tao-")) {
            outputs.file(input)
            return
        }
        check(input.name == "nucleus.decorated-window-tao-2.5.18.jar") {
            "The Compose 1.13 compatibility patch requires Nucleus Tao 2.5.18, got ${input.name}"
        }

        val patchedEntries = mutableSetOf<String>()
        val output = outputs.file("${input.nameWithoutExtension}-compose-1.13.jar")
        ZipFile(input).use { original ->
            ZipOutputStream(output.outputStream().buffered()).use { patched ->
                for (entry in original.entries()) {
                    patched.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                    original.getInputStream(entry).use { content ->
                        if (entry.name in nucleusTaoClassEntries) {
                            patched.write(patchNucleusTaoClass(entry.name, content.readBytes()))
                            patchedEntries += entry.name
                        } else {
                            content.copyTo(patched)
                        }
                    }
                    patched.closeEntry()
                }
            }
        }
        check(patchedEntries == nucleusTaoClassEntries) {
            "Nucleus Tao JAR is missing classes required by the Compose patch: ${nucleusTaoClassEntries - patchedEntries}"
        }
    }
}
