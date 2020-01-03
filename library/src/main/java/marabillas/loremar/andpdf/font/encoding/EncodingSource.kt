package marabillas.loremar.andpdf.font.encoding

import androidx.collection.SparseArrayCompat

internal interface EncodingSource {
    fun putAllTo(target: SparseArrayCompat<String>)
}