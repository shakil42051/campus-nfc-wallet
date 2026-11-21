package com.example

import com.example.nfc.NfcUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcUtilsTest {

    @Test
    fun testBytesToHex() {
        val bytes = byteArrayOf(0x04, 0xA2.toByte(), 0x3B.toByte(), 0x8F.toByte())
        val hex = NfcUtils.bytesToHex(bytes, ":")
        assertEquals("04:A2:3B:8F", hex)
        assertEquals("04A23B8F", NfcUtils.bytesToHexNoSep(bytes))
    }

    @Test
    fun testManufacturerLookup_7ByteNxpUid() {
        val nxp7ByteUid = byteArrayOf(0x04, 0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte())
        val manufacturer = NfcUtils.getManufacturerFromUid(nxp7ByteUid)
        assertEquals("NXP Semiconductors", manufacturer)
    }

    @Test
    fun testManufacturerLookup_4ByteRandomUid() {
        val randomUid = byteArrayOf(0x08, 0x11, 0x22, 0x33)
        val manufacturer = NfcUtils.getManufacturerFromUid(randomUid)
        assertTrue(manufacturer.contains("Random UID", ignoreCase = true))
    }

    @Test
    fun testManufacturerLookup_4ByteNonManufacturerUid() {
        val uid4Byte = byteArrayOf(0x04, 0x12, 0x34, 0x56)
        val manufacturer = NfcUtils.getManufacturerFromUid(uid4Byte)
        assertTrue(manufacturer.contains("4-Byte UID", ignoreCase = true))
    }

    @Test
    fun testSakDecoder() {
        val desfireSak = NfcUtils.decodeSak(0x20)
        assertEquals("ISO/IEC 14443-4 Compliant (IsoDep / DESFire / Smart Card)", desfireSak)
        assertTrue(NfcUtils.decodeSak(0x08).contains("MIFARE Classic 1K"))
    }

    @Test
    fun testAtqaLsbFirstDecoding() {
        // Android NfcA.atqa returns bytes LSB-first: [0x04, 0x00] -> 0x0004
        assertTrue(NfcUtils.decodeAtqa(byteArrayOf(0x04, 0x00)).startsWith("0x0004"))
        assertTrue(NfcUtils.decodeAtqa(byteArrayOf(0x02, 0x00)).startsWith("0x0002"))
        assertTrue(NfcUtils.decodeAtqa(byteArrayOf(0x44, 0x00)).startsWith("0x0044"))
        assertTrue(NfcUtils.decodeAtqa(byteArrayOf(0x44, 0x03)).startsWith("0x0344"))
    }

    @Test
    fun testIsLikelyDesfire() {
        val nxp7ByteUid = byteArrayOf(0x04, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66)
        val random4ByteUid = byteArrayOf(0x08, 0x11, 0x22, 0x33)

        assertTrue(NfcUtils.isLikelyDesfire(byteArrayOf(0x44, 0x03), 0x20, nxp7ByteUid))
        assertTrue(NfcUtils.isLikelyDesfire(byteArrayOf(0x00, 0x00), 0x20, nxp7ByteUid))
        assertFalse(NfcUtils.isLikelyDesfire(byteArrayOf(0x04, 0x00), 0x08, random4ByteUid))
        assertFalse(NfcUtils.isLikelyDesfire(byteArrayOf(0x08, 0x00), 0x20, random4ByteUid))
    }
}
