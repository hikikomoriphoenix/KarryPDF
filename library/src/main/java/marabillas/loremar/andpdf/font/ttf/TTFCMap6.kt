package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap6(val data: ByteArray, val pos: Long) : TTFCMapDefault() {
    init {
        val firstCode = TTFParser.getUInt16At(data, pos.toInt() + 6)
        val entryCount = TTFParser.getUInt16At(data, pos.toInt() + 8)
        val start = pos + 10
        for (i in 0 until entryCount) {
            val indexLoc = start + (2 * i)
            val glyphIndex = TTFParser.getUInt16At(data, indexLoc.toInt())
            map[firstCode + i] = glyphIndex
        }
    }
}