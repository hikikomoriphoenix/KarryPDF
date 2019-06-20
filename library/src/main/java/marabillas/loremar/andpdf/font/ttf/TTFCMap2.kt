package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap2(data: ByteArray, pos: Long) : TTFCMapDefault() {
    init {
        var hiBytePos = pos + 6
        val subHeaderStart = hiBytePos + (256 * 2)
        repeat(256) { hiByte ->
            val k = TTFParser.getUInt16At(data, hiBytePos.toInt()) / 8
            if (k == 0) {
                val idRangeOffsetPos = subHeaderStart.toInt() + 6
                val idRangeOffset = TTFParser.getUInt16At(data, idRangeOffsetPos)
                val firstCodePos = idRangeOffsetPos + idRangeOffset
                val glyphIndexPos = firstCodePos + (hiByte * 2)
                val glyphIndex = TTFParser.getUInt16At(data, glyphIndexPos)
                map[hiByte] = glyphIndex
            } else {
                val subHeaderPos = subHeaderStart.toInt() + (k * 8)
                val firstCode = TTFParser.getUInt16At(data, subHeaderPos)
                val entryCount = TTFParser.getUInt16At(data, subHeaderPos + 2)
                val idDelta = TTFParser.getUInt16At(data, subHeaderPos + 4)
                val idRangeOffset = TTFParser.getUInt16At(data, subHeaderPos + 6)
                val idRangeOffsetPos = subHeaderPos + 6
                val firstCodePos = idRangeOffsetPos + idRangeOffset
                repeat(entryCount) { j ->
                    val secondByte = firstCode + j
                    val glyphIndexPos = firstCodePos + (j * 2)
                    var glyphIndex = TTFParser.getUInt16At(data, glyphIndexPos)
                    if (glyphIndex != 0) {
                        glyphIndex += idDelta
                    }
                    val charCode = (hiByte shl 8) or secondByte
                    map[charCode] = glyphIndex
                }
            }
            hiBytePos += 2
        }
    }
}