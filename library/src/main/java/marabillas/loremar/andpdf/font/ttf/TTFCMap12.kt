package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap12(val data: ByteArray, val pos: Long) : TTFCMapDefault() {
    init {
        val nGroups = TTFParser.getUInt32At(data, pos.toInt() + 12)
        var start = pos + 16
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