package com.example.cameraxapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageUtils {

    /**
     * Saves a [Bitmap] to the device gallery.
     * Handles API 29+ (Scoped Storage) and API 28- gracefully.
     * Returns the [Uri] string representing the saved media.
     */
    fun saveImageToGallery(context: Context, bitmap: Bitmap, prompt: String?): String? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val displayName = "Lumina_${timestamp}.jpg"
        val mimeType = "image/jpeg"

        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GeminiCanvas")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                try {
                    resolver.openOutputStream(imageUri).use { os ->
                        if (os != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                    return imageUri.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(imageUri, null, null)
                }
            }
        } else {
            // Older Android versions fallback
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "GeminiCanvas")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            val imageFile = File(appDir, displayName)
            try {
                FileOutputStream(imageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
                // Notify scan
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                return Uri.fromFile(imageFile).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * Converts a base64 string directly into a [Bitmap].
     */
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val cleanBase64 = base64Str.trim()
                .replace("\n", "")
                .replace("\r", "")
            val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Triggers the Android system share sheet for the generated Bitmap image.
     */
    fun shareImage(context: Context, bitmap: Bitmap, prompt: String) {
        try {
            // Write to local cache folder
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "lumina_shared.jpg")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }

            val authority = "${context.packageName}.provider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiver
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "✨ Generated image using Lumina AI!\nPrompt: \"$prompt\"")
                    type = "image/jpeg"
                }
                val chooserIntent = Intent.createChooser(shareIntent, "Share Masterpiece Canvas")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
