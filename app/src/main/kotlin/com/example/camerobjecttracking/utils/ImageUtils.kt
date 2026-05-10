package com.example.camerobjecttracking.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import android.media.ImagePrinter
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Extension pour convertir un ImageProxy (YUV_420_888) en Bitmap.
 * Basé sur la conversion NV21 → JPEG → Bitmap.
 */
fun ImageProxy.toBitmap(): Bitmap {
    // Conversion YUV_NV21 (format attendu par YuvImage)
    val yBuffer = this.planes[0].buffer // Y
    val uBuffer = this.planes[1].buffer // U
    val vBuffer = this.planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)  // V then U for NV21
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

/** Alternative plus rapide (si nécessaire) : utiliser RenderScript ou ScriptIntrinsicYuvToRGB. */
fun ImageProxy.toBitmapFast(): Bitmap {
    // TODO: implémenter avec allocation de buffer + copyYUVToCenter (plus efficace)
    return toBitmap()
}
