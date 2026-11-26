@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.mrl.pixiv.common.util

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.asAwtTransferable
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Paths
import javax.imageio.ImageIO

actual suspend fun copyToClipboard(text: String) {
    val clipboard = desktopWindowServices().clipboard
    // Keep GTK clipboard access on Main without blocking its asynchronous callbacks.
    withContext(Dispatchers.Main.immediate) {
        clipboard.writePlainText(text)
    }
}

actual suspend fun readTextFromClipboard(): String? = try {
    val clipboard = desktopWindowServices().clipboard
    withContext(Dispatchers.Main.immediate) { clipboard.readPlainText() }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

actual suspend fun readLinkTextFromClipboard(): String? = readTextFromClipboard()

actual suspend fun clipboardChangeCount(): Long? = null

internal suspend fun Clipboard.writePlainText(text: String) {
    setClipEntry(ClipEntry(StringSelection(text)))
}

internal suspend fun Clipboard.readPlainText(): String? {
    val transferable = getClipEntry()?.asAwtTransferable ?: return null
    return if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        transferable.getTransferData(DataFlavor.stringFlavor) as? String
    } else {
        null
    }
}

suspend fun copyImageToClipboard(imageUri: String) {
    val bitmap = try {
        val file = if (imageUri.startsWith("file:")) {
            Paths.get(URI(imageUri)).toFile()
        } else {
            File(imageUri)
        }
        file.readBytes()
    } catch (e: Exception) {
        Logger.e(throwable = e, tag = "CopyUtil") { "Unable to read clipboard image source" }
        null
    } ?: return

    when (platform) {
        Platform.Desktop.MacOS -> {
            copyImageAsImageToClipboardOnMacOS(bitmap)
        }

        Platform.Desktop.Windows -> {
            copyImageToClipboardOnWindows(bitmap)
        }

        else -> {
            desktopWindowServices().clipboard.setClipEntry(ClipEntry(TransferableImage(bitmap)))
        }
    }
}

private suspend fun copyImageAsImageToClipboardOnMacOS(imageBytes: ByteArray) {
    withContext(Dispatchers.IO) {
        val pngFile = File.createTempFile("clipboard_img_", ".png")
        pngFile.writeBytes(imageBytes)

        val tiffFile = File.createTempFile("clipboard_img_", ".tiff")
        if (tiffFile.exists()) tiffFile.delete()

        val sips = ProcessBuilder(
            "sips",
            "-s",
            "format",
            "tiff",
            pngFile.absolutePath,
            "--out",
            tiffFile.absolutePath
        )
            .redirectErrorStream(true)
            .start()
        val sipsExit = sips.waitFor()
        if (sipsExit != 0) {
            // 清理并抛出或记录错误
            pngFile.delete()
            tiffFile.delete()
            throw RuntimeException("sips transferred failed，exit=$sipsExit")
        }

        val appleScriptCmd = listOf(
            "osascript",
            "-e",
            "set the clipboard to (read (POSIX file \"${tiffFile.absolutePath}\") as TIFF picture)",
        )
        val osascript = ProcessBuilder(appleScriptCmd)
            .redirectErrorStream(true)
            .start()
        val osExit = osascript.waitFor()
        if (osExit != 0) {
            pngFile.delete()
            tiffFile.delete()
            throw RuntimeException("osascript execute failed，exit=$osExit")
        }
        pngFile.delete()
        tiffFile.delete()
    }
}

private suspend fun copyImageToClipboardOnWindows(imageBytes: ByteArray) {
    withContext(Dispatchers.IO) {
        val pngFile = File.createTempFile("clipboard_img_", ".png")
        pngFile.writeBytes(imageBytes)

        val psScript = """
            Add-Type -AssemblyName System.Windows.Forms
            [System.Windows.Forms.Clipboard]::SetImage([System.Drawing.Image]::FromFile("${
            pngFile.absolutePath.replace(
                "\\",
                "\\\\"
            )
        }"))
        """.trimIndent()

        val process = ProcessBuilder(
            "powershell", "-NoProfile", "-Command", psScript
        )
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        pngFile.delete()

        if (exitCode != 0) {
            throw RuntimeException("PowerShell clipboard copy failed, exit=$exitCode")
        }
    }
}

private data class TransferableImage(private val image: ByteArray) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = flavor in transferDataFlavors

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (flavor == DataFlavor.imageFlavor) {
            return ImageIO.read(ByteArrayInputStream(image))
        }
        throw UnsupportedFlavorException(flavor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransferableImage

        return image.contentEquals(other.image)
    }

    override fun hashCode(): Int = image.contentHashCode()
}
