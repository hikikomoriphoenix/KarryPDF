package marabillas.loremar.karrypdf.font.ttf

import androidx.collection.SparseArrayCompat

/**
 * Represents a TrueType font CMap table as described at https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6cmap.html.
 * All formats extend this interface.
 */
internal interface TTFCMap {
    operator fun get(charCode: Int): Int?

    fun getAll(target: SparseArrayCompat<Int>)
}