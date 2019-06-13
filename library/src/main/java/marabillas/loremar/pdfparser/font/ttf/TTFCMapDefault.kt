package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat

abstract class TTFCMapDefault : TTFCMap {
    protected val map = SparseArrayCompat<Int>()

    override fun get(charCode: Int): Int? {
        return map[charCode]
    }

    override fun getAll(target: SparseArrayCompat<Int>) {
        target.putAll(map)
    }
}