package com.example.backup.gateway

import android.content.Context
import android.net.Uri
import android.os.Build
import com.example.backup.model.BackupException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AndroidBackupFileGateway : BackupFileGateway {

    override suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw BackupException.ReadFailed("ContentResolver returned null input stream for URI: $uri")

            val buffer = ByteArray(8192)
            val baos = ByteArrayOutputStream()
            var bytesRead: Int
            var totalBytes = 0L
            val maxSizeBytes = 50 * 1024 * 1024L // 50MB max safety threshold

            inputStream.use { stream ->
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    if (totalBytes > maxSizeBytes) {
                        throw BackupException.ReadFailed("Backup file exceeds maximum allowed size of 50 MB.")
                    }
                    baos.write(buffer, 0, bytesRead)
                }
            }
            baos.toByteArray()
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException.ReadFailed(e.localizedMessage ?: "Failed to read content from document picker", e)
        }
    }

    override suspend fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri, "w")
                ?: throw BackupException.WriteFailed("ContentResolver returned null output stream for URI: $uri")

            outputStream.use { stream ->
                stream.write(bytes)
                stream.flush()
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException.WriteFailed(e.localizedMessage ?: "Failed to write backup file bytes to storage", e)
        }
    }

    override fun buildDiagnosticsString(e: Exception): String {
        return """
            |EXCEPTION DIAGNOSTICS:
            |Type: ${e.javaClass.simpleName}
            |Message: ${e.localizedMessage ?: "Unknown error"}
            |
            |DEVICE CONTEXT:
            |Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            |Model: ${Build.MANUFACTURER} ${Build.MODEL}
        """.trimMargin()
    }

    override fun isNativeAndroidAvailable(): Boolean = true
}
