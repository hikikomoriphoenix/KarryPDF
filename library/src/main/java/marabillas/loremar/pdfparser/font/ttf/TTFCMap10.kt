package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat

internal class TTFCMap10(val data: ByteArray, val pos: Long) : TTFCMap {
    override fun getCharacterWidths(glyphWidths: IntArray): SparseArrayCompat<Float> {
        val characterWidths = SparseArrayCompat<Float>()
        characterWidths.put(-1, glyphWidths[0].toFloat())

        val startCharCode = TTFParser.getUInt32At(data, pos.toInt() + 12)
        val numChars = TTFParser.getUInt32At(data, pos.toInt() + 16)

        val start = pos + 20
        for (i in 0 until numChars.toInt()) {
            val indexLoc = start + (2 * i)
            val glyphIndex = TTFParser.getUInt16At(data, indexLoc.toInt())
            if (
                glyphIndex in 0..glyphWidths.lastIndex
                && glyphWidths[glyphIndex] > 0
            ) {
                characterWidths.put(startCharCode.toInt() + i, glyphWidths[glyphIndex].toFloat())
            }
        }

        return characterWidths
    }
}