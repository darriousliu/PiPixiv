package com.mrl.pixiv.common.util

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.net.URI
import java.nio.file.Paths
import javax.imageio.ImageIO

actual fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val selection = StringSelection(text)
    clipboard.setContents(selection, null)
}

fun copyImageToClipboard(imageUri: String) {
    val image = try {
        val file = if (imageUri.startsWith("file:")) {
            Paths.get(URI(imageUri)).toFile()
        } else {
            File(imageUri)
        }
        ImageIO.read(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } ?: return

    val selection = object : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DataFlavor.imageFlavor)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return DataFlavor.imageFlavor == flavor
        }

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) {
                throw UnsupportedFlavorException(flavor)
            }
            return image
        }
    }
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
}
