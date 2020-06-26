package marabillas.loremar.karrypdf.utils.exts

import androidx.collection.SparseArrayCompat
import marabillas.loremar.karrypdf.utils.octalToDecimal

internal operator fun <E> SparseArrayCompat<E>.set(key: Int, value: E) {
    this.put(key, value)
}

internal fun <E> SparseArrayCompat<E>.copyOf(): SparseArrayCompat<E> {
    val copy = SparseArrayCompat<E>()
    for (i in 0 until this.size()) {
        copy[this.keyAt(i)] = this.valueAt(i)
    }
    return copy
}

internal fun <E> SparseArrayCompat<E>.octalToDecimalKeys() {
    val original = copyOf()
    clear()
    for (i in 0 until original.size()) {
        val decimalKey = octalToDecimal(original.keyAt(i))
        put(decimalKey, original.valueAt(i))
    }
}