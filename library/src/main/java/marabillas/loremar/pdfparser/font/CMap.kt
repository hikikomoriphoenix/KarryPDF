package marabillas.loremar.pdfparser.font

import marabillas.loremar.pdfparser.convertContentsToHex
import marabillas.loremar.pdfparser.hexToInt

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

    fun isWithinRange(codeSpaceRange: Array<String>, codeSB: StringBuilder): Boolean {
        val l = codeSpaceRange[0].length
        if (codeSB.length != l) return false
        for (i in 0 until l step 2) {
            val b1 = sb.clear().append(codeSpaceRange[0][i]).append(codeSpaceRange[0][i + 1]).hexToInt()
            val b2 = sb.clear().append(codeSpaceRange[1][i]).append(codeSpaceRange[1][i + 1]).hexToInt()
            val b3 = sb.clear().append(codeSB[i]).append(codeSB[i + 1]).hexToInt()
            if (b3 !in b1..b2) return false
        }
        return true
    }
}