package marabillas.loremar.pdfparser.font.ttf

import marabillas.loremar.pdfparser.utils.exts.set

internal class TTFCMap4(val data: ByteArray, val pos: Long) : TTFCMapDefault() {
    init {
        // Get segCountX2 word and divide by 2 to get number of segments.
        val segCount = TTFParser.getUInt16At(data, pos.toInt() + 6) / 2

        // Get endCodes.
        var start = pos + 14
        val endCodes = IntArray(segCount)
        fillImportantArrayForSegments(endCodes, start, segCount)

        // Get startCodes.
        start += (segCount * 2 + 2)
        val startCodes = IntArray(segCount)
        fillImportantArrayForSegments(startCodes, start, segCount)

        // Get idDeltas.
        start += (segCount * 2)
        val idDeltas = IntArray(segCount)
        fillImportantArrayForSegments(idDeltas, start, segCount)

        // Get idRangeOffset.
        start += (segCount * 2)
        val idRangeOffset = IntArray(segCount)
        fillImportantArrayForSegments(idRangeOffset, start, segCount)

        // Get position of glyphIndexArray
        start += (segCount * 2)

        for (i in 0 until segCount) {
            if (idRangeOffset[i] != 0) {
                for (k in startCodes[i]..endCodes[i]) {
                    val indexPos = start.toInt() + idRangeOffset[i] + (2 * (k - startCodes[i]))
                    var glyphIndex = TTFParser.getUInt16At(data, indexPos)
                    if (glyphIndex != 0) {
                        glyphIndex += idDeltas[i]
                    }
                    map[k] = glyphIndex
                }
            } else {
                for (k in startCodes[i]..endCodes[i]) {
                    val glyphIndex = k + idDeltas[i]
                    map[k] = glyphIndex
                }
            }
        }
    }

    private fun fillImportantArrayForSegments(array: IntArray, start: Long, segCount: Int) {
        for (i in 0 until segCount) {
            array[i] = TTFParser.getUInt16At(data, start.toInt() + (i * 2))
        }
    }
}