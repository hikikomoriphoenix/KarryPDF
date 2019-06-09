package marabillas.loremar.pdfparser.font.cmap

import marabillas.loremar.pdfparser.utils.exts.hexToInt

internal interface EmbeddedCMap : CMap {
    companion object {
        private val sb = StringBuilder()
    }
    fun isWithinRange(codeSpaceRange: Array<String>, codeSB: StringBuilder): Boolean {
        val l = codeSpaceRange[0].length
        if (codeSB.length != l) return false
        for (i in 0 until l step 2) {
            val b1 = EmbeddedCMap.sb.clear().append(codeSpaceRange[0][i]).append(codeSpaceRange[0][i + 1]).hexToInt()
            val b2 = EmbeddedCMap.sb.clear().append(codeSpaceRange[1][i]).append(codeSpaceRange[1][i + 1]).hexToInt()
            val b3 = EmbeddedCMap.sb.clear().append(codeSB[i]).append(codeSB[i + 1]).hexToInt()
            if (b3 !in b1..b2) return false
        }
        return true
    }
}