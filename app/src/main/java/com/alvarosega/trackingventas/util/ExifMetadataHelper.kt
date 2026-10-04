package com.alvarosega.trackingventas.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object ExifMetadataHelper {

    private const val MAX_WIDTH_PX = 1280
    private const val JPEG_QUALITY = 75

    /**
     * Redimensiona, comprime la imagen a ~180KB e inyecta los metadatos EXIF oficiales
     * sellados con el timestamp del servidor y las coordenadas GPS.
     */
    fun compressAndInjectMetadata(
        photoFile: File,
        location: Location,
        sellerCode: String,
        serverTimeMillis: Long
    ) {
        if (!photoFile.exists()) return

        // 1. Decodificar tamaño original para escalar sin saturar la memoria RAM
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoFile.absolutePath, boundsOptions)

        val originalWidth = boundsOptions.outWidth
        val originalHeight = boundsOptions.outHeight

        var sampleSize = 1
        while ((originalWidth / sampleSize) > MAX_WIDTH_PX * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // Optimiza 50% de memoria frente a ARGB_8888
        }

        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, decodeOptions) ?: return

        // 2. Escalar proporcionalmente si el ancho supera 1280px
        val finalBitmap = if (originalBitmap.width > MAX_WIDTH_PX) {
            val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width.toFloat()
            val targetHeight = (MAX_WIDTH_PX * aspectRatio).roundToInt()
            Bitmap.createScaledBitmap(originalBitmap, MAX_WIDTH_PX, targetHeight, true)
        } else {
            originalBitmap
        }

        // 3. Sobrescribir el archivo local con la compresión al 75%
        FileOutputStream(photoFile).use { outStream ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outStream)
            outStream.flush()
        }

        if (finalBitmap != originalBitmap) {
            originalBitmap.recycle()
        }
        finalBitmap.recycle()

        // 4. Inyectar metadatos EXIF limpios
        val exif = ExifInterface(photoFile.absolutePath)

        val latRef = if (location.latitude >= 0) "N" else "S"
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToDms(abs(location.latitude)))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef)

        val lonRef = if (location.longitude >= 0) "E" else "W"
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToDms(abs(location.longitude)))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef)

        // Sello criptográfico temporal tomado de ServerTimeManager
        val gpsDate = SimpleDateFormat("yyyy:MM:dd", Locale.US).format(Date(serverTimeMillis))
        val gpsTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(serverTimeMillis))
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, gpsDate)
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, gpsTime)

        exif.setAttribute(
            ExifInterface.TAG_USER_COMMENT,
            "SELLER:$sellerCode|ACCURACY:${location.accuracy}|MOCK:${location.isFromMockProvider}"
        )

        exif.saveAttributes()
    }

    private fun decimalToDms(coordinate: Double): String {
        val degrees = coordinate.toInt()
        val remainder = (coordinate - degrees) * 60.0
        val minutes = remainder.toInt()
        val seconds = ((remainder - minutes) * 60.0 * 1000).toInt()
        return "$degrees/1,$minutes/1,$seconds/1000"
    }
}