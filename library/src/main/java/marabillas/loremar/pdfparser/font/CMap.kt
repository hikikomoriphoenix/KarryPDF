package marabillas.loremar.pdfparser.font

internal interface CMap {
    fun decodeString(encoded: String): String

    fun isWithinRange(codeSpaceRange: Array<String>, code: String): Boolean {
        val l = codeSpaceRange[0].length
        if (code.length != l) return false
        for (i in 0 until l step 2) {
            val b1 = Integer.parseInt("${codeSpaceRange[0][i]}${codeSpaceRange[0][i + 1]}", 16)
            val b2 = Integer.parseInt("${codeSpaceRange[1][i]}${codeSpaceRange[1][i + 1]}", 16)
            val b3 = Integer.parseInt("${code[i]}${code[i + 1]}", 16)
            if (b3 !in b1..b2) return false
        }
        return true
    }
}