package com.siddarth.criminalintent.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

object EvidenceDecoder {
    fun decode(file: File): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = 1 }
        // Decode a preview-sized bitmap instead of keeping a full camera image in memory.
        while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1400) {
            options.inSampleSize *= 2
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val metadata = ExifInterface(file)
        // Camera files can store rotation and mirroring in EXIF rather than in their pixels.
        val transform = Matrix().apply {
            if (metadata.isFlipped) postScale(-1f, 1f)
            postRotate(metadata.rotationDegrees.toFloat())
        }
        if (transform.isIdentity) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true)
        }
    }.getOrNull()
}
