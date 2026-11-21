package com.example.nfc

import java.util.Locale

object NfcUtils {

    fun bytesToHex(bytes: ByteArray?, separator: String = ":"): String {
        if (bytes == null || bytes.isEmpty()) return "N/A"
        return bytes.joinToString(separator) { String.format(Locale.US, "%02X", it) }
    }

    fun bytesToHexNoSep(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return ""
        return bytes.joinToString("") { String.format(Locale.US, "%02X", it) }
    }

    fun bytesToAscii(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return "N/A"
        return bytes.map { b ->
            val c = b.toInt() and 0xFF
            if (c in 32..126) c.toChar() else '.'
        }.joinToString("")
    }

    fun bytesToBinary(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return "N/A"
        return bytes.joinToString(" ") { b ->
            String.format(Locale.US, "%8s", Integer.toBinaryString(b.toInt() and 0xFF)).replace(' ', '0')
        }
    }

    /**
     * Resolves RFID / NFC silicon manufacturer based on ISO/IEC 14443-3 & ISO/IEC 7816-6.
     * Per ISO/IEC 14443-3:
     *  - 4-byte UID starting with 0x08 indicates a Random UID (RID), NOT a manufacturer prefix.
     *  - 4-byte non-unique UIDs do not carry an ISO 7816-6 manufacturer byte in byte 0.
     *  - 7-byte and 10-byte UIDs carry the IC manufacturer code in byte 0.
     */
    fun getManufacturerFromUid(uid: ByteArray?): String {
        if (uid == null || uid.isEmpty()) return "Unknown Manufacturer"
        val firstByte = uid[0].toInt() and 0xFF

        if (uid.size == 4) {
            return when (firstByte) {
                0x08 -> "Random UID (ISO/IEC 14443-3 RID)"
                else -> "ISO 14443-3A 4-Byte UID (Non-Manufacturer Prefix)"
            }
        }

        return when (firstByte) {
            0x01 -> "Motorola / Sony FeliCa"
            0x02 -> "STMicroelectronics"
            0x03 -> "Hitachi Ltd."
            0x04 -> "NXP Semiconductors"
            0x05 -> "Infineon Technologies AG"
            0x06 -> "Cylink"
            0x07 -> "Texas Instruments"
            0x08 -> "Fujitsu Limited"
            0x09 -> "Matsushita Electronics"
            0x0A -> "NEC"
            0x0B -> "Oki Electric Industry"
            0x0C -> "Toshiba"
            0x0D -> "Mitsubishi Electric"
            0x0E -> "Samsung Electronics"
            0x0F -> "Hynix Semiconductor"
            0x10 -> "LG Semiconductors"
            0x11 -> "Emosyn"
            0x12 -> "INSIDE Secure / Microchip"
            0x13 -> "ORGA Kartensysteme"
            0x14 -> "SHARP"
            0x15 -> "ATMEL"
            0x16 -> "EM Microelectronic"
            0x17 -> "SMARTRAC"
            0x18 -> "ZMD"
            0x19 -> "Xicor"
            0x1A -> "Sony Corporation"
            0x1B -> "Malaysia Microelectronic"
            0x1C -> "Infineon Technologies"
            0x1D -> "Broadcom"
            0x1E -> "Printec Microelectronics"
            0x1F -> "Atmel"
            0x25 -> "Samsung Semiconductor"
            0x33 -> "LEGIC Identsystems AG"
            0x80 -> "HID Global / ASSA ABLOY"
            else -> "Standard ISO 14443 Tag (IC 0x${String.format(Locale.US, "%02X", firstByte)})"
        }
    }

    /**
     * Decode SAK (Select Acknowledge) byte per NXP AN10833.
     */
    fun decodeSak(sakByte: Int): String {
        return when (sakByte) {
            0x00 -> "MIFARE Ultralight / NTAG (ISO 14443-3A)"
            0x08 -> "MIFARE Classic 1K (ISO 14443-3A)"
            0x09 -> "MIFARE Mini 0.3K (ISO 14443-3A)"
            0x18 -> "MIFARE Classic 4K (ISO 14443-3A)"
            0x20 -> "ISO/IEC 14443-4 Compliant (IsoDep / DESFire / Smart Card)"
            0x28 -> "MIFARE Plus / JCOP with Classic 1K emulation (ISO 14443-4)"
            0x38 -> "MIFARE SmartMX with Classic 4K emulation (ISO 14443-4)"
            0x88 -> "Infineon MIFARE Classic 1K"
            0x98 -> "Gemplus MPCOS"
            else -> "SAK 0x${String.format(Locale.US, "%02X", sakByte)} (ISO 14443-3A)"
        }
    }

    /**
     * Decode ATQA (Answer to Request, Type A) per ISO/IEC 14443-3 and NXP AN10833.
     * Note: Android's NfcA.atqa returns bytes in LSB-first order (e.g. [0x04, 0x00] = 0x0004).
     */
    fun decodeAtqa(atqaBytes: ByteArray?): String {
        if (atqaBytes == null || atqaBytes.size < 2) return "N/A"
        // Convert Android LSB-first 2-byte ATQA into canonical 16-bit hex representation
        val canonicalInt = ((atqaBytes[1].toInt() and 0xFF) shl 8) or (atqaBytes[0].toInt() and 0xFF)
        val canonicalHex = String.format(Locale.US, "%04X", canonicalInt)
        return when (canonicalHex) {
            "0004" -> "0x0004 (MIFARE Classic 1K / 4-Byte UID)"
            "0002" -> "0x0002 (MIFARE Classic 4K / 4-Byte UID)"
            "0044" -> "0x0044 (MIFARE Ultralight / NTAG / 7-Byte UID)"
            "0344" -> "0x0344 (MIFARE DESFire / 7-Byte UID)"
            "0042" -> "0x0042 (MIFARE Classic 4K / 7-Byte UID)"
            "0008" -> "0x0008 (ISO 14443-4 Smart Card / 4-Byte UID)"
            "0304" -> "0x0304 (MIFARE Plus / 4-Byte UID)"
            else -> "0x$canonicalHex"
        }
    }

    fun isLikelyDesfire(atqaBytes: ByteArray?, sak: Int, uid: ByteArray?): Boolean {
        val canonicalAtqa = if (atqaBytes != null && atqaBytes.size >= 2) {
            ((atqaBytes[1].toInt() and 0xFF) shl 8) or (atqaBytes[0].toInt() and 0xFF)
        } else -1
        val isNxp7Byte = uid != null && uid.size == 7 && (uid[0].toInt() and 0xFF) == 0x04
        return canonicalAtqa == 0x0344 || (sak == 0x20 && isNxp7Byte)
    }

    fun determineIsoStandard(techList: List<String>, sak: Int): String {
        return when {
            techList.contains("android.nfc.tech.IsoDep") -> "ISO/IEC 14443-4 (IsoDep / Contactless Smart Card)"
            techList.contains("android.nfc.tech.MifareClassic") -> "ISO/IEC 14443-3A (MIFARE Classic)"
            techList.contains("android.nfc.tech.MifareUltralight") -> "ISO/IEC 14443-3A (MIFARE Ultralight / NTAG)"
            techList.contains("android.nfc.tech.NfcV") -> "ISO/IEC 15693 (Vicinity Card)"
            techList.contains("android.nfc.tech.NfcF") -> "JIS X 6319-4 / FeliCa"
            techList.contains("android.nfc.tech.NfcB") -> "ISO/IEC 14443-3B"
            techList.contains("android.nfc.tech.NfcA") -> "ISO/IEC 14443-3A"
            else -> "Generic ISO/IEC Contactless Standard"
        }
    }
}
