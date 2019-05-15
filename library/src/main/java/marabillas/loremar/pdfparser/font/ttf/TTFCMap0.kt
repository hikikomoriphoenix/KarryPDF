package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat

internal class TTFCMap0(val data: ByteArray, val pos: Long) : TTFCMap {
    override fun getCharacterWidths(glyphWidths: IntArray): SparseArrayCompat<Float> {
        val characterWidths = SparseArrayCompat<Float>()
        val start = pos + 6
        characterWidths.put(-1, glyphWidths[0].toFloat())
        for (i in 0..255) {
            // Get byte and convert to unsigned int
            val glyphIndex = data[start.toInt() + i].toInt() and 0xff
            if (
                glyphIndex in 0..glyphWidths.lastIndex
                && glyphWidths[glyphIndex] > 0
            ) {
                characterWidths.put(i, glyphWidths[glyphIndex].toFloat())
            }
        }
        return characterWidths
    }
}