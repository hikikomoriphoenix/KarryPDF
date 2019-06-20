package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap10(data: ByteArray, pos: Long) : TTFCMapDefault(data, pos) {
    init {
        length = TTFParser.getUInt32At(data, pos.toInt() + 4).toInt()
        val startCharCode = TTFParser.getUInt32At(data, pos.toInt() + 12)
        val numChars = TTFParser.getUInt32At(data, pos.toInt() + 16)

        val start = pos + 20
        for (i in 0 until numChars.toInt()) {
            val indexLoc = start + (2 * i)
            checkIfLocationIsWithinTableLength(indexLoc.toInt())
            val glyphIndex = TTFParser.getUInt16At(data, indexLoc.toInt())
            map[startCharCode.toInt() + i] = glyphIndex
        }

    }
}