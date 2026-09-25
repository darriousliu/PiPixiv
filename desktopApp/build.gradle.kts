import com.mrl.pixiv.buildsrc.configureDesktopSentryMapping
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import dev.nucleusframework.desktop.application.tasks.AbstractElectronBuilderPackageTask
import dev.nucleusframework.desktop.application.tasks.AbstractGenerateAotCacheTask
import dev.nucleusframework.desktop.application.tasks.AbstractJPackageTask
import dev.nucleusframework.desktop.application.tasks.AbstractProguardTask

plugins {
    id("pixiv.desktop.compose")
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.nucleus)
}

val desktopOsName = System.getProperty("os.name").toString()
// AOT trades additional cache bytes for startup work; keep size-focused builds unchanged.
val desktopAotEnabled = providers.gradleProperty("desktopAot")
    .map(String::toBooleanStrict)
    .getOrElse(false)
val (mmkvNativeLibraryName, mmkvNativeLibraryDependency) = when {
    desktopOsName == "Mac OS X" ->
        "libmmkvc.dylib" to libs.mmkv.kotlin.nativelib.macos

    desktopOsName.startsWith("Windows") ->
        "mmkvc.dll" to libs.mmkv.kotlin.nativelib.windows

    desktopOsName.startsWith("Linux") ->
        "libmmkvc.so" to libs.mmkv.kotlin.nativelib.linux

    else -> error("Unsupported desktop OS: $desktopOsName")
}

val mmkvNativeLibrary = configurations.create("mmkvNativeLibrary") {
    description = "MMKV native library packaged by the desktop launcher"
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    add(mmkvNativeLibrary.name, mmkvNativeLibraryDependency)
}

val desktopResourcesDirectory = layout.buildDirectory.dir("generated/desktopResources")
val mmkvResourcesDirectory = desktopResourcesDirectory.map { it.dir("composeResources/files/mmkv") }
val packagedMMKVLibraryPath = "composeResources/files/mmkv/$mmkvNativeLibraryName"

val copyMMKVNativeLibraryToDesktopResources =
    tasks.register("copyMMKVNativeLibraryToDesktopResources", Sync::class) {
        group = "build"
        description = "Copies the current platform's MMKV native library into Compose app resources"

        val nativeLibraryArchives = mmkvNativeLibrary.incoming.files.elements.map { artifacts ->
            artifacts.map { zipTree(it.asFile) }
        }
        from(nativeLibraryArchives) {
            include(mmkvNativeLibraryName)
        }
        into(mmkvResourcesDirectory)

        doLast {
            val copiedLibrary = destinationDir.resolve(mmkvNativeLibraryName)
            check(copiedLibrary.isFile) {
                "MMKV native library $mmkvNativeLibraryName was not found in ${mmkvNativeLibrary.files}"
            }
        }
    }

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":common:core"))
    implementation(project(":common:data"))
    implementation(project(":common:network"))
    implementation(project(":common:repository"))
    implementation(project(":common:ui"))
    implementation(project(":lib_strings"))
    implementation(compose.desktop.currentOs)
    implementation(libs.bundles.compose.baselibs)
    implementation(libs.compose.jetbrains.compose.resources)
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.window.tao)
    implementation(libs.bundles.koin)
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.coil3.bom))
    implementation(libs.bundles.coil3)
    implementation(libs.zoomimage.core)
    implementation(libs.kotlinx.ktor.client.core)
    implementation(libs.filekit.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":feature:image-preview"))
    testImplementation(libs.bundles.compose.navigation3)
}

nucleus {
    application {
        mainClass = "com.mrl.pixiv.MainKt"

        nativeDistributions {
            includeAllModules = true
            cleanupNativeLibs = true
            enableAotCache = desktopAotEnabled
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.RawAppImage)
            packageName = rootProject.name
            packageVersion = findProperty("versionName")?.toString()
            windows {
                iconFile.set(file("icons/pipixiv.ico"))
                shortcut = true
                msi {
                    perMachine = false
                    oneClick = false
                }
                msiPackageVersion = findProperty("versionName")?.toString()
                upgradeUuid = "650ae9c7-32ad-400e-93f3-6b0874eccc1c"
                menuGroup = rootProject.name
            }
            linux {
                iconFile.set(file("icons/pipixiv.png"))
                shortcut = true
            }
            macOS {
                iconFile.set(file("icons/pipixiv.icns"))
                bundleID = "com.mrl.pixiv"
                // The bundled MMKV native library targets macOS 12.0.
                minimumSystemVersion = "12.0"
            }
        }

        buildTypes.release.proguard {
            version.set("7.9.1")
        }

        jvmArgs("--enable-native-access", "ALL-UNNAMED", "-Djava.net.useSystemProxies=true")
        if (desktopOsName == "Mac OS X") {
            jvmArgs("-XstartOnFirstThread")
        }
    }
}

tasks.withType<AbstractGenerateAotCacheTask>().configureEach {
    safetyTimeoutSeconds.set(90)
    doFirst {
        // Nucleus 2.5.14 only checks that a cache exists after training.
        // Never let an older cache hide a failed training process.
        distributableDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.name == "app.aot" }
            .forEach { cache -> check(cache.delete()) { "Cannot remove stale AOT cache: $cache" } }
    }
}

if (desktopAotEnabled && findProperty("debug") != "true") {
    val verifyReleaseAotCache = tasks.register<Exec>("verifyReleaseAotCache") {
        group = "verification"
        description = "Requires the packaged JVM to load its freshly trained AOT cache"
        dependsOn("generateReleaseAotCache")
        // The training branch exits before opening a window or initializing user data/analytics.
        environment("JAVA_TOOL_OPTIONS", "-XX:AOTMode=on -Dnucleus.aot.mode=training -Xlog:aot=info")
        doFirst {
            val distribution = tasks.named<AbstractGenerateAotCacheTask>("generateReleaseAotCache")
                .get().distributableDir.get().asFile
            val appName = rootProject.name
            val launcher = when {
                desktopOsName == "Mac OS X" -> distribution.resolve("$appName.app/Contents/MacOS/$appName")
                desktopOsName.startsWith("Windows") -> distribution.resolve("$appName/$appName.exe")
                else -> distribution.resolve("$appName/bin/$appName")
            }
            check(launcher.isFile) { "Packaged launcher not found: $launcher" }
            commandLine(launcher.absolutePath)
        }
    }
    tasks.matching { it.name in setOf("packageReleaseDmg", "packageReleaseMsi") }.configureEach {
        dependsOn(verifyReleaseAotCache)
    }
}

val prepareDesktopAppResources = tasks.withType<Sync>().matching { it.name == "prepareAppResources" }
prepareDesktopAppResources.configureEach {
    dependsOn(copyMMKVNativeLibraryToDesktopResources)
    // Include a real file in the installer; loading MMKV must never extract resources at startup.
    from(desktopResourcesDirectory) {
        include(packagedMMKVLibraryPath)
    }
}

val verifyMMKVNativeLibraryPackaging = tasks.register("verifyMMKVNativeLibraryPackaging") {
    group = "verification"
    description = "Checks that MMKV is available as a native distribution resource"
    dependsOn(prepareDesktopAppResources)

    val libraryFile = providers.provider {
        prepareDesktopAppResources.single().destinationDir.resolve(packagedMMKVLibraryPath)
    }
    inputs.file(libraryFile)

    doLast {
        val library = libraryFile.get()
        check(library.isFile && library.length() > 0) {
            "MMKV native library is missing from the desktop distribution resources: $library"
        }
    }
}

tasks.withType<AbstractJPackageTask>().configureEach {
    dependsOn(verifyMMKVNativeLibraryPackaging)
}

tasks.withType<AbstractElectronBuilderPackageTask>().configureEach {
    doFirst {
        // Nucleus 会复用已有的元数据，打包前重新生成以确保安装包使用当前版本。
        val packageMetadata = destinationDir.file("package.json").get().asFile
        check(!packageMetadata.exists() || packageMetadata.delete()) {
            "无法清理旧的桌面安装包元数据：$packageMetadata"
        }
    }
}

val directJvmRunTasks = setOf("run", "hotRun", "hotDev")
tasks.withType(JavaExec::class.java).configureEach {
    if (name in directJvmRunTasks) {
        systemProperty("java.net.useSystemProxies", "true")
        dependsOn(copyMMKVNativeLibraryToDesktopResources)
        systemProperty(
            "compose.application.resources.dir",
            desktopResourcesDirectory.get().asFile.absolutePath,
        )
    }
}

tasks.matching { it.name == "hotRunAsync" || it.name == "hotDevAsync" }.configureEach {
    dependsOn(copyMMKVNativeLibraryToDesktopResources)
}

logger.quiet("debug: ${findProperty("debug")}")

if (findProperty("debug") != "true") {
    afterEvaluate {
        val proguardReleaseJars = tasks.named<AbstractProguardTask>("proguardReleaseJars")
        proguardReleaseJars.configure {
            doFirst {
                layout.buildDirectory.file("compose/binaries/main-release/proguard")
                    .get().asFile.mkdirs()
            }
        }
        configureDesktopSentryMapping(
            proguardTask = proguardReleaseJars,
            mappingFile = layout.buildDirectory.file("compose/binaries/main-release/proguard/mapping.txt"),
            outputJar = proguardReleaseJars.flatMap { task ->
                task.destinationDir.file(task.mainJarBaseName)
            },
            organization = "pipixiv",
            sentryProject = "pipixiv",
        )
    }

    tasks.withType(AbstractProguardTask::class.java) {
        val proguardFile = File.createTempFile("tmp", ".pro", temporaryDir)
        proguardFile.deleteOnExit()

        nucleus.application.buildTypes.release.proguard {
            configurationFiles.from(proguardFile, file("compose-desktop.pro"))
            optimize.set(false) // fixme(tarsin): proguard internal error
            // Sentry restores these names using the mapping UUID embedded before packaging.
            obfuscate.set(true)
            joinOutputJars.set(true)
        }

        doFirst {
            proguardFile.bufferedWriter().use { proguardFileWriter ->
                sourceSets["main"].runtimeClasspath
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
    nucleus.application.buildTypes.release.proguard {
        isEnabled.set(false)
    }
}
