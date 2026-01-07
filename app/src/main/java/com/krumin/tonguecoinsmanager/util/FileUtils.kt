package com.krumin.tonguecoinsmanager.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun uriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile(
                com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.TEMP_PREFIX_UPLOAD,
                com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.EXTENSION_JPG,
                context.cacheDir
            )
            val outputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
