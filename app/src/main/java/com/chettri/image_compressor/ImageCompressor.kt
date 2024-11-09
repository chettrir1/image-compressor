package com.chettri.image_compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class ImageCompressor(
    private val context: Context,
) {
    suspend fun compressImage(
        contentUri: Uri,
        compressThreshold: Long,
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            /*get the type of the file*/
            val mimeType = context
                .contentResolver
                .getType(contentUri)

            /*get the input bytes of the actual file*/
            val inputBytes = context
                .contentResolver
                .openInputStream(contentUri).use { inputStream ->
                    inputStream?.readBytes()
                } ?: return@withContext null

            ensureActive()

            withContext(Dispatchers.Default) {

                /*convert the file taking a bytes to a bitmap*/
                val newBitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)

                ensureActive()

                //checking the actual mimeType of the image and handling w.r.t the image type
                val compressFormat = when (mimeType) {
                    "image/jpeg" -> Bitmap.CompressFormat.JPEG
                    "image/png" -> Bitmap.CompressFormat.PNG
                    "image/webp" -> if (Build.VERSION.SDK_INT >= 30) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }

                    else ->
                        Bitmap.CompressFormat.JPEG
                }

                var outputBytes: ByteArray
                var quality = 90
                do {
                    ByteArrayOutputStream().use { outputStream ->
                        newBitmap.compress(compressFormat, quality, outputStream)
                        outputBytes = outputStream.toByteArray()
                        quality -= (quality * 0.1).roundToInt()
                    }
                } while (isActive &&
                    outputBytes.size > compressThreshold &&
                    quality > 5 &&
                    compressFormat != Bitmap.CompressFormat.PNG
                )
                outputBytes
            }
        }
    }
}