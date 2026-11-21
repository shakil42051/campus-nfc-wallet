package com.example.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.NfcScanResult
import java.io.File
import java.util.Locale

object Exporter {

    private const val EXPORTS_DIR = "exports"
    private const val STALE_EXPORT_MAX_AGE_MS = 60 * 60 * 1000L // 1 hour

    fun sanitizeFilename(rawName: String, fallback: String = "campus_card"): String {
        val cleaned = rawName
            .replace("..", "")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_', '.')
            .take(64)
        return cleaned.ifBlank { fallback }
    }

    private fun escapeCsvValue(value: String): String {
        // Neutralize spreadsheet formula injection and escape embedded quotes/newlines per RFC 4180
        val formulaSafe = if (value.isNotEmpty() && value[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            "'$value"
        } else {
            value
        }
        val escaped = formulaSafe.replace("\"", "\"\"").replace("\r\n", " ").replace("\n", " ")
        return "\"$escaped\""
    }

    fun generateJson(scanResult: NfcScanResult): String {
        return scanResult.toJsonString()
    }

    fun generateCsv(scanResult: NfcScanResult): String {
        val rows = listOf(
            "UID" to scanResult.uid,
            "Manufacturer" to scanResult.manufacturer,
            "Primary Technology" to scanResult.primaryTechnology,
            "Card Standard" to scanResult.cardStandard,
            "ATQA" to scanResult.atqa,
            "SAK" to scanResult.sak,
            "ATS" to scanResult.ats,
            "Memory Size" to scanResult.memorySizeBytes,
            "Encryption Type" to scanResult.securityAnalysis.encryptionType,
            "Clone Feasibility" to scanResult.securityAnalysis.cloneRiskLevel
        )
        return buildString {
            append("Field,Value\n")
            rows.forEach { (field, value) ->
                append(escapeCsvValue(field))
                append(',')
                append(escapeCsvValue(value))
                append('\n')
            }
        }
    }

    fun generateTxt(scanResult: NfcScanResult): String {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append(" CAMPUS NFC WALLET - CARD TECHNICAL DUMP\n")
        sb.append("========================================\n\n")
        sb.append("UID: ${scanResult.uid}\n")
        sb.append("Hex: ${scanResult.uidHex}\n")
        sb.append("ASCII: ${scanResult.uidAscii}\n")
        sb.append("Binary: ${scanResult.uidBinary}\n")
        sb.append("Manufacturer: ${scanResult.manufacturer}\n")
        sb.append("Primary Tech: ${scanResult.primaryTechnology}\n")
        sb.append("Standard: ${scanResult.cardStandard}\n")
        sb.append("Technologies: ${scanResult.technologies.joinToString(", ")}\n")
        sb.append("ATQA: ${scanResult.atqa}\n")
        sb.append("SAK: ${scanResult.sak}\n")
        sb.append("ATS: ${scanResult.ats}\n")
        sb.append("Historical Bytes: ${scanResult.historicalBytes}\n\n")

        sb.append("--- SECURITY & CLONING ANALYSIS ---\n")
        sb.append("Encryption: ${scanResult.securityAnalysis.encryptionType}\n")
        sb.append("Risk Level: ${scanResult.securityAnalysis.cloneRiskLevel}\n")
        sb.append("Explanation: ${scanResult.securityAnalysis.explanation}\n\n")

        sb.append("--- NDEF RECORDS (${scanResult.ndefRecords.size}) ---\n")
        if (scanResult.ndefRecords.isEmpty()) {
            sb.append("No NDEF records present.\n")
        } else {
            scanResult.ndefRecords.forEachIndexed { i, record ->
                sb.append("[$i] TNF: ${record.tnf} | Type: ${record.type}\n")
                sb.append("    Payload Text: ${record.payloadText}\n")
                sb.append("    Payload Hex: ${record.payloadHex}\n")
            }
        }
        sb.append("\n--- MEMORY SECTORS & PAGES (${scanResult.memorySectors.size}) ---\n")
        scanResult.memorySectors.forEach { sector ->
            sb.append("${sector.title}: ${sector.statusMessage}\n")
            sb.append("  Data: ${sector.dataHex}\n")
        }
        sb.append("\n========================================\n")
        return sb.toString()
    }

    fun generateHexDump(scanResult: NfcScanResult): String {
        val sb = StringBuilder()
        sb.append("=== RAW HEX DUMP ===\n")
        sb.append("UID Bytes: ${scanResult.uid}\n\n")
        sb.append("Block    Hex Data                                         ASCII\n")
        sb.append("------   -----------------------------------------------  ----------------\n")

        scanResult.memorySectors.forEachIndexed { idx, sec ->
            val offsetStr = String.format(Locale.US, "%04X", idx * 16)
            val hexPadded = sec.dataHex.padEnd(47, ' ')
            val asciiPadded = sec.dataAscii.padEnd(16, ' ')
            sb.append("$offsetStr   $hexPadded  $asciiPadded  (${sec.title})\n")
        }
        return sb.toString()
    }

    fun cleanupStaleExports(context: Context) {
        val exportDir = File(context.cacheDir, EXPORTS_DIR)
        if (!exportDir.exists()) return
        val cutoff = System.currentTimeMillis() - STALE_EXPORT_MAX_AGE_MS
        exportDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    fun writeExportFile(context: Context, rawFilename: String, bytes: ByteArray): File {
        cleanupStaleExports(context)
        val exportDir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
        val safeName = sanitizeFilename(rawFilename, fallback = "export_${System.currentTimeMillis()}.dat")
        val target = File(exportDir, safeName)
        if (!target.canonicalPath.startsWith(exportDir.canonicalPath + File.separator)) {
            throw SecurityException("Invalid export path.")
        }
        target.writeBytes(bytes)
        return target
    }

    fun shareTextFile(context: Context, filename: String, content: String, mimeType: String = "text/plain") {
        try {
            val file = writeExportFile(context, filename, content.toByteArray(Charsets.UTF_8))
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Card Data"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
