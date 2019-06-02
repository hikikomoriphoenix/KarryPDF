package marabillas.loremar.pdfparser.font.cmap

import marabillas.loremar.pdfparser.convertContentsToHex

internal interface CMap {
    companion object {
        val sb = StringBuilder()
    }

    fun decodeString(encoded: String): String

    fun extractActualEncoded(encodedSB: StringBuilder) {
        if (encodedSB.startsWith('(') && encodedSB.endsWith(')')) {
            encodedSB.deleteCharAt(0).deleteCharAt(encodedSB.lastIndex)
            encodedSB.convertContentsToHex()
        } else {
            encodedSB.deleteCharAt(0).deleteCharAt(encodedSB.lastIndex)
        }
    }
}