package com.sphereon.kiwa.sample.app.qr

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val nsData = NSData.create(bytes = this, length = size.toULong())
    val uiImage = UIImage(nsData)
    return uiImage.toComposeImageBitmap()
}
