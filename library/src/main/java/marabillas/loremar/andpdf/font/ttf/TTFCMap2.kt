package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap2(data: ByteArray, pos: Long) : TTFCMapDefault(data, pos) {
    init {
        var hiBytePos = pos + 6
        checkIfLocationIsWithinTableLength(hiBytePos.toInt())
        val subHeaderStart = hiBytePos + (256 * 2)
        checkIfLocationIsWithinTableLength(subHeaderStart.toInt())
        repeat(256) { hiByte ->
            val k = TTFParser.getUInt16At(data, hiBytePos.toInt()) / 8
            if (k == 0) {
                val idRangeOffsetPos = subHeaderStart.toInt() + 6
                checkIfLocationIsWithinTableLength(idRangeOffsetPos)
                val idRangeOffset = TTFParser.getUInt16At(data, idRangeOffsetPos)
                val firstCodePos = idRangeOffsetPos + idRangeOffset
                checkIfLocationIsWithinTableLength(firstCodePos)
                val glyphIndexPos = firstCodePos + (hiByte * 2)
                checkIfLocationIsWithinTableLength(glyphIndexPos)
                val glyphIndex = TTFParser.getUInt16At(data, glyphIndexPos)
                map[hiByte] = glyphIndex
            } else {
                val subHeaderPos = subHeaderStart.toInt() + (k * 8)
                checkIfLocationIsWithinTableLength(subHeaderPos)
                val firstCode = TTFParser.getUInt16At(data, subHeaderPos)
                val entryCount = TTFParser.getUInt16At(data, subHeaderPos + 2)
                val idDelta = TTFParser.getUInt16At(data, subHeaderPos + 4)
                val idRangeOffset = TTFParser.getUInt16At(data, subHeaderPos + 6)
                val idRangeOffsetPos = subHeaderPos + 6
                checkIfLocationIsWithinTableLength(idRangeOffsetPos)
                val firstCodePos = idRangeOffsetPos + idRangeOffset
                checkIfLocationIsWithinTableLength(firstCodePos)
                repeat(entryCount) { j ->
                    val secondByte = firstCode + j
                    val glyphIndexPos = firstCodePos + (j * 2)
                    checkIfLocationIsWithinTableLength(glyphIndexPos)
                    var glyphIndex = TTFParser.getUInt16At(data, glyphIndexPos)
                    if (glyphIndex != 0) {
                        glyphIndex += idDelta
                    }
                    val charCode = (hiByte shl 8) or secondByte
                    map[charCode] = glyphIndex
                }
            }
            hiBytePos += 2
            checkIfLocationIsWithinTableLength(hiBytePos.toInt())
        }
    }
}