package com.mrl.pixiv.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageContentModeDefault
import platform.Photos.PHImageManager
import platform.Photos.PHImageManagerMaximumSize
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object ShareUtil {
    actual fun shareText(text: String) {
        val controller = UIActivityViewController(listOf(text), null)
        presentViewController(controller)
    }

    /**
     * 分享指定的图片到其他应用。
     *
     * @param imageUri ph://<PHAsset.localIdentifier>。
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun shareImage(imageUri: String) {
        if (imageUri.startsWith("ph://")) {
            val localIdentifier = imageUri.removePrefix("ph://")
            val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localIdentifier), null)
            val asset = fetchResult.firstObject as? PHAsset

            if (asset != null) {
                val options = PHImageRequestOptions()
                options.networkAccessAllowed = true
                options.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat

                PHImageManager.defaultManager().requestImageForAsset(
                    asset,
                    targetSize = PHImageManagerMaximumSize.readValue(),
                    contentMode = PHImageContentModeDefault,
                    options = options
                ) { image, _ ->
                    if (image is UIImage) {
                        dispatch_async(dispatch_get_main_queue()) {
                            val controller = UIActivityViewController(listOf(image), null)
                            presentViewController(controller)
                        }
                    }
                }
            }
        } else {
            val url = NSURL.fileURLWithPath(imageUri)
            val controller = UIActivityViewController(listOf(url), null)
            presentViewController(controller)
        }
    }

    private fun presentViewController(controller: UIViewController) {
        val window = UIApplication.sharedApplication.keyWindow
            ?: UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow

        var topController = window?.rootViewController
        while (topController?.presentedViewController != null) {
            topController = topController.presentedViewController
        }

        topController?.presentViewController(controller, true, null)
    }
}
