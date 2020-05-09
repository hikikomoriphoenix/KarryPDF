package marabillas.loremar.karrypdf.font.ttf

import marabillas.loremar.karrypdf.exceptions.font.InvalidTTFCMapException
import marabillas.loremar.karrypdf.utils.exts.set

internal class TTFCMap13(data: ByteArray, pos: Long) : TTFCMapDefault(data, pos) {
    init {
        length = TTFParser.getUInt32At(data, pos.toInt() + 4).toInt()
        val nGroups = TTFParser.getUInt32At(data, pos.toInt() + 12)
        var start = pos + 16
        repeat(nGroups.toInt()) {
            checkIfLocationIsWithinTableLength(start.toInt() + 8)
            val startCharCode = TTFParser.getUInt32At(data, start.toInt())
            val endCharCode = TTFParser.getUInt32At(data, start.toInt() + 4)
            if (endCharCode > 200000) {
                throw InvalidTTFCMapException(
                    "Last character code exceeds beyond expected total number of character codes"
                )
            }
            val glyphIndex = TTFParser.getUInt32At(data, start.toInt() + 8)
            for (k in startCharCode..endCharCode) {
                map[k.toInt()] = glyphIndex.toInt()
            }
            start += 12
        }
    }
}