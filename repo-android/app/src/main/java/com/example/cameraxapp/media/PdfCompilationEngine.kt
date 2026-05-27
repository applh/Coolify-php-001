package com.example.cameraxapp.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfCompilationEngine {

    enum class PageSize {
        A4, LETTER, ORIGINAL_IMAGE_SIZE
    }

    enum class PageOrientation {
        PORTRAIT, LANDSCAPE
    }

    data class PdfConfig(
        val pageSize: PageSize = PageSize.A4,
        val orientation: PageOrientation = PageOrientation.PORTRAIT,
        val marginPixels: Int = 36 // ~0.5 inch under standard 72-points-per-inch PDF formats
    )

    /**
     * Compiles a sequence of visual files directly on disk into a unified PDF.
     */
    suspend fun compileImagesToPdf(
        context: Context,
        imageFiles: List<File>,
        outputFile: File,
        config: PdfConfig = PdfConfig()
    ): Boolean = withContext(Dispatchers.Default) {
        val pdfDocument = PdfDocument()

        // Page sizes configurations under standard 72 points per inch rule:
        // A4: 595 x 842 points | Letter: 612 x 792 points
        val baseWidth = if (config.pageSize == PageSize.LETTER) 612 else 595
        val baseHeight = if (config.pageSize == PageSize.LETTER) 792 else 842
        
        val targetWidth = if (config.orientation == PageOrientation.PORTRAIT) baseWidth else baseHeight
        val targetHeight = if (config.orientation == PageOrientation.PORTRAIT) baseHeight else baseWidth

        try {
            imageFiles.forEachIndexed { index, file ->
                if (!file.exists()) return@forEachIndexed

                // Memory safeguard: Downsample large photo layouts
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                
                val sourceBitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@forEachIndexed

                val pageW = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.width else targetWidth
                val pageH = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.height else targetHeight

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Calculate scaling factor to safely fit inside margins
                val usableW = pageW - (config.marginPixels * 2)
                val usableH = pageH - (config.marginPixels * 2)

                val scaleX = usableW.toFloat() / sourceBitmap.width
                val scaleY = usableH.toFloat() / sourceBitmap.height
                val scale = minOf(scaleX, scaleY, 1.0f)

                val drawW = (sourceBitmap.width * scale).toInt()
                val drawH = (sourceBitmap.height * scale).toInt()

                // Center coordinates alignments inside margins
                val startX = config.marginPixels + (usableW - drawW) / 2
                val startY = config.marginPixels + (usableH - drawH) / 2

                val destRect = Rect(startX, startY, startX + drawW, startY + drawH)
                
                // Draw imagery and finish page
                canvas.drawBitmap(sourceBitmap, null, destRect, null)
                pdfDocument.finishPage(page)
                
                // Free native bitmap memory immediately
                sourceBitmap.recycle()
            }

            // Save PDF to path
            if (outputFile.parentFile?.exists() == false) {
                outputFile.parentFile?.mkdirs()
            }
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Compiles a sequence of URIs (e.g. from PhotoPicker loader) into a unified PDF.
     */
    suspend fun compileImagesToPdfFromUris(
        context: Context,
        imageUris: List<Uri>,
        outputFile: File,
        config: PdfConfig = PdfConfig()
    ): Boolean = withContext(Dispatchers.Default) {
        val pdfDocument = PdfDocument()

        val baseWidth = if (config.pageSize == PageSize.LETTER) 612 else 595
        val baseHeight = if (config.pageSize == PageSize.LETTER) 792 else 842
        
        val targetWidth = if (config.orientation == PageOrientation.PORTRAIT) baseWidth else baseHeight
        val targetHeight = if (config.orientation == PageOrientation.PORTRAIT) baseHeight else baseWidth

        try {
            imageUris.forEachIndexed { index, uri ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                
                val sourceBitmap = context.contentResolver.openInputStream(uri).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@forEachIndexed

                val pageW = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.width else targetWidth
                val pageH = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.height else targetHeight

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val usableW = pageW - (config.marginPixels * 2)
                val usableH = pageH - (config.marginPixels * 2)

                val scaleX = usableW.toFloat() / sourceBitmap.width
                val scaleY = usableH.toFloat() / sourceBitmap.height
                val scale = minOf(scaleX, scaleY, 1.0f)

                val drawW = (sourceBitmap.width * scale).toInt()
                val drawH = (sourceBitmap.height * scale).toInt()

                val startX = config.marginPixels + (usableW - drawW) / 2
                val startY = config.marginPixels + (usableH - drawH) / 2

                val destRect = Rect(startX, startY, startX + drawW, startY + drawH)
                
                canvas.drawBitmap(sourceBitmap, null, destRect, null)
                pdfDocument.finishPage(page)
                
                sourceBitmap.recycle()
            }

            if (outputFile.parentFile?.exists() == false) {
                outputFile.parentFile?.mkdirs()
            }
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }
}
