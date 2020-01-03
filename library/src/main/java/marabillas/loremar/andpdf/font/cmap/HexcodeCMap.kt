package marabillas.loremar.andpdf.font.cmap

import androidx.collection.SparseArrayCompat
import marabillas.loremar.andpdf.utils.exts.hexToInt

class HexcodeCMap(private val codeToHexArray: SparseArrayCompat<String>) : CMap {
    private val encodedSB = StringBuilder()
    private val decodedSB = StringBuilder()
    private val codeSB = StringBuilder()

    override fun decodeString(encoded: String): String {
        encodedSB.clear()
        decodedSB.clear()

        extractActualEncoded(encodedSB.append(encoded))

        for (i in 0 until encodedSB.length step 2) {
            codeSB.clear()
            codeSB.append(encodedSB, i, i + 2)
            val hexValue = codeToHexArray[codeSB.hexToInt()]
            codeSB.clear()
            codeSB.append(hexValue)
            decodedSB.append(codeSB.hexToInt().toChar())
        }

        // Convert to literal string for PDF
        encodeParentheses(decodedSB)
        decodedSB.insert(0, '(').append(')')
        return decodedSB.toString()
    }

    override fun charCodeToUnicode(code: Int): Int? {
        val hexValue = codeToHexArray[code]
        codeSB.clear()
        codeSB.append(hexValue)
        return codeSB.hexToInt()
    }
}