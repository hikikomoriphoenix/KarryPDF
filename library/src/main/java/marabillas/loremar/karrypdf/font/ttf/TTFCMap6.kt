package marabillas.loremar.karrypdf.font.ttf

import marabillas.loremar.karrypdf.utils.exts.set

internal class TTFCMap6(data: ByteArray, pos: Long) : TTFCMapDefault(data, pos) {
    init {
        val firstCode = TTFParser.getUInt16At(data, pos.toInt() + 6)
        val entryCount = TTFParser.getUInt16At(data, pos.toInt() + 8)
        val start = pos + 10
        checkIfLocationIsWithinTableLength(start.toInt())
        for (i in 0 until entryCount) {
            val indexLoc = start + (2 * i)
            checkIfLocationIsWithinTableLength(indexLoc.toInt())
            val glyphIndex = TTFParser.getUInt16At(data, indexLoc.toInt())
            map[firstCode + i] = glyphIndex
        }
    }
}