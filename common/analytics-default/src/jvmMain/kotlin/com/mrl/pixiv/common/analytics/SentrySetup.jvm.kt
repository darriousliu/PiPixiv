package com.mrl.pixiv.common.analytics

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.filesDir
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.ScopeType
import io.sentry.SendCachedEnvelopeFireAndForgetIntegration
import io.sentry.SendFireAndForgetEnvelopeSender
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.protocol.App
import io.sentry.protocol.Device
import io.sentry.protocol.OperatingSystem
import io.sentry.protocol.SentryRuntime
import io.sentry.protocol.User
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.util.UUID
import io.sentry.Sentry as JavaSentry

internal actual fun initializePlatformSentry(dsn: String, appVersion: String, appBuild: Int) {
    val id = runCatching { desktopInstallationId(Path.of(FileKit.filesDir.absolutePath())) }.getOrNull()
    val cacheDir = runCatching {
        val directory = Path.of(FileKit.filesDir.absolutePath()).resolve("sentry-cache")
        Files.createDirectories(directory).takeIf(Files::isWritable)?.toString()
    }.getOrNull()
    Sentry.initWithPlatformOptions { options ->
        options.configureDesktopCrashReports(dsn, appVersion, appBuild, cacheDir)
        options.distinctId = id
    }
    if (!Sentry.isEnabled()) return
    configureCrashScope(id)
    JavaSentry.startSession()
    Runtime.getRuntime().addShutdownHook(Thread {
        JavaSentry.endSession()
        JavaSentry.flush(2_000)
    })
}

private fun configureCrashScope(userId: String?) {
    if (userId == null) return
    JavaSentry.configureScope(ScopeType.ISOLATION) { scope ->
        scope.user = User().apply { id = userId }
    }
}

private fun desktopInstallationId(directory: Path): String? {
    if (!directory.isAbsolute) return null
    Files.createDirectories(directory)
    val idFile = directory.resolve("sentry-installation-id")
    val lockFile = directory.resolve("sentry-installation-id.lock")
    return FileChannel.open(lockFile, CREATE, WRITE).use { channel ->
        channel.lock().use {
            val existing = if (Files.isRegularFile(idFile) && Files.size(idFile) <= 64) {
                val value = Files.readString(idFile).trim()
                runCatching { UUID.fromString(value) }.getOrNull()?.takeIf { uuid ->
                    uuid.version() == 4 && uuid.variant() == 2 && uuid.toString().equals(value, ignoreCase = true)
                }?.let { value }
            } else {
                null
            }
            existing ?: run {
                val id = UUID.randomUUID().toString()
                val temporary = Files.createTempFile(directory, "sentry-installation-id.", ".tmp")
                try {
                    Files.writeString(temporary, id)
                    Files.move(temporary, idFile, ATOMIC_MOVE, REPLACE_EXISTING)
                    id
                } finally {
                    Files.deleteIfExists(temporary)
                }
            }
        }
    }
}

internal fun SentryOptions.configureDesktopCrashReports(
    dsn: String,
    appVersion: String,
    appBuild: Int,
    cacheDir: String?,
) {
    configureCrashReports(dsn)
    isAttachServerName = false
    if (cacheDir != null) {
        cacheDirPath = cacheDir
        addIntegration(
            SendCachedEnvelopeFireAndForgetIntegration(SendFireAndForgetEnvelopeSender { cacheDirPath }),
        )
    }
    isGlobalHubMode = true
    val appRelease = "pipixiv@$appVersion"
    val build = appBuild.toString()
    release = appRelease
    dist = build

    val osName = System.getProperty("os.name").let { if (it == "Mac OS X") "macOS" else it }
    val osVersion = System.getProperty("os.version")
    val architecture = System.getProperty("os.arch")
    val jvmName = System.getProperty("java.vm.name")
    val jvmVersion = Runtime.version().toString()
    val jvmVendor = System.getProperty("java.vm.vendor")
    val runtime = Runtime.getRuntime()

    addEventProcessor(object : EventProcessor {
        override fun process(event: SentryEvent, hint: Hint): SentryEvent {
            // The SDK has already applied release/dist to new events. Skip older cached events.
            if (event.release != appRelease || event.dist != build) return event

            with(event.contexts) {
                // Sentry's nullable getters and non-null setters expose read-only Kotlin properties.
                @Suppress("UsePropertyAccessSyntax")
                setApp(App().apply {
                    appIdentifier = "com.mrl.pixiv"
                    appName = "PiPixiv"
                    this.appVersion = appVersion
                    this.appBuild = build
                })
                @Suppress("UsePropertyAccessSyntax")
                setOperatingSystem(OperatingSystem().apply {
                    name = osName
                    version = osVersion
                })
                @Suppress("UsePropertyAccessSyntax")
                setDevice(Device().apply {
                    // The Java SDK has no typed field for the schema's `device.arch`.
                    unknown = architecture?.let { mapOf("arch" to it) }
                    processorCount = runtime.availableProcessors()
                })
                setRuntime(SentryRuntime().apply {
                    name = jvmName
                    version = jvmVersion
                    unknown = jvmVendor?.let { mapOf("vendor" to it) }
                })
                val committed = runtime.totalMemory()
                this["jvm_heap"] = mapOf(
                    "max_bytes" to runtime.maxMemory(),
                    "committed_bytes" to committed,
                    "used_bytes" to (committed - runtime.freeMemory()).coerceAtLeast(0),
                )
            }
            return event
        }
    })
}
