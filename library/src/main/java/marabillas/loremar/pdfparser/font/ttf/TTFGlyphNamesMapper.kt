package marabillas.loremar.pdfparser.font.ttf

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.font.cmap.AGLCMap
import marabillas.loremar.pdfparser.utils.exts.set

internal class TTFGlyphNamesMapper {
    fun post1(glyphNamesArray: SparseArrayCompat<String>) {
        MacOSStandardGlyphs.putAllTo(glyphNamesArray)
    }

    fun post2(glyphNamesArray: SparseArrayCompat<String>, data: ByteArray, start: Int) {
        val numGlyphs = TTFParser.getUInt16At(data, start)

        var pos = start + 2
        for (i in 0 until numGlyphs) {
            val nameIndex = TTFParser.getUInt16At(data, pos)
            var charName: String? = null

            if (nameIndex in 0..257) {
                // Get name from standard Macintosh ordering
                charName = MacOSStandardGlyphs[nameIndex]
            } else if (nameIndex in 258..32767) {
                // Get name from the Pascal strings at the end of the post format 2 subtable
                val newNameIndex = nameIndex - 258
                val offset = start + (2 * numGlyphs) + (2 * newNameIndex)
                // The first byte of a Pascal string provides the length of the string. The glyph name will therefore be
                // equals to length - 1 to exclude the first byte.
                val length = data[offset].toInt() and 0xff
                var cPos = offset + 1
                val cArray = CharArray(length - 1)
                repeat(length - 1) {
                    cArray[it] = (data[cPos].toInt() and 0xff).toChar()
                    cPos++
                }
                charName = cArray.toString()
            }

            // Map name to glyph index
            charName?.let {
                glyphNamesArray[i] = it
            }
            pos += 2
        }
    }

    fun post25(glyphNamesArray: SparseArrayCompat<String>, data: ByteArray, start: Int) {
        val numGlyphs = TTFParser.getUInt16At(data, start)

        var pos = start + 2
        for (i in 0 until numGlyphs) {
            // Get signed offset
            val offset = data[pos].toInt()
            val nameIndex = offset + i
            val charName = MacOSStandardGlyphs[nameIndex]
            // Map name to glyph index
            charName?.let {
                glyphNamesArray[i] = it
            }
            pos += 2
        }
    }

    fun post4(glyphNamesArray: SparseArrayCompat<String>, data: ByteArray, start: Int, numGlyphs: Int) {
        var pos = start
        repeat(numGlyphs) { glyphIndex ->
            val unicode = TTFParser.getUInt16At(data, pos)
            val charName = AGLCMap.unicodeToCharName(unicode)
            // Map name to glyph index
            charName?.let {
                glyphNamesArray[glyphIndex] = it
            }
            pos += 2
        }
    }
}