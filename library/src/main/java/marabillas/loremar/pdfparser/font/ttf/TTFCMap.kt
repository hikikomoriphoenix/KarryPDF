package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat

/**
 * Represents a TrueType font CMap table as described at https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6cmap.html.
 * All formats extend this interface.
 */
interface TTFCMap {
    fun getCharacterWidths(glyphWidths: IntArray): SparseArrayCompat<Float>
}