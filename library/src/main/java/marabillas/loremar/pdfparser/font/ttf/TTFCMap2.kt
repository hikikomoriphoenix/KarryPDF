package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.exceptions.UnsupportedPDFElementException

internal class TTFCMap2(data: ByteArray, pos: Long) : TTFCMap {
    override fun getCharacterWidths(glyphWidths: IntArray): SparseArrayCompat<Float> {
        throw UnsupportedPDFElementException("Japanese, Chinese, Korean TrueType Fonts are not yet supported.")
    }
}