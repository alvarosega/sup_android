package com.alvarosega.trackingventas.sanitization.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressor {

    private const val MAX_WIDTH = 1280
    private const val MAX_HEIGHT = 720
    private const val QUALITY = 75

    suspend fun compressImageFile(context: Context, originalFile: File): File = withContext(Dispatchers.IO) {
        val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
            ?: return@withContext originalFile

        val exif = ExifInterface(originalFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotatedBitmap = rotateBitmap(originalBitmap, orientation)

        val width = rotatedBitmap.width
        val height = rotatedBitmap.height
        val ratio = max(width.toFloat() / MAX_WIDTH, height.toFloat() / MAX_HEIGHT)

        val scaledBitmap = if (ratio > 1.0f) {
            val targetWidth = (width / ratio).roundToInt()
            val targetHeight = (height / ratio).roundToInt()
            Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)
        } else {
            rotatedBitmap
        }

        val compressedDir = File(context.filesDir, "audit_photos")
        if (!compressedDir.exists()) {
            compressedDir.mkdirs()
        }
        val compressedFile = File(compressedDir, "compressed_${originalFile.name}")

        FileOutputStream(compressedFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        }

        if (originalFile.exists() && originalFile.absolutePath != compressedFile.absolutePath) {
            originalFile.delete()
        }

        compressedFile
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}