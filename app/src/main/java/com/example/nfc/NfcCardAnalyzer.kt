package com.example.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import com.example.data.model.NdefRecordInfo
import com.example.data.model.NfcScanResult
import com.example.data.model.SectorMemoryInfo
import com.example.data.model.SecurityAnalysis
import java.nio.charset.Charset
import java.util.Locale

object NfcCardAnalyzer {

    fun analyzeTag(tag: Tag): NfcScanResult {
        val rawUid = tag.id ?: byteArrayOf()
        val uidFormatted = NfcUtils.bytesToHex(rawUid, ":")
        val uidHexNoSep = NfcUtils.bytesToHexNoSep(rawUid)
        val uidAscii = NfcUtils.bytesToAscii(rawUid)
        val uidBinary = NfcUtils.bytesToBinary(rawUid)
        val uidLength = rawUid.size

        val techListRaw = tag.techList?.toList() ?: emptyList()
        val techListFormatted = techListRaw.map { it.substringAfterLast(".") }

        // 1. Manufacturer Lookup
        val manufacturer = NfcUtils.getManufacturerFromUid(rawUid)

        // 2. Extract NfcA hardware parameters (SAK, ATQA)
        var sakHex = "N/A"
        var sakInt = -1
        var atqaHex = "N/A"
        var rawAtqa: ByteArray? = null

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                sakInt = nfcA.sak.toInt() and 0xFF
                sakHex = String.format(Locale.US, "0x%02X (%s)", sakInt, NfcUtils.decodeSak(sakInt))
                rawAtqa = nfcA.atqa
                atqaHex = NfcUtils.decodeAtqa(rawAtqa)
            } catch (e: Exception) {
                sakHex = "Error reading SAK"
            }
        }

        // 3. Extract IsoDep parameters (ATS / Historical Bytes / HiLayer)
        var atsHex = "N/A"
        var historicalBytesHex = "N/A"
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                val hist = isoDep.historicalBytes
                if (hist != null && hist.isNotEmpty()) {
                    historicalBytesHex = NfcUtils.bytesToHex(hist, ":")
                }
                val hiLayer = isoDep.hiLayerResponse
                if (hiLayer != null && hiLayer.isNotEmpty()) {
                    atsHex = NfcUtils.bytesToHex(hiLayer, ":")
                } else if (hist != null && hist.isNotEmpty()) {
                    atsHex = historicalBytesHex
                }
            } catch (e: Exception) {
                atsHex = "Protected / Not exposed"
            }
        }

        // 4. ISO Standard Identification
        val cardStandard = NfcUtils.determineIsoStandard(techListRaw, sakInt)

        // 5. NDEF Inspection (always closed in finally)
        var isNdefSupported = false
        var isNdefWritable = false
        var isNdefReadOnly = false
        var ndefMaxSizeBytes = 0
        val ndefRecordsList = mutableListOf<NdefRecordInfo>()

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            isNdefSupported = true
            try {
                ndef.connect()
                isNdefWritable = ndef.isWritable
                ndefMaxSizeBytes = ndef.maxSize
                val ndefMessage: NdefMessage? = ndef.cachedNdefMessage ?: ndef.ndefMessage
                isNdefReadOnly = !ndef.isWritable

                if (ndefMessage != null) {
                    for (record in ndefMessage.records) {
                        ndefRecordsList.add(parseNdefRecord(record))
                    }
                }
            } catch (e: Exception) {
                if (ndefRecordsList.isEmpty()) {
                    ndefRecordsList.add(
                        NdefRecordInfo(
                            tnf = "NDEF Read Error",
                            type = "Protected NDEF",
                            payloadHex = "N/A",
                            payloadText = "NDEF record present but unreadable or access protected."
                        )
                    )
                }
            } finally {
                runCatching { if (ndef.isConnected) ndef.close() }
            }
        }

        // 6. Memory & Sector Inspection (always closed in finally)
        val memorySectors = mutableListOf<SectorMemoryInfo>()
        var memorySizeBytes = "Unknown"
        var blockCountStr = "N/A"
        var pageCountStr = "N/A"

        val mifareClassic = MifareClassic.get(tag)
        val mifareUltralight = MifareUltralight.get(tag)

        if (mifareClassic != null) {
            try {
                mifareClassic.connect()
                val sectorCount = mifareClassic.sectorCount
                val totalBlocks = mifareClassic.blockCount
                val sizeBytes = mifareClassic.size
                memorySizeBytes = "$sizeBytes Bytes (${sizeBytes / 1024} KB)"
                blockCountStr = "$totalBlocks blocks ($sectorCount sectors)"
                pageCountStr = "N/A (Sector based)"

                val defaultKeys = listOf(
                    MifareClassic.KEY_DEFAULT,
                    MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
                    MifareClassic.KEY_NFC_FORUM,
                    byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                )

                for (s in 0 until sectorCount) {
                    var authenticated = false
                    for (key in defaultKeys) {
                        if (mifareClassic.authenticateSectorWithKeyA(s, key) ||
                            mifareClassic.authenticateSectorWithKeyB(s, key)
                        ) {
                            authenticated = true
                            break
                        }
                    }

                    if (authenticated) {
                        val firstBlock = mifareClassic.sectorToBlock(s)
                        val blocksInSector = mifareClassic.getBlockCountInSector(s)
                        val dataBlocksCount = (blocksInSector - 1).coerceAtLeast(1) // Exclude sector trailer block
                        val hexLines = mutableListOf<String>()
                        val asciiLines = mutableListOf<String>()

                        for (bOffset in 0 until dataBlocksCount) {
                            val blockIndex = firstBlock + bOffset
                            val blockBytes = runCatching { mifareClassic.readBlock(blockIndex) }.getOrNull()
                            if (blockBytes != null) {
                                hexLines.add(NfcUtils.bytesToHex(blockBytes, " "))
                                asciiLines.add(NfcUtils.bytesToAscii(blockBytes))
                            }
                        }

                        if (hexLines.isNotEmpty()) {
                            memorySectors.add(
                                SectorMemoryInfo(
                                    index = s,
                                    title = "Sector $s (Blocks $firstBlock–${firstBlock + dataBlocksCount - 1})",
                                    isProtected = false,
                                    dataHex = hexLines.joinToString("\n"),
                                    dataAscii = asciiLines.joinToString("\n"),
                                    statusMessage = "Readable with standard public keys"
                                )
                            )
                        } else {
                            memorySectors.add(
                                SectorMemoryInfo(
                                    index = s,
                                    title = "Sector $s",
                                    isProtected = true,
                                    dataHex = "PROTECTED",
                                    dataAscii = "PROTECTED",
                                    statusMessage = "This memory block is protected."
                                )
                            )
                        }
                    } else {
                        memorySectors.add(
                            SectorMemoryInfo(
                                index = s,
                                title = "Sector $s",
                                isProtected = true,
                                dataHex = "PROTECTED",
                                dataAscii = "PROTECTED",
                                statusMessage = "This memory block is protected."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                if (memorySectors.isEmpty()) {
                    memorySectors.add(
                        SectorMemoryInfo(
                            index = 0,
                            title = "MIFARE Sectors",
                            isProtected = true,
                            dataHex = "PROTECTED",
                            dataAscii = "PROTECTED",
                            statusMessage = "This memory block is protected."
                        )
                    )
                }
            } finally {
                runCatching { if (mifareClassic.isConnected) mifareClassic.close() }
            }
        } else if (mifareUltralight != null) {
            try {
                mifareUltralight.connect()
                val (typeStr, maxPageBatches) = when (mifareUltralight.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight (64 Bytes)" to 4
                    MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C (192 Bytes)" to 12
                    else -> "MIFARE Ultralight / NTAG Variant" to 12
                }
                memorySizeBytes = typeStr
                blockCountStr = "N/A"

                var readablePages = 0
                for (batch in 0 until maxPageBatches) {
                    val startPage = batch * 4
                    try {
                        val pageData = mifareUltralight.readPages(startPage)
                        readablePages += 4
                        memorySectors.add(
                            SectorMemoryInfo(
                                index = batch,
                                title = "Pages $startPage–${startPage + 3}",
                                isProtected = false,
                                dataHex = NfcUtils.bytesToHex(pageData, " "),
                                dataAscii = NfcUtils.bytesToAscii(pageData),
                                statusMessage = if (batch == 0) "Header / UID / Lock Bytes" else "Readable User Memory"
                            )
                        )
                    } catch (e: Exception) {
                        if (batch < 4) {
                            memorySectors.add(
                                SectorMemoryInfo(
                                    index = batch,
                                    title = "Pages $startPage–${startPage + 3}",
                                    isProtected = true,
                                    dataHex = "PROTECTED",
                                    dataAscii = "PROTECTED",
                                    statusMessage = "This memory block is protected."
                                )
                            )
                        } else {
                            // Reached end of tag memory on smaller tags
                            break
                        }
                    }
                }
                pageCountStr = "$readablePages Readable Pages (4 Bytes/Page)"
            } catch (e: Exception) {
                memorySectors.add(
                    SectorMemoryInfo(
                        index = 0,
                        title = "Pages 0–3",
                        isProtected = true,
                        dataHex = "PROTECTED",
                        dataAscii = "PROTECTED",
                        statusMessage = "This memory block is protected."
                    )
                )
            } finally {
                runCatching { if (mifareUltralight.isConnected) mifareUltralight.close() }
            }
        } else if (isoDep != null) {
            memorySizeBytes = "ISO 7816 Smart Card Microcontroller"
            memorySectors.add(
                SectorMemoryInfo(
                    index = 0,
                    title = "ISO-DEP Application Memory",
                    isProtected = true,
                    dataHex = "HARDWARE ENCRYPTED",
                    dataAscii = "HARDWARE ENCRYPTED",
                    statusMessage = "This memory block is protected."
                )
            )
        } else {
            memorySectors.add(
                SectorMemoryInfo(
                    index = 0,
                    title = "Tag Memory",
                    isProtected = true,
                    dataHex = "N/A",
                    dataAscii = "N/A",
                    statusMessage = "This memory block is protected."
                )
            )
        }

        val isDesfire = NfcUtils.isLikelyDesfire(rawAtqa, sakInt, rawUid)

        // 7. Security & Cloning Feasibility Analysis
        val securityAnalysis = generateSecurityAnalysis(techListFormatted, isDesfire)

        // 8. Primary Technology Name
        val primaryTech = when {
            techListFormatted.contains("IsoDep") && isDesfire -> "MIFARE DESFire Smart Card (ISO 14443-4)"
            techListFormatted.contains("IsoDep") -> "ISO/IEC 14443-4 Smart Card (IsoDep)"
            techListFormatted.contains("MifareClassic") -> "MIFARE Classic RFID"
            techListFormatted.contains("MifareUltralight") -> "MIFARE Ultralight / NTAG"
            techListFormatted.contains("NfcV") -> "ISO 15693 Vicinity Tag"
            techListFormatted.contains("NfcF") -> "FeliCa Smart Card"
            techListFormatted.contains("Ndef") -> "NFC Forum NDEF Tag"
            else -> techListFormatted.firstOrNull() ?: "Standard Contactless Tag"
        }

        // Build complete raw hex dump from UID + readable sector/page payloads
        val dumpParts = mutableListOf(NfcUtils.bytesToHex(rawUid, " "))
        memorySectors.filter { !it.isProtected && it.dataHex.isNotBlank() }.forEach {
            dumpParts.add(it.dataHex.replace("\n", " "))
        }
        val rawDataDumpHex = dumpParts.joinToString(" | ")

        return NfcScanResult(
            uid = uidFormatted,
            uidHex = uidHexNoSep,
            uidAscii = uidAscii,
            uidBinary = uidBinary,
            uidLengthBytes = uidLength,
            manufacturer = manufacturer,
            primaryTechnology = primaryTech,
            cardStandard = cardStandard,
            technologies = techListFormatted,
            atqa = atqaHex,
            sak = sakHex,
            ats = atsHex,
            historicalBytes = historicalBytesHex,
            memorySizeBytes = memorySizeBytes,
            blockCount = blockCountStr,
            pageCount = pageCountStr,
            isNdefSupported = isNdefSupported,
            isNdefWritable = isNdefWritable,
            isNdefReadOnly = isNdefReadOnly,
            ndefMaxSizeBytes = ndefMaxSizeBytes,
            ndefRecords = ndefRecordsList,
            memorySectors = memorySectors,
            securityAnalysis = securityAnalysis,
            rawDataDumpHex = rawDataDumpHex,
            timestamp = System.currentTimeMillis()
        )
    }

    internal fun parseNdefRecord(record: NdefRecord): NdefRecordInfo {
        val tnfStr = when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> "Well Known (RTD)"
            NdefRecord.TNF_MIME_MEDIA -> "MIME Media"
            NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
            NdefRecord.TNF_EXTERNAL_TYPE -> "External (Custom)"
            NdefRecord.TNF_EMPTY -> "Empty"
            NdefRecord.TNF_UNCHANGED -> "Unchanged"
            else -> "Unknown TNF"
        }

        val typeStr = String(record.type ?: byteArrayOf(), Charset.forName("UTF-8"))
        val payloadBytes = record.payload ?: byteArrayOf()
        val payloadHex = NfcUtils.bytesToHex(payloadBytes, " ")

        var payloadText = "Binary Payload (${payloadBytes.size} bytes)"

        try {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && payloadBytes.isNotEmpty()) {
                if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    val status = payloadBytes[0].toInt() and 0xFF
                    val isUtf16 = (status and 0x80) != 0
                    val langLength = status and 0x3F
                    val textOffset = 1 + langLength
                    if (textOffset <= payloadBytes.size) {
                        val charset = if (isUtf16) Charset.forName("UTF-16") else Charset.forName("UTF-8")
                        payloadText = String(payloadBytes, textOffset, payloadBytes.size - textOffset, charset)
                    }
                } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    val prefixByte = payloadBytes[0].toInt() and 0xFF
                    val uriPrefixes = arrayOf(
                        "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
                        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://", "ftp://",
                        "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:", "sips:", "tftp:",
                        "btspp://", "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
                        "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
                    )
                    val prefix = if (prefixByte in uriPrefixes.indices) uriPrefixes[prefixByte] else ""
                    val uriBody = if (payloadBytes.size > 1) {
                        String(payloadBytes, 1, payloadBytes.size - 1, Charset.forName("UTF-8"))
                    } else {
                        ""
                    }
                    payloadText = "$prefix$uriBody"
                }
            } else if ((record.tnf == NdefRecord.TNF_MIME_MEDIA || record.tnf == NdefRecord.TNF_ABSOLUTE_URI) && payloadBytes.isNotEmpty()) {
                payloadText = String(payloadBytes, Charset.forName("UTF-8"))
            }
        } catch (e: Exception) {
            payloadText = NfcUtils.bytesToAscii(payloadBytes)
        }

        return NdefRecordInfo(
            tnf = tnfStr,
            type = typeStr,
            payloadHex = payloadHex,
            payloadText = payloadText
        )
    }

    private fun generateSecurityAnalysis(
        technologies: List<String>,
        isDesfire: Boolean
    ): SecurityAnalysis {
        val features = mutableListOf<String>()
        val encType: String
        val supportsCloning = false
        val riskLevel: String
        val explanation: String

        if (technologies.contains("IsoDep") && isDesfire) {
            encType = "AES-128 / 3DES Hardware Security (MIFARE DESFire)"
            riskLevel = "None (Cryptographically Protected)"
            features.add("Hardware AES-128 / 3DES Coprocessor")
            features.add("Mutual Challenge-Response Authentication")
            features.add("Session Key Derivation")
            features.add("Anti-Tearing / Transaction MAC Protection")
            explanation = "Detected an NXP ISO/IEC 14443-4 DESFire-class smart card. While the public UID is readable during RF anti-collision, application files and access credentials are protected by hardware mutual authentication."
        } else if (technologies.contains("IsoDep")) {
            encType = "ISO/IEC 14443-4 APDU Smart Card (IsoDep)"
            riskLevel = "Low to None (Application-Dependent)"
            features.add("ISO 7816-4 APDU Command Interface")
            features.add("Secure Microcontroller Architecture")
            explanation = "This card communicates via ISO/IEC 14443-4 (IsoDep). Protected applets require authenticated APDU sessions and cannot be cloned by reading the public UID."
        } else if (technologies.contains("MifareClassic")) {
            encType = "MIFARE Crypto-1 Stream Cipher (48-bit Key)"
            riskLevel = "High (Legacy 48-bit Cipher)"
            features.add("48-bit Sector Key A & Key B")
            features.add("Per-Sector Access Condition Bits")
            explanation = "MIFARE Classic uses the legacy 48-bit Crypto-1 cipher, which has well-known cryptographic weaknesses if audited with specialized hardware. This app only performs non-intrusive reads using public default keys and marks protected sectors as locked."
        } else if (technologies.contains("MifareUltralight")) {
            encType = "OTP Lock Bits / Optional PWD or 3DES Auth"
            riskLevel = "Medium (UID-Only Systems Vulnerable)"
            features.add("One-Time Programmable (OTP) Lock Bytes")
            features.add("Read-Only Page Locks")
            explanation = "MIFARE Ultralight / NTAG tag. Public pages and UID are readable without authentication unless password protection or Ultralight C 3DES authentication is enabled."
        } else {
            encType = "Standard ISO Contactless Interface"
            riskLevel = "Low (Public Metadata Only)"
            features.add("Public Broadcast UID")
            explanation = "Standard contactless tag. Android accesses only public broadcast metadata; secure campus systems validate cryptographic credentials rather than UID alone."
        }

        return SecurityAnalysis(
            encryptionType = encType,
            supportsCloning = supportsCloning,
            cloneRiskLevel = riskLevel,
            explanation = explanation,
            hardwareSecurityFeatures = features
        )
    }
}
