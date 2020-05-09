package marabillas.loremar.karrypdf.font.ttf

import marabillas.loremar.karrypdf.utils.exts.set
import marabillas.loremar.karrypdf.utils.logd

internal class TTFCMap0(data: ByteArray, pos: Long) : TTFCMapDefault(data, pos) {
    init {
        logd("length=$length")
        val start = pos + 6
        for (i in 0..255) {
            val indexPos = start.toInt() + i
            //checkIfLocationIsWithinTableLength(indexPos)
            // Get byte and convert to unsigned int
            val glyphIndex = data[indexPos].toInt() and 0xff
            map[i] = glyphIndex
        }
    }
}