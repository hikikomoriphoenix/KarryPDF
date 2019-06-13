package marabillas.loremar.pdfparser.font.cmap

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.font.cmap.CMap.Companion.MISSING_CHAR
import marabillas.loremar.pdfparser.utils.exts.hexToInt
import marabillas.loremar.pdfparser.utils.exts.set
import java.io.IOException
import java.io.InputStream

/**
 * A CMap that makes use of the Adobe Glyph List to convert character code to unicode value. A character code to glyph
 * name mappings is required for this CMap
 */
internal class AGLCMap(private val codeToNameArray: SparseArrayCompat<String>) : CMap {
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
            decodedSB.append(
                unicodes[codeToNameArray[codeSB.hexToInt()]]?.toChar() ?: MISSING_CHAR
            )
        }

        // Convert to literal string for PDF
        encodeParentheses(decodedSB)
        decodedSB.insert(0, '(').append(')')
        return decodedSB.toString()
    }

    override fun charCodeToUnicode(code: Int): Int? {
        return unicodes[codeToNameArray[code]]
    }

    companion object {
        private val unicodes = HashMap<String, Int>()
        private val glyphNames = SparseArrayCompat<String>()
        private val stringBuilder = StringBuilder()

        init {
            val stream = javaClass.classLoader.getResourceAsStream("res/raw/glyphlist.txt")
            var isComment = false

            if (stream != null) {
                var c = read(stream) ?: throw IOException("Nothing was read from glyphlist.txt")
                while (true) {
                    // Skip comments
                    if (c == '\n' || c == '\r') {
                        c = read(stream) ?: break
                        if (c == '#') {
                            isComment = true
                            c = read(stream) ?: break
                            continue
                        } else {
                            isComment = false
                        }
                    } else if (isComment) {
                        c = read(stream) ?: break
                        continue
                    } else if (c == '#') {
                        isComment = true
                        c = read(stream) ?: break
                        continue
                    }

                    // Get character name to Unicode mappings.

                    // Get character name
                    stringBuilder.clear()
                    while (c != ';') {
                        stringBuilder.append(c)
                        c = read(stream) ?: break
                    }
                    val charName = stringBuilder.toString()

                    // Get unicode
                    stringBuilder.clear()
                    c = read(stream) ?: break
                    while (c != '\n' && c != '\r') {
                        if (c != ' ') {
                            stringBuilder.append(c)
                        }
                        c = read(stream) ?: break
                    }
                    val unicode = stringBuilder.hexToInt()
                    unicodes[charName] = unicode
                    glyphNames[unicode] = charName
                }
                stream.close()
            } else {
                throw IOException("Attempting to open glyphlist.txt but no stream was obtained.")
            }
        }

        private fun read(stream: InputStream): Char? {
            val read = stream.read()
            if (read == -1) return null
            return read.toChar()
        }

        fun charNameToUnicode(charName: String): Int? {
            return unicodes[charName]
        }

        fun unicodeToCharName(unicode: Int): String? {
            return glyphNames[unicode]
        }
    }
}