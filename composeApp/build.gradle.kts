import com.mrl.pixiv.buildsrc.configureRemoveKoinMeta
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractProguardTask

plugins {
    id("pixiv.multiplatform.compose")
    alias(composes.plugins.composeHotReload)
    alias(kotlinx.plugins.native.cocoapods)
    alias(libs.plugins.kotzilla)
}

if (properties["applyFirebasePlugins"] == "true") {
    pluginManager.apply(libs.plugins.sentry.kmp.get().pluginId)
}

kotlin {
    androidLibrary {
        namespace = "com.mrl.pixiv.multiplatform"
    }

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "17.0"
        framework {
            baseName = "ComposeApp"
            isStatic = true
            export(project(":common:core"))
        }
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":lib_strings"))
                implementation(project(":common:data"))
                implementation(project(":common:datasource-local"))
                implementation(project(":common:network"))
                implementation(project(":common:repository"))
                implementation(project(":common:ui"))
                api(project(":common:core"))
                rootDir.resolve("feature").listFiles()?.filter { it.isDirectory }?.forEach {
                    implementation(project(":feature:${it.name}"))
                }
                implementation(composes.bundles.navigation3)
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
                implementation(composes.bundles.navigation3.android)
            }
        }
        iosMain {
            dependencies {

            }
        }
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }

    configureRemoveKoinMeta()
}

compose.desktop {
    application {
        mainClass = "com.mrl.pixiv.MainKt"

        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = rootProject.name
            packageVersion = properties["versionName"]?.toString()
            windows {
                iconFile.set(file("icons/pipixiv.ico"))
                shortcut = true
            }
            linux {
                iconFile.set(file("icons/pipixiv.png"))
                shortcut = true
            }
            macOS { iconFile.set(file("icons/pipixiv.icns")) }
        }

        buildTypes.release.proguard {
            version = "7.8.2"
        }

        // release mode
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs(
            "--add-opens",
            "java.desktop/java.awt.peer=ALL-UNNAMED"
        ) // recommended but not necessary

        if ("Mac" in System.getProperty("os.name")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
        afterEvaluate {
            tasks.withType<JavaExec> {
                jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
                jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
                jvmArgs("--enable-native-access", "ALL-UNNAMED")

                if (System.getProperty("os.name").contains("Mac")) {
                    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
                    jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
                    jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
                }
            }
        }
    }
}

logger.quiet("debug: ${properties["debug"]}")

if (properties["debug"] != "true") {
    gradle.projectsEvaluated {
        tasks.named("proguardReleaseJars").configure {
            doFirst {
                layout.buildDirectory.file("compose/binaries/main-release/proguard")
                    .get().asFile.mkdirs()
            }
        }
    }

    tasks.withType(AbstractProguardTask::class.java) {
        val proguardFile = File.createTempFile("tmp", ".pro", temporaryDir)
        proguardFile.deleteOnExit()

        compose.desktop.application.buildTypes.release.proguard {
            configurationFiles.from(proguardFile, file("compose-desktop.pro"))
            optimize = false // fixme(tarsin): proguard internal error
            obfuscate = true
            joinOutputJars = true
        }

        doFirst {
            proguardFile.bufferedWriter().use { proguardFileWriter ->
                sourceSets["jvmMain"].runtimeClasspath
                    .filter { it.extension == "jar" }
                    .forEach { jar ->
                        val zip = zipTree(jar)
                        zip.matching { include("META-INF/**/proguard/*.pro") }.forEach {
                            proguardFileWriter.appendLine("########   ${jar.name} ${it.name}")
                            proguardFileWriter.appendLine(it.readText())
                        }
                        zip.matching { include("META-INF/services/*") }.forEach {
                            it.readLines().forEach { cls ->
                                val rule = "-keep class $cls"
                                proguardFileWriter.appendLine(rule)
                            }
                        }
                    }
            }
        }
    }
} else {
    compose.desktop.application.buildTypes.release.proguard {
        isEnabled = false
    }
}

fun ipaArguments(
    destination: String = "generic/platform=iOS",
    sdk: String = "iphoneos",
): Array<String> {
    return arrayOf(
        "xcodebuild",
        "-workspace", "PiPixiv.xcworkspace",
        "-scheme", "PiPixiv",
        "-destination", destination,
        "-sdk", sdk,
        "CODE_SIGNING_ALLOWED=NO",
        "CODE_SIGNING_REQUIRED=NO",
    )
}

val buildReleaseArchive = tasks.register("buildReleaseArchive", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Release"
    workingDir(rootDir.resolve("iosApp"))

    dependsOn(":composeApp:linkPodReleaseFrameworkIosArm64")
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

tasks.matching { it.name == "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("generateKotzillaConfig")
}
