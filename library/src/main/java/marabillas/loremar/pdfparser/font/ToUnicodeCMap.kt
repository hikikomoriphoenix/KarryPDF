package marabillas.loremar.pdfparser.font

import android.util.SparseIntArray
import marabillas.loremar.pdfparser.hexFromInt
import marabillas.loremar.pdfparser.hexToInt

internal class ToUnicodeCMap(private var stream: String) : CMap {
    private val codeSpaceRange = ArrayList<Array<String>>()
    private val bfChars = SparseIntArray()
    private val bfRanges = mutableListOf<BfRange>()
    private var cMapName = ""
    private var ptr = 0
    private var end = 0
    private var loStart = 0
    private var loEnd = 0
    private var hiStart = 0
    private var hiEnd = 0
    private var dstStart = 0
    private var dstEnd = 0
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
    private var codeLength = 0
    private val srcCodeSB = StringBuilder()
    private val dstCodeSB = StringBuilder()

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
            // Determine if next code is valid.
            if (!isNextValid()) {
                decodedSB.append(" ")
                ptr += 2
                continue
            }

            srcCodeSB.clear()
            srcCodeSB.append(encodedSB, ptr, ptr + codeLength)

            val srcInt = srcCodeSB.hexToInt()

            // Attempt to get unicode from bfChars
            var dstInt = bfChars[srcInt]
            if (dstInt != 0) {
                convertCodeToCharAndAppend(
                    srcCodeSB.hexFromInt(dstInt)
                )
                continue
            }

            try {
                val range = bfRanges.first { srcInt in it.lo..it.hi }
                val offset = srcInt - range.lo
                if (range.dst.startsWith('<') && range.dst.endsWith('>')) {
                    dstCodeSB
                        .clear()
                        .append(range.dst)
                        .deleteCharAt(0)              // Delete '<'
                        .deleteCharAt(dstCodeSB.lastIndex)  // Delete '>'
                    dstInt = dstCodeSB.hexToInt() + offset
                    dstCodeSB.hexFromInt(dstInt)
                } else {
                    dstCodeSB.clear().append(range.dst)

                    // Locate the required dstCode
                    dstStart = 0
                    for (i in 0..offset) {
                        dstStart = dstCodeSB.indexOf('<', dstStart + 1)
                    }
                    if (dstStart != -1) {
                        dstEnd = dstCodeSB.indexOf('>', dstStart)
                        if (dstEnd != -1) {
                            // Delete everything in dst except the required dstCode
                            dstCodeSB.delete(dstEnd + 1, dstCodeSB.length)
                            dstCodeSB.delete(0, dstStart)
                        } else {
                            dstCodeSB.clear()
                        }
                    }
                }
                if (dstCodeSB.isNotEmpty()) {
                    convertCodeToCharAndAppend(dstCodeSB)
                    continue
                } else {
                    decodedSB.append(" ")
                    ptr += 2
                }
            } catch (e: NoSuchElementException) {
                decodedSB.append(" ")
                ptr += 2
            }
        }

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
            codeSB.length == 2 -> {
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