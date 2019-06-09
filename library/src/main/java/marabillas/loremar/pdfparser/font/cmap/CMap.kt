package marabillas.loremar.pdfparser.font.cmap

import marabillas.loremar.pdfparser.utils.exts.convertContentsToHex
import marabillas.loremar.pdfparser.utils.exts.resolveEscapedSequences

internal interface CMap {
    companion object {
        const val MISSING_CHAR = '□'
    }

    fun decodeString(encoded: String): String

    fun extractActualEncoded(encodedSB: StringBuilder) {
        if (encodedSB.startsWith('(') && encodedSB.endsWith(')')) {
            encodedSB.deleteCharAt(0).deleteCharAt(encodedSB.lastIndex)
            encodedSB.resolveEscapedSequences()
            encodedSB.convertContentsToHex()
        } else {
            encodedSB.deleteCharAt(0).deleteCharAt(encodedSB.lastIndex)
        }
    }
}