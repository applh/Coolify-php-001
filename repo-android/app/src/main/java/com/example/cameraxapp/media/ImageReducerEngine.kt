package com.example.cameraxapp.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageReducerEngine {

    enum class OutputFormat {
        JPEG, PNG, WEBP_LOSSY, WEBP_LOSSLESS
    }

    data class CompressionConfig(
        val quality: Int = 80,         // 1 to 100
        val scale: Float = 0.8f,       // 0.1f to 1.0f
        val targetFormat: OutputFormat = OutputFormat.JPEG
    )

    data class CompressionResult(
        val originalSize: Long,
        val compressedSize: Long,
        val outputPath: String,
        val savedBytes: Long
    )

    suspend fun compressAndResizeImage(
        context: Context,
        inputUri: Uri,
        outputDirectory: File,
        config: CompressionConfig
    ): CompressionResult = withContext(Dispatchers.Default) {
        val originalSize = getUriSize(context, inputUri)
        
        // 1. Open Stream to read dimensions (bounds only)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(inputUri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // 2. Calculate optimal inSampleSize for memory safety
        val targetWidth = (options.outWidth * config.scale).toInt()
        val targetHeight = (options.outHeight * config.scale).toInt()
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetWidth, targetHeight)
        options.inJustDecodeBounds = false

        // 3. Decode bitmap with sampling
        val sampledBitmap = context.contentResolver.openInputStream(inputUri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalStateException("Failed to decode bitmap from source URI")

        // 4. Perform precise fractional scaling via Matrix if sample size wasn't exact enough
        val finalBitmap = if (config.scale < 1.0f) {
            val matrix = Matrix().apply {
                postScale(config.scale, config.scale)
            }
            val scaled = Bitmap.createBitmap(
                sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true
            )
            if (scaled != sampledBitmap) {
                sampledBitmap.recycle()
            }
            scaled
        } else {
            sampledBitmap
        }

        // Prepare output file structure
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        val extension = when (config.targetFormat) {
            OutputFormat.JPEG -> "jpg"
            OutputFormat.PNG -> "png"
            OutputFormat.WEBP_LOSSY, OutputFormat.WEBP_LOSSLESS -> "webp"
        }
        val outputFile = File(outputDirectory, "COMP_${UUID.randomUUID()}.$extension")

        // 5. Compress and write
        val format = when (config.targetFormat) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP_LOSSY -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            OutputFormat.WEBP_LOSSLESS -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }

        FileOutputStream(outputFile).use { outStream ->
            finalBitmap.compress(format, config.quality, outStream)
        }

        // Clear memory bindings
        finalBitmap.recycle()

        val compressedSize = outputFile.length()
        val savedBytes = originalSize - compressedSize

        CompressionResult(
            originalSize = originalSize,
            compressedSize = compressedSize,
            outputPath = outputFile.absolutePath,
            savedBytes = if (savedBytes > 0) savedBytes else 0L
        )
    }

    suspend fun compressAndResizeImage(
        context: Context,
        inputFile: File,
        outputDirectory: File,
        config: CompressionConfig
    ): CompressionResult = withContext(Dispatchers.Default) {
        val originalSize = inputFile.length()
        
        // 1. Read dimensions only
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(inputFile.absolutePath, options)

        // 2. Calculate optimal sample size
        val targetWidth = (options.outWidth * config.scale).toInt()
        val targetHeight = (options.outHeight * config.scale).toInt()
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetWidth, targetHeight)
        options.inJustDecodeBounds = false

        // 3. Decode bitmap
        val sampledBitmap = BitmapFactory.decodeFile(inputFile.absolutePath, options)
            ?: throw IllegalStateException("Failed to decode bitmap from source file: ${inputFile.name}")

        // 4. Perform scaling
        val finalBitmap = if (config.scale < 1.0f) {
            val matrix = Matrix().apply {
                postScale(config.scale, config.scale)
            }
            val scaled = Bitmap.createBitmap(
                sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true
            )
            if (scaled != sampledBitmap) {
                sampledBitmap.recycle()
            }
            scaled
        } else {
            sampledBitmap
        }

        // Prepare output
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        val extension = when (config.targetFormat) {
            OutputFormat.JPEG -> "jpg"
            OutputFormat.PNG -> "png"
            OutputFormat.WEBP_LOSSY, OutputFormat.WEBP_LOSSLESS -> "webp"
        }
        val outputFile = File(outputDirectory, "COMP_${UUID.randomUUID()}.$extension")

        // 5. Compress and write
        val format = when (config.targetFormat) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP_LOSSY -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            OutputFormat.WEBP_LOSSLESS -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }

        FileOutputStream(outputFile).use { outStream ->
            finalBitmap.compress(format, config.quality, outStream)
        }

        // Clear memory
        finalBitmap.recycle()

        val compressedSize = outputFile.length()
        val savedBytes = originalSize - compressedSize

        CompressionResult(
            originalSize = originalSize,
            compressedSize = compressedSize,
            outputPath = outputFile.absolutePath,
            savedBytes = if (savedBytes > 0) savedBytes else 0L
        )
    }

    private fun getUriSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
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
}
