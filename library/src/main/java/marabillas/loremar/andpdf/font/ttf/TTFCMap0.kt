package marabillas.loremar.andpdf.font.ttf

import marabillas.loremar.andpdf.utils.exts.set

internal class TTFCMap0(val data: ByteArray, val pos: Long) : TTFCMapDefault() {
    init {
        val start = pos + 6
        for (i in 0..255) {
            // Get byte and convert to unsigned int
            val glyphIndex = data[start.toInt() + i].toInt() and 0xff
            map[i] = glyphIndex
        }
    }
}