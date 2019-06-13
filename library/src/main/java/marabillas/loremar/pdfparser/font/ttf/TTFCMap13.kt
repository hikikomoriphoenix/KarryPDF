package marabillas.loremar.pdfparser.font.ttf

import marabillas.loremar.pdfparser.utils.exts.set

internal class TTFCMap13(val data: ByteArray, val pos: Long) : TTFCMapDefault() {
    init {
        val nGroups = TTFParser.getUInt32At(data, pos.toInt() + 12)
        var start = pos + 16
        repeat(nGroups.toInt()) {
            val startCharCode = TTFParser.getUInt32At(data, start.toInt())
            val endCharCode = TTFParser.getUInt32At(data, start.toInt() + 4)
            val glyphIndex = TTFParser.getUInt32At(data, start.toInt() + 8)
            for (k in startCharCode..endCharCode) {
                map[k.toInt()] = glyphIndex.toInt()
            }
            start += 12
        }
    }
}