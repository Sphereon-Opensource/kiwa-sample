actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val nsData = NSData.create(bytes = this, length = size.toULong())
    val uiImage = UIImage(nsData)
    return uiImage.toComposeImageBitmap()
}
