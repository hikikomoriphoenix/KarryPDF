package marabillas.loremar.andpdf.utils.exts

import androidx.collection.SparseArrayCompat
import marabillas.loremar.andpdf.utils.octalToDecimal

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
    // Get all octal keys
    val keys = IntArray(this.size())
    for (i in 0 until this.size()) {
        keys[i] = this.keyAt(i)
    }

    // For each octal key, add an entry with the value mapped to that key but with decimal result as key. Remove the entry
    // with the given octal key. In other words, replace the octal key with a decimal key.
    for (i in 0 until keys.size) {
        val result = octalToDecimal(keys[i])
        this.put(result, this[keys[i]])
        this.remove(keys[i])
    }
}