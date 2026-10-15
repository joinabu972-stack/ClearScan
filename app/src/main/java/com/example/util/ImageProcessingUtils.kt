package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.FilterType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageProcessingUtils {

    fun applyFilter(source: Bitmap, filterType: FilterType): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (filterType) {
            FilterType.ORIGINAL -> {
                canvas.drawBitmap(source, 0f, 0f, paint)
                return output
            }

            FilterType.MAGIC -> {
                // High contrast, brightened whites, vibrant colors for clear documents
                val colorMatrix = ColorMatrix()
                val contrast = 1.35f
                val brightness = 20f
                val cm = floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(cm)
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                return output
            }

            FilterType.BLACK_WHITE -> {
                // High contrast thresholded monochrome (clean white background, deep dark text)
                val colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(0f)
                val m = colorMatrix.array
                // Increase contrast drastically
                val contrast = 2.4f
                val scale = contrast
                val translate = (-0.5f * contrast + 0.5f) * 255f + 15f
                val highContrast = floatArrayOf(
                    m[0] * scale, m[1] * scale, m[2] * scale, 0f, translate,
                    m[5] * scale, m[6] * scale, m[7] * scale, 0f, translate,
                    m[10] * scale, m[11] * scale, m[12] * scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(highContrast)
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                return output
            }

            FilterType.GRAYSCALE -> {
                val colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                return output
            }

            FilterType.SHARP -> {
                val colorMatrix = ColorMatrix()
                val contrast = 1.5f
                val brightness = 10f
                val cm = floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(cm)
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                return output
            }

            FilterType.VINTAGE -> {
                // Warm vintage paper tone
                val colorMatrix = ColorMatrix()
                val cm = floatArrayOf(
                    1.15f, 0f, 0f, 0f, 15f,
                    0f, 1.05f, 0f, 0f, 10f,
                    0f, 0f, 0.85f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                )
                colorMatrix.set(cm)
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
                return output
            }
        }
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, fileNamePrefix: String = "scan"): String {
        val dir = File(context.filesDir, "scans")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val file = File(dir, "${fileNamePrefix}_$timeStamp.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file.absolutePath
    }

    fun loadBitmapFromFile(filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a multi-page PDF from a list of image paths using native Android PdfDocument
     */
    fun generatePdf(
        context: Context,
        pageImagePaths: List<String>,
        documentTitle: String
    ): File? {
        if (pageImagePaths.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val standardWidth = 595 // Standard A4 width in PostScript points (72 DPI)
        val standardHeight = 842 // Standard A4 height in PostScript points

        try {
            for ((index, path) in pageImagePaths.withIndex()) {
                val bitmap = loadBitmapFromFile(path) ?: continue

                val pageInfo = PdfDocument.PageInfo.Builder(standardWidth, standardHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Draw clean white background
                canvas.drawColor(Color.WHITE)

                // Scale bitmap to fit within standard A4 while preserving aspect ratio
                val scale = minOf(
                    (standardWidth - 40f) / bitmap.width.toFloat(),
                    (standardHeight - 40f) / bitmap.height.toFloat()
                )
                val destWidth = bitmap.width * scale
                val destHeight = bitmap.height * scale
                val left = (standardWidth - destWidth) / 2f
                val top = (standardHeight - destHeight) / 2f

                val matrix = Matrix()
                matrix.postScale(scale, scale)
                matrix.postTranslate(left, top)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, matrix, paint)

                pdfDocument.finishPage(page)
            }

            val pdfDir = File(context.filesDir, "scans/pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val sanitizedTitle = documentTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val pdfFile = File(pdfDir, "${sanitizedTitle}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Share a file (PDF or Image) with external apps using FileProvider
     */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Share Scanned Document") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Share multiple files together
     */
    fun shareMultipleFiles(context: Context, files: List<File>, mimeType: String) {
        try {
            val uris = ArrayList<Uri>()
            for (file in files) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                uris.add(uri)
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Documents")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
