package com.mrl.pixiv.buildsrc

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CopyApk : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @TaskAction
    fun taskAction() {
        // delete the previous content. This task does not support incremental mode but could
        // be modified to do so
        val outputDirectory = output.get()
        val outputFile = outputDirectory.asFile

        outputFile.deleteRecursively()
        outputFile.mkdirs()

        // this will load the content of the folder and give access to all items representing
        // the artifact with their metadata
        val builtArtifacts = builtArtifactsLoader.get().load(input.get())
            ?: throw RuntimeException("Cannot load APKs")

        builtArtifacts.elements.forEach { artifact ->
            // construct a new name to copy the APK, using some of the APK metadata
            val name = buildString {
                append(project.rootProject.name)
                artifact.versionName?.let {
                    if (it.isNotBlank()) {
                        append("-v$it")
                    }
                }
                append("-")
                append(
                    if (project.findProperty("applyFirebasePlugins") == "true") {
                        "default"
                    } else {
                        "foss"
                    }
                )
                append("-")
                append(builtArtifacts.variantName)
                append(".apk")
            }

            File(artifact.outputFile).renameTo(outputDirectory.file(name).asFile)
        }

        // The above will only save the artifact themselves. It will not save the
        // metadata associated with them. Depending on our needs we may need to copy it.
        // This is required when transforming such an artifact. We'll do it here for demonstration
        // purpose.
        builtArtifacts.save(outputDirectory)
    }
}