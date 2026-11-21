package com.example.utils

/**
 * Pure-Kotlin ISO/IEC 15417 Code 128 (Code Set B) 1D Barcode Encoder.
 * Generates genuine, scannable monochrome module sequences with Start B (104),
 * Modulo-103 checksum, Stop (106), and standard 10-module quiet zones.
 */
object Code128Encoder {

    private const val START_CODE_B = 104
    private const val STOP_CODE = 106
    private const val QUIET_ZONE_MODULES = 10

    // Official ISO/IEC 15417 Code 128 bar/space width patterns (indices 0..106).
    // Patterns 0..105 each have 6 alternating bar/space widths summing to 11 modules.
    // Pattern 106 (Stop) has 7 widths summing to 13 modules.
    internal val PATTERNS: Array<String> = arrayOf(
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
        "114131", "311141", "411131", "211412", "211214", "211232",
        "2331112" // Stop (106)
    )

    /**
     * Encodes printable ASCII input (codes 32..126) into a boolean module array
     * where `true` represents a dark bar and `false` represents a light space.
     * Returns null if the sanitized input is empty.
     */
    fun encodeCode128B(rawInput: String): BooleanArray? {
        val sanitized = rawInput.filter { it.code in 32..126 }
        if (sanitized.isEmpty()) return null

        val symbolCodes = mutableListOf<Int>()
        symbolCodes.add(START_CODE_B)

        var checksum = START_CODE_B
        sanitized.forEachIndexed { index, ch ->
            val value = ch.code - 32
            symbolCodes.add(value)
            checksum += value * (index + 1)
        }
        symbolCodes.add(checksum % 103)
        symbolCodes.add(STOP_CODE)

        val totalModules = (QUIET_ZONE_MODULES * 2) + ((symbolCodes.size - 1) * 11) + 13
        val modules = BooleanArray(totalModules)
        var cursor = QUIET_ZONE_MODULES

        for (code in symbolCodes) {
            val pattern = PATTERNS[code]
            var isBar = true
            for (digit in pattern) {
                val width = digit - '0'
                for (i in 0 until width) {
                    modules[cursor++] = isBar
                }
                isBar = !isBar
            }
        }
        return modules
    }
}
