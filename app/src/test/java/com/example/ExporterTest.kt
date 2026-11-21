package com.example

import com.example.data.model.NfcScanResult
import com.example.data.model.SecurityAnalysis
import com.example.utils.Exporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExporterTest {

    @Test
    fun sanitizeFilename_stripsPathTraversalAndSpecialChars() {
        assertEquals("evil_etc_passwd", Exporter.sanitizeFilename("../../evil/etc/passwd"))
        assertEquals("campus_card", Exporter.sanitizeFilename("...///   "))
        assertEquals("My_Student_Card_1", Exporter.sanitizeFilename("My Student:Card*1?"))
    }

    @Test
    fun generateCsv_neutralizesFormulaInjectionAndEscapesQuotes() {
        val maliciousScan = NfcScanResult(
            uid = "=CMD|' /C calc'!A0",
            uidHex = "04112233",
            uidAscii = "test",
            uidBinary = "00000100",
            uidLengthBytes = 4,
            manufacturer = """NXP "Quoted", Inc.""",
            primaryTechnology = "+HYPERLINK(\"http://evil\")",
            cardStandard = "ISO/IEC 14443-A",
            technologies = listOf("NfcA"),
            atqa = "0x0004",
            sak = "0x08",
            ats = "N/A",
            historicalBytes = "N/A",
            memorySizeBytes = "1024",
            blockCount = "64",
            pageCount = "N/A",
            isNdefSupported = false,
            isNdefWritable = false,
            isNdefReadOnly = true,
            ndefMaxSizeBytes = 0,
            ndefRecords = emptyList(),
            memorySectors = emptyList(),
            securityAnalysis = SecurityAnalysis(
                encryptionType = "@SUM(A1:A2)",
                supportsCloning = false,
                cloneRiskLevel = "Low",
                explanation = "Test",
                hardwareSecurityFeatures = emptyList()
            ),
            rawDataDumpHex = "04 11 22 33"
        )

        val csv = Exporter.generateCsv(maliciousScan)
        assertTrue(csv.contains("\"'=CMD|' /C calc'!A0\""))
        assertTrue(csv.contains("\"NXP \"\"Quoted\"\", Inc.\""))
        assertTrue(csv.contains("\"'+HYPERLINK(\"\"http://evil\"\")\""))
        assertTrue(csv.contains("\"'@SUM(A1:A2)\""))
        assertFalse(csv.contains(",\"=CMD"))
    }
}
