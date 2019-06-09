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

    fun encodeParentheses(decodedSB: StringBuilder) {
        var i = 0
        while (i < decodedSB.length) {
            if (decodedSB[i] == '(') {
                decodedSB.deleteCharAt(i)
                decodedSB.insert(i, "\\050")
                i += 3
            } else if (decodedSB[i] == ')') {
                decodedSB.deleteCharAt(i)
                decodedSB.insert(i, "\\051")
                i += 3
            }
            i++
        }
    }
}