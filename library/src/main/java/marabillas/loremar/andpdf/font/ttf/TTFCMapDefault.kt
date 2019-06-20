package marabillas.loremar.andpdf.font.ttf

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.andpdf.exceptions.font.InvalidTTFCMapException

abstract class TTFCMapDefault(val data: ByteArray, val pos: Long) : TTFCMap {
    protected val map = SparseArrayCompat<Int>()
    protected var length = TTFParser.getUInt16At(data, pos.toInt() + 2)

    override fun get(charCode: Int): Int? {
        return map[charCode]
    }

    override fun getAll(target: SparseArrayCompat<Int>) {
        target.putAll(map)
    }

    protected fun checkIfLocationIsWithinTableLength(position: Int) {
        if ((position - pos) >= length)
            throw InvalidTTFCMapException("Acquired location of byte is outside the cmap table")
    }
}