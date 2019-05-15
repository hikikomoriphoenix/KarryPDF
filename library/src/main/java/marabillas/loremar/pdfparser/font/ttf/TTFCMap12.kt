package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat

internal class TTFCMap12(val data: ByteArray, val pos: Long) : TTFCMap {
    override fun getCharacterWidths(glyphWidths: IntArray): SparseArrayCompat<Float> {
        val characterWidths = SparseArrayCompat<Float>()
        characterWidths.put(-1, glyphWidths[0].toFloat())

        val nGroups = TTFParser.getUInt32At(data, pos.toInt() + 12)
        var start = pos + 16
        repeat(nGroups.toInt()) {
            val startCharCode = TTFParser.getUInt32At(data, start.toInt())
            val endCharCode = TTFParser.getUInt32At(data, start.toInt() + 4)
            val startGlyphCode = TTFParser.getUInt32At(data, start.toInt() + 8)
            for (k in startCharCode..endCharCode) {
                val offset = k - startCharCode
                val glyphIndex = startGlyphCode + offset
                if (
                    glyphIndex in 0..glyphWidths.lastIndex
                    && glyphWidths[glyphIndex.toInt()] > 0
                ) {
                    characterWidths.put(k.toInt(), glyphWidths[glyphIndex.toInt()].toFloat())
                }
            }
            start += 12
        }

        return characterWidths
    }
}