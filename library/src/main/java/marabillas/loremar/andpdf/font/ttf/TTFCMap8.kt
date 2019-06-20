package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap8(data: ByteArray, pos: Long) : TTFCMapDefault(data, pos) {
    init {
        length = TTFParser.getUInt32At(data, pos.toInt() + 4).toInt()
        // Skip also the is32[65536] array.
        var start = pos + 12 + 65536
        checkIfLocationIsWithinTableLength(start.toInt())
        // Get number of code ranges.
        val nGroups = TTFParser.getUInt32At(data, start.toInt())
        start += 4
        repeat(nGroups.toInt()) {
            checkIfLocationIsWithinTableLength(start.toInt() + 8)
            val startCharCode = TTFParser.getUInt32At(data, start.toInt())
            val endCharCode = TTFParser.getUInt32At(data, start.toInt() + 4)
            val startGlyphCode = TTFParser.getUInt32At(data, start.toInt() + 8)
            for (k in startCharCode..endCharCode) {
                val offset = k - startCharCode
                val glyphIndex = startGlyphCode + offset
                map[k.toInt()] = glyphIndex.toInt()
            }
            start += 12
        }

    }
}