package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

object ReceiptImageHelper {
  private const val MAX_DIMENSION = 480
  private const val MAX_BASE64_LENGTH = 180_000

  fun uriToCompressedBase64(context: Context, uri: Uri): String? {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
      val scaled = scaleBitmap(original, MAX_DIMENSION)
      if (scaled !== original) original.recycle()

      var quality = 70
      var encoded = ""
      while (quality >= 25) {
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val bytes = baos.toByteArray()
        encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (encoded.length <= MAX_BASE64_LENGTH) break
        quality -= 15
      }
      scaled.recycle()
      encoded.takeIf { it.length <= MAX_BASE64_LENGTH }
    } catch (_: Exception) {
      null
    }
  }

  fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
      null
    }
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDim && height <= maxDim) return bitmap
    val ratio = width.toFloat() / height.toFloat()
    val targetW: Int
    val targetH: Int
    if (width > height) {
      targetW = maxDim
      targetH = (maxDim / ratio).toInt().coerceAtLeast(1)
    } else {
      targetH = maxDim
      targetW = (maxDim * ratio).toInt().coerceAtLeast(1)
    }
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }
}
