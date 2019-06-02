package marabillas.loremar.pdfparser.font.encoding

import android.support.v4.util.SparseArrayCompat

internal interface EncodingSource {
    fun putAllTo(target: SparseArrayCompat<String>)
}