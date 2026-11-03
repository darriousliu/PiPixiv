import com.mrl.pixiv.buildsrc.configureRemoveKoinMeta
import io.sentry.kotlin.multiplatform.gradle.AppleDependencyProvider
import io.sentry.kotlin.multiplatform.gradle.SentryExtension

plugins {
    id("pixiv.multiplatform.compose")
}

if (findProperty("applyFirebasePlugins") == "true") {
    pluginManager.apply(libs.plugins.sentry.kmp.get().pluginId)
    extensions.configure<SentryExtension> {
        // Sentry Cocoa 已由 Xcode 的 SwiftPM 管理，保留原生链接并关闭重复安装。
        autoInstall.apple.provider.set(AppleDependencyProvider.NONE)
    }
}

kotlin {
    android {
        namespace = "com.mrl.pixiv.multiplatform"
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":lib_strings"))
                implementation(project(":common:ai"))
                implementation(project(":common:data"))
                implementation(project(":common:datasource-local"))
                implementation(project(":common:network"))
                implementation(project(":common:repository"))
                implementation(project(":common:ui"))
                implementation(project(":common:core"))
                rootDir.resolve("feature").listFiles()?.filter { it.isDirectory }?.forEach {
                    implementation(project(":feature:${it.name}"))
                }
                implementation(libs.bundles.compose.navigation3)
                // Coil3
                implementation(project.dependencies.platform(libs.coil3.bom))
                implementation(libs.bundles.coil3)
                // FileKit
                implementation(libs.filekit.core)
                // MMKV
                implementation(libs.mmkv.kotlin)
            }
        }
        androidMain {
            dependencies {
                // Navigation3
                implementation(libs.bundles.compose.navigation3.android)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:${libs.versions.composeMultiplatform.get()}")
            implementation(compose.desktop.currentOs)
        }
        iosMain {
            dependencies {

            }
        }
    }

    configureRemoveKoinMeta()
}

fun ipaArguments(
    destination: String = "generic/platform=iOS",
    sdk: String = "iphoneos",
): Array<String> {
    return arrayOf(
        "xcodebuild",
        "-project", "PiPixiv.xcodeproj",
        "-scheme", "PiPixiv",
        "-destination", destination,
        "-sdk", sdk,
        "CODE_SIGNING_ALLOWED=NO",
        "CODE_SIGNING_REQUIRED=NO",
    )
}

val buildReleaseArchive = tasks.register("buildReleaseArchive", Exec::class) {
    group = "build"
    description = "Archives the iOS app with the Swift Export package"
    workingDir(rootDir.resolve("iosApp"))

    val output = layout.buildDirectory.dir("archives/release/PiPixiv.xcarchive")
    outputs.dir(output)
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration", "Release",
        "-archivePath", output.get().asFile.absolutePath,
    )
}

@CacheableTask
abstract class BuildIpaTask : DefaultTask() {

    /* -------------------------------------------------------------
     * Inputs / outputs
     * ----------------------------------------------------------- */

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val archiveDir: DirectoryProperty

    @get:OutputFile
    abstract val outputIpa: RegularFileProperty

    /* -------------------------------------------------------------
     * Services (injected)
     * ----------------------------------------------------------- */

    @get:Inject
    abstract val execOperations: ExecOperations

    /* -------------------------------------------------------------
     * Action
     * ----------------------------------------------------------- */

    @TaskAction
    fun buildIpa() {
        // 1. Locate the .app inside the .xcarchive
        val appDir = archiveDir.get().asFile.resolve("Products/Applications/PiPixiv.app")
        if (!appDir.exists())
            throw GradleException("Could not find PiPixiv.app in archive at: ${appDir.absolutePath}")

        // 2. Create temporary Payload directory and copy .app into it
        val payloadDir = File(temporaryDir, "Payload").apply { mkdirs() }
        val destApp = File(payloadDir, appDir.name)
        appDir.copyRecursively(destApp, overwrite = true)

        // 3. Inject placeholder (ad‑hoc) code signature so AltStore / SideStore accept it
        logger.lifecycle("[IPA] Ad‑hoc signing ${destApp.name} …")
        execOperations.exec {
            commandLine(
                "codesign", "--force", "--deep", "--sign", "-", "--timestamp=none",
                destApp.absolutePath,
            )
        }

        // 4. Zip Payload ⇒ .ipa using the system `zip` command
        //
        //    -r : recurse into directories
        //    -y : store symbolic links as the link instead of the referenced file
        //
        // The working directory is the temporary folder so the archive
        // has a top‑level "Payload/" directory (required for .ipa files).
        val zipFile = File(temporaryDir, "PiPixiv.zip")
        execOperations.exec {
            workingDir(temporaryDir)
            commandLine("zip", "-r", "-y", zipFile.absolutePath, "Payload")
        }

        // 5. Move to final location (with .ipa extension)
        outputIpa.get().asFile.apply {
            parentFile.mkdirs()
            delete()
            zipFile.renameTo(this)
        }

        logger.lifecycle("[IPA] Created ad‑hoc‑signed IPA at: ${outputIpa.get().asFile.absolutePath}")
    }
}

tasks.register("buildReleaseIpa", BuildIpaTask::class) {
    description = "Manually packages the .app from the .xcarchive into an unsigned .ipa"
    group = "build"

    // Adjust these paths as needed
    archiveDir = layout.buildDirectory.dir("archives/release/PiPixiv.xcarchive")
    outputIpa = layout.buildDirectory.file("archives/release/PiPixiv.ipa")
    dependsOn(buildReleaseArchive)
}
