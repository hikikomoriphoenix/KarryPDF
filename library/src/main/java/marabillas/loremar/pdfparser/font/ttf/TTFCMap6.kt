package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat

internal class TTFCMap6(val data: ByteArray, val pos: Long) : TTFCMap {
    override fun getCharacterWidths(glyphWidths: IntArray): SparseArrayCompat<Float> {
        val characterWidths = SparseArrayCompat<Float>()
        // Put missing character width to array with -1 as key.
        characterWidths.put(-1, glyphWidths[0].toFloat())

        val firstCode = TTFParser.getUInt16At(data, pos.toInt() + 6)
        val entryCount = TTFParser.getUInt16At(data, pos.toInt() + 8)
        val start = pos + 10
        for (i in 0 until entryCount) {
            val indexLoc = start + (2 * i)
            val glyphIndex = TTFParser.getUInt16At(data, indexLoc.toInt())
            if (
                glyphIndex in 0..glyphWidths.lastIndex
                && glyphWidths[glyphIndex] > 0
            ) {
                characterWidths.put(firstCode + i, glyphWidths[glyphIndex].toFloat())
            }
        }

        return characterWidths
    }
}