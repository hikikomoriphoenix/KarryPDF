package marabillas.loremar.karrypdf.font.encoding

import androidx.collection.SparseArrayCompat

internal interface EncodingSource {
    fun putAllTo(target: SparseArrayCompat<String>)
}