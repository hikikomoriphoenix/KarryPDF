package marabillas.loremar.pdfparser.font.cmap

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.pdfparser.hexFromInt
import marabillas.loremar.pdfparser.hexToInt

/**
 * @param stream The stream contents of this ToUnicodeCMap
 */
internal class ToUnicodeCMap(private var stream: String) : EmbeddedCMap {
    /**
     * List of code spaces. A code that is outside the code space range does not have mapping.
     */
    private val codeSpaceRange = ArrayList<Array<String>>()
    /**
     * A list of single mappings
     */
    private val bfChars = SparseArrayCompat<Int>()
    /**
     * A list of ranged mappings
     */
    private val bfRanges = mutableListOf<BfRange>()
    /**
     * CMapName value indicated in the cmap file. Empty means not indicated
     */
    private var cMapName = ""
    /**
     * Current position in text being parsed or decoded
     */
    private var ptr = 0
    /**
     * Position of endcodespacerange, endbfchar, or endbfrange
     */
    private var end = 0
    /**
     * Position of a ranges's starting value's '<'
     */
    private var loStart = 0
    /**
     * Position of a range's starting value's '>'
     */
    private var loEnd = 0
    /**
     * Position of a range's ending value's '<'
     */
    private var hiStart = 0
    /**
     * Position of a range's ending value's '>'
     */
    private var hiEnd = 0
    /**
     * Starting position of a mapping's value
     */
    private var dstStart = 0
    /**
     * Ending position of a mapping's value
     */
    private var dstEnd = 0
    /**
     * Position of beginbfchar, beginbfrange, or endcmap
     */
    private var operator = 0

    // Operators
    private val BEGINCODESPACERANGE = "begincodespacerange"
    private val ENDCODESPACERANGE = "endcodespacerange"
    private val BEGINBFCHAR = "beginbfchar"
    private val ENDBFCHAR = "endbfchar"
    private val BEGINBFRANGE = "beginbfrange"
    private val ENDBFRANGE = "endbfrange"
    private val ENDCMAP = "endcmap"

    private val operatorList = listOf(BEGINBFCHAR, BEGINBFRANGE, ENDCMAP)
    private val encloseList = charArrayOf('<', '[')

    private val encodedSB = StringBuilder()
    private val decodedSB = StringBuilder()
    /*private val IDENTITY = "Identity"
    private val UCS = "UCS"
    private val ZERO = "00"*/
    /**
     * Length of current code to be decoded.
     */
    private var codeLength = 0
    /**
     * StringBuilder to hold code to be decoded.
     */
    private val srcCodeSB = StringBuilder()
    /**
     * StringBuilder to hold map value for code
     */
    private val dstCodeSB = StringBuilder()

    /**
     * Class to hold values for a mapping within beginbfrange and endbfrange
     */
    data class BfRange(val lo: Int, val hi: Int, val dst: String)

    fun parse(): ToUnicodeCMap {
        //print("cmap -> $stream")

        ptr = getCMapName()

        // Parse for code spaces.
        ptr = stream.indexOf(BEGINCODESPACERANGE)
        ptr += 19
        end = stream.indexOf(ENDCODESPACERANGE)
        while (ptr < end) {
            loStart = stream.indexOf('<', ptr)
            loEnd = stream.indexOf('>', loStart)
            hiStart = stream.indexOf('<', loEnd)
            hiEnd = stream.indexOf('>', hiStart)
            codeSpaceRange.add(
                arrayOf(
                    stream.substring(loStart + 1, loEnd), // Exclude '<' and '>'
                    stream.substring(hiStart + 1, hiEnd) // Exclude '<' and '>'
                )
            )
            ptr = hiEnd + 1
            // Skip whitespaces.
            skipWhitespaces()
        }

        var cStart: Int
        var cEnd: Int
        // Get mappings.
        loop@ while (ptr < stream.length) {
            operator = stream.indexOfAny(operatorList, ptr)
            when {
                stream.startsWith(BEGINBFCHAR, operator) -> {
                    ptr = operator + 11
                    end = stream.indexOf(ENDBFCHAR, ptr)
                    while (ptr < end) {
                        cStart = stream.indexOf('<', ptr)
                        cEnd = stream.indexOf('>', cStart)
                        dstStart = stream.indexOf('<', cEnd)
                        dstEnd = stream.indexOf('>', dstStart)

                        // Add mapping. Make sure to exclude '<' and '>'
                        bfChars.put(
                            srcCodeSB.clear()
                                .append(stream, cStart + 1, cEnd)
                                .hexToInt(),
                            dstCodeSB.clear()
                                .append(stream, dstStart + 1, dstEnd)
                                .hexToInt()
                        )

                        ptr = dstEnd + 1
                        skipWhitespaces()
                    }
                }
                stream.startsWith(BEGINBFRANGE, operator) -> {
                    ptr = operator + 12
                    end = stream.indexOf(ENDBFRANGE, ptr)
                    while (ptr < end) {
                        loStart = stream.indexOf('<', ptr)
                        loEnd = stream.indexOf('>', loStart)
                        hiStart = stream.indexOf('<', loEnd)
                        hiEnd = stream.indexOf('>', hiStart)
                        dstStart = stream.indexOfAny(encloseList, hiEnd)
                        when (stream[dstStart]) {
                            '<' -> dstEnd = stream.indexOf('>', dstStart)
                            '[' -> dstEnd = stream.indexOf(']', dstStart)
                        }

                        // Add mapping. Exclude '<' and '>' in lo and hi.
                        bfRanges.add(
                            BfRange(
                                srcCodeSB.clear().append(stream, loStart + 1, loEnd).hexToInt(),
                                srcCodeSB.clear().append(stream, hiStart + 1, hiEnd).hexToInt(),
                                stream.substring(dstStart, dstEnd + 1)
                            )
                        )

                        ptr = dstEnd + 1
                        skipWhitespaces()
                    }
                }
                stream.startsWith(ENDCMAP, operator) -> {
                    break@loop
                }
            }
        }
        return this
    }

    private fun getCMapName(): Int {
        val i = stream.indexOf("/CMapName")
        return if (i != -1) {
            val nameStart = stream.indexOf('/', i + 1)
            val nameEnd = stream.indexOf(" def", nameStart)
            cMapName = stream.substring(nameStart, nameEnd)
            nameEnd
        } else {
            0
        }
    }

    override fun decodeString(encoded: String): String {
        encodedSB.clear().append(encoded)
        decodedSB.clear()

        extractActualEncoded(encodedSB)

        /*// Add missing "00" in first code
        if (cMapName.contains(IDENTITY, true) && cMapName.contains(UCS, true)) {
            encodedSB.insert(0, ZERO)
        }*/

        ptr = 0
        while (ptr < encodedSB.length) {
            // Determine if next code is within code space range. If not, proceed to next code.
            if (!isNextValid()) {
                decodedSB.append("□")
                ptr += 2
                continue
            }

            // Extract code to be decoded into the appropriate StringBuilder.
            srcCodeSB.clear()
            srcCodeSB.append(encodedSB, ptr, ptr + codeLength)

            // Convert code to integer. This integer will help determine the code's position within a range or get the
            // actual value mapped to this integer
            val srcInt = srcCodeSB.hexToInt()

            // Attempt to get unicode from bfChars
            var dstInt = bfChars[srcInt]
            if (dstInt != null) {
                convertCodeToCharAndAppend(
                    srcCodeSB.hexFromInt(dstInt)
                )
                continue
            }

            // Get mapped value within beginbfrange and endbfrange
            try {
                // Get the range which includes the code.
                val range = bfRanges.first { srcInt in it.lo..it.hi }
                // The offset value is the position of the code within the range.
                val offset = srcInt - range.lo
                if (range.dst.startsWith('<') && range.dst.endsWith('>')) {
                    // Get mapped value. This value is mapped to the starting code in the range. Add offset to this value
                    // to get the required code.
                    dstCodeSB
                        .clear()
                        .append(range.dst)
                        .deleteCharAt(0)              // Delete '<'
                        .deleteCharAt(dstCodeSB.lastIndex)  // Delete '>'
                    dstInt = dstCodeSB.hexToInt() + offset
                    // Get required code.
                    dstCodeSB.hexFromInt(dstInt)
                } else { //Else when value is an array of codes mapped to each code in the range.
                    dstCodeSB.clear().append(range.dst)

                    // Locate the required code in the array.
                    dstStart = 0
                    for (i in 0..offset) {
                        dstStart = dstCodeSB.indexOf('<', dstStart + 1)
                    }
                    if (dstStart != -1) {
                        dstEnd = dstCodeSB.indexOf('>', dstStart)
                        if (dstEnd != -1) {
                            // Delete everything in the array except the required code, also deleting surrounding
                            // '<' and '>'.
                            dstCodeSB.delete(dstEnd, dstCodeSB.length)
                            dstCodeSB.delete(0, dstStart + 1)
                        } else {
                            dstCodeSB.clear()
                        }
                    }
                }
                if (dstCodeSB.isNotEmpty()) {
                    convertCodeToCharAndAppend(dstCodeSB)
                    continue
                } else {
                    decodedSB.append("□")
                    ptr += 2
                }
            } catch (e: NoSuchElementException) {
                decodedSB.append("□")
                ptr += 2
            }
        }

        // Convert to literal string for PDF
        decodedSB.insert(0, '(').append(')')
        return decodedSB.toString()
    }

    private fun isNextValid(): Boolean {
        codeSpaceRange.forEach {
            codeLength = it[0].length
            if (encodedSB.length < codeLength)
                return@forEach
            srcCodeSB.clear()
            srcCodeSB.append(encodedSB, ptr, ptr + codeLength)
            if (isWithinRange(it, srcCodeSB))
                return true
        }
        return false
    }

    private fun convertCodeToCharAndAppend(codeSB: StringBuilder) {
        when {
            codeSB.length % 4 != 0 -> {
                decodedSB.append(
                    codeSB.hexToInt().toChar()
                )
            }
            else -> {
                for (i in 0 until codeSB.length step 4) {
                    decodedSB.append(
                        codeSB.hexToInt(i, i + 4).toChar()
                    )
                }
            }
        }
        ptr += codeLength
    }

    private fun skipWhitespaces() {
        while (ptr < end) {
            if (stream[ptr] == ' ' || stream[ptr] == '\n' || stream[ptr] == '\r')
                ptr++
            else
                break
        }
    }
}