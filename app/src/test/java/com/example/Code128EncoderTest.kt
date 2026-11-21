package com.example

import com.example.utils.Code128Encoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Code128EncoderTest {

    @Test
    fun allPatternsHaveValidModuleSums() {
        assertEquals(107, Code128Encoder.PATTERNS.size)
        for (i in 0 until 106) {
            val pattern = Code128Encoder.PATTERNS[i]
            assertEquals("Pattern $i must have 6 elements", 6, pattern.length)
            val sum = pattern.sumOf { it - '0' }
            assertEquals("Pattern $i must sum to 11 modules", 11, sum)
        }
        val stop = Code128Encoder.PATTERNS[106]
        assertEquals(7, stop.length)
        assertEquals(13, stop.sumOf { it - '0' })
    }

    @Test
    fun encodeCode128B_emptyAndNonAsciiReturnNull() {
        assertNull(Code128Encoder.encodeCode128B(""))
        assertNull(Code128Encoder.encodeCode128B("\n\t\u0007"))
    }

    @Test
    fun encodeCode128B_validInputHasQuietZonesStartAndStop() {
        val input = "2026001001"
        val modules = Code128Encoder.encodeCode128B(input)
        assertNotNull(modules)
        val arr = modules!!

        // Total modules = 10 (quiet) + (1 Start + N data + 1 checksum) * 11 + 13 (Stop) + 10 (quiet)
        val expectedLength = 20 + (input.length + 2) * 11 + 13
        assertEquals(expectedLength, arr.size)

        // Leading 10-module quiet zone (all spaces)
        for (i in 0 until 10) {
            assertFalse("Leading quiet zone index $i should be false", arr[i])
        }
        // Trailing 10-module quiet zone (all spaces)
        for (i in (arr.size - 10) until arr.size) {
            assertFalse("Trailing quiet zone index $i should be false", arr[i])
        }

        // First bar after leading quiet zone must be dark (Start B begins with 2 dark modules)
        assertTrue(arr[10])
        assertTrue(arr[11])
        assertFalse(arr[12])

        // Stop pattern ends with 2 dark modules right before trailing quiet zone
        assertTrue(arr[arr.size - 11])
        assertTrue(arr[arr.size - 12])
        assertFalse(arr[arr.size - 13])
    }
}
