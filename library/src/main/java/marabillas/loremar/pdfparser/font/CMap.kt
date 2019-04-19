package marabillas.loremar.pdfparser.font

import marabillas.loremar.pdfparser.convertContentsToHex

internal interface CMap {
    fun decodeString(encoded: String): String

    fun extractActualEncoded(encodedSB: StringBuilder) {
        if (encodedSB.startsWith("(") && encodedSB.endsWith(")")) {
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
            val b1 = Integer.parseInt("${codeSpaceRange[0][i]}${codeSpaceRange[0][i + 1]}", 16)
            val b2 = Integer.parseInt("${codeSpaceRange[1][i]}${codeSpaceRange[1][i + 1]}", 16)
            val b3 = Integer.parseInt("${codeSB[i]}${codeSB[i + 1]}", 16)
            if (b3 !in b1..b2) return false
        }
        return true
    }
}