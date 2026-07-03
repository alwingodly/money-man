package com.alwin.moneymanager.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Decodes a bitmap from disk downsampled to roughly [reqWidth]x[reqHeight] instead of loading it
 * at full resolution just to shrink it on screen — a `BitmapFactory.decodeFile` with no options
 * decodes (and allocates memory for) every pixel of the source file regardless of how small the
 * `Image` displaying it ends up being.
 */
fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
    }
    return BitmapFactory.decodeFile(path, options)
}

/**
 * Reads [source] via [resolver], downsamples so its longest edge is at most [maxDimension], and
 * writes the result to [destination] as a JPEG. A photo picked from the camera/gallery can be
 * several megabytes at 4000px+ per side; without this, that full-resolution file gets stored
 * forever and re-decoded in full on every screen that shows the avatar.
 */
fun downsampleImageToJpeg(
    resolver: ContentResolver,
    source: Uri,
    destination: File,
    maxDimension: Int,
    quality: Int = 85,
) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, maxDimension, maxDimension)
    }
    val bitmap = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return
    destination.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output) }
    bitmap.recycle()
}
