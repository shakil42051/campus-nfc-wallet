package com.example.backup.gateway

import android.content.Context
import android.net.Uri

interface BackupFileGateway {
    suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray
    suspend fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray)
    fun buildDiagnosticsString(e: Exception): String
    fun isNativeAndroidAvailable(): Boolean
}
