package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap8(val data: ByteArray, val pos: Long) : TTFCMapDefault() {
    init {
        // Skip also the is32[65536] array.
        var start = pos + 12 + 65536
        // Get number of code ranges.
        val nGroups = TTFParser.getUInt32At(data, start.toInt())
        start += 4
        repeat(nGroups.toInt()) {
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