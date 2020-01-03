package marabillas.loremar.andpdf.font.ttf

import androidx.collection.SparseArrayCompat
import marabillas.loremar.andpdf.font.cmap.AGLCMap
import marabillas.loremar.andpdf.font.encoding.MacOSRomanEncoding
import marabillas.loremar.andpdf.font.encoding.StandardEncoding
import marabillas.loremar.andpdf.utils.exts.hexFromInt
import marabillas.loremar.andpdf.utils.exts.set
import marabillas.loremar.andpdf.utils.logd
import marabillas.loremar.andpdf.utils.wholeNumToFractional

internal class TTFParser(val data: ByteArray) {
    val stringBuilder = StringBuilder()
    var scalerType = ""
        private set
    var numTables: Int = 0
        private set
    var searchRange: Int = 0
        private set
    var entrySelector: Int = 0
        private set
    var rangeShift: Int = 0
        private set
    var platformID: Int? = null
        private set

    val tables = HashMap<String, Table>()
    var ttfCMap: TTFCMap? = null
        private set

    init {
        //val pos = parseOffsetSubTable()
        val pos = 0
        parseTableDirectory(pos)
    }

    private fun parseOffsetSubTable(): Int {
        stringBuilder.clear()
        for (i in 0..3) {
            stringBuilder.append(data[i].toChar())
        }
        scalerType = stringBuilder.toString()
        //logd("scaler -> $scalerType")

        var pos = 4
        numTables = getUInt16At(data, pos); pos += 2
        searchRange = getUInt16At(data, pos); pos += 2
        entrySelector = getUInt16At(data, pos); pos += 2
        rangeShift = getUInt16At(data, pos); pos += 2

        return pos
    }

    private fun parseTableDirectory(start: Int) {
        var pos = getNextTablePosition(start, data.size.toLong())
        var nearestOffset = data.size.toLong()
        while (pos < data.size) {
            //logd(stringBuilder.toString())
            pos += 4
            val checksum = getUInt32At(data, pos)
            //logd("checksum=$checksum")
            pos += 4
            val offset = getUInt32At(data, pos)
            //logd("offset=$offset")
            pos += 4
            val length = getUInt32At(data, pos)
            //logd("length=$length")
            pos += 4
            tables[stringBuilder.toString()] =
                Table(checksum, offset, length)
            if (offset < nearestOffset)
                nearestOffset = offset
            pos = getNextTablePosition(pos, nearestOffset)
            if (pos == -1)
                break
        }
    }

    private fun getNextTablePosition(start: Int, nearestOffset: Long): Int {
        var pos = start
        val chars = CharArray(4)
        while (true) {
            if (pos + 3 >= nearestOffset)
                return -1

            stringBuilder.clear()
            for (i in pos..(pos + 3)) {
                stringBuilder.append(data[i].toChar())
            }
            var isTag = false
            for (tag in tags) {
                stringBuilder.getChars(0, stringBuilder.length, chars, 0)
                if (tag.contentEquals(chars)) {
                    isTag = true
                    break
                }
            }
            if (isTag)
                break
            else
                pos++
        }

        return pos
    }

    fun getCharacterWidths(): SparseArrayCompat<Float> {
        logd("Getting character widths from a TrueType font")

        val numOfLongHorMetrics = getNumOfLongHorMetrics()
        ttfCMap = getCMap()
        if (ttfCMap != null) {
            if (numOfLongHorMetrics > 0) {
                val glyphWidths = getAdvancedWidths(numOfLongHorMetrics)
                val widths = getCharacterWidths(glyphWidths, ttfCMap as TTFCMap)
                logd("${widths.size()} widths obtained")
                return widths
            } else {
                val glyphWidths = getGlyphBoundingBoxWidths()
                if (glyphWidths.count() > 0) {
                    val widths = getCharacterWidths(glyphWidths, ttfCMap as TTFCMap)
                    logd("${widths.size()} widths obtained")
                    return widths
                }
            }
        } else {
            logd("Can not obtain valid TTF cmap")
        }

        return SparseArrayCompat()
    }

    private fun getCharacterWidths(glyphWidths: IntArray, srcCMap: TTFCMap): SparseArrayCompat<Float> {
        val characterWidths = SparseArrayCompat<Float>()
        // Assign missing character width to -1
        characterWidths.put(-1, glyphWidths[0].toFloat())

        val map = SparseArrayCompat<Int>()
        srcCMap.getAll(map)
        for (i in 0 until map.size()) {
            val charCode = map.keyAt(i)
            val glyphIndex = map.valueAt(i)
            if (platformID != 1) {
                if (
                    glyphIndex in 0..glyphWidths.lastIndex
                    && glyphWidths[glyphIndex] > 0
                ) {
                    characterWidths.put(charCode, glyphWidths[glyphIndex].toFloat())
                }
            } else {
                val charName = MacOSRomanEncoding[charCode]
                val unicode = charName?.let { AGLCMap.charNameToUnicode(it) }
                if (
                    glyphIndex in 0..glyphWidths.lastIndex
                    && glyphWidths[glyphIndex] > 0
                    && unicode != null
                ) {
                    characterWidths.put(unicode, glyphWidths[glyphIndex].toFloat())
                }
            }
        }

        return characterWidths
    }

    private fun getNumOfLongHorMetrics(): Int {
        var num = 0
        val pos = tables["hhea"]?.offset
        if (pos is Long) {
            // numOfLongHorMetrics is located at the 32nd byte from offset.
            num = getUInt16At(data, (pos + 32).toInt())
        }
        return num
    }

    private fun getAdvancedWidths(num: Int): IntArray {
        val widths = IntArray(num)
        val pos = tables["hmtx"]?.offset
        if (pos is Long) {
            for (i in 0 until widths.count()) {
                // A longHorMetric is composed of advancedWidth and leftSideBearing. Each of which are 2 bytes in size.
                widths[i] = getUInt16At(data, pos.toInt() + (i * 4))
            }
        }
        return widths
    }

    private fun getGlyphBoundingBoxWidths(): IntArray {
        val maxpPos = tables["maxp"]?.offset
        val headPos = tables["head"]?.offset
        val locaPos = tables["loca"]?.offset
        val glyfPos = tables["glyf"]?.offset

        if (maxpPos != null && headPos != null && locaPos != null && glyfPos != null) {
            val numGlyphs = getUInt16At(data, maxpPos.toInt() + 4)
            val glyphWidths = IntArray(numGlyphs)
            val indexToLocFormat = getUInt16At(data, headPos.toInt() + 50)
            if (indexToLocFormat != 0 && indexToLocFormat != 1) {
                return intArrayOf()
            }
            var glyphLocPos = locaPos
            for (i in 0 until numGlyphs) {
                if (indexToLocFormat == 0) {
                    val glyphPos = glyfPos + (2 * getUInt16At(data, glyphLocPos.toInt()))
                    val xMin = getUInt16At(data, glyphPos.toInt() + 2)
                    val xMax = getUInt16At(data, glyphPos.toInt() + 6)
                    glyphWidths[i] = xMax - xMin
                    glyphLocPos += 2
                } else {
                    val glyphPos = glyfPos + getUInt16At(data, glyphLocPos.toInt())
                    val xMin = getUInt16At(data, glyphPos.toInt() + 2)
                    val xMax = getUInt16At(data, glyphPos.toInt() + 6)
                    glyphWidths[i] = xMax - xMin
                    glyphLocPos += 4
                }
            }

            return glyphWidths
        }

        return intArrayOf()
    }

    private fun getCMap(): TTFCMap? {
        if (ttfCMap != null) return ttfCMap

        var pos = tables["cmap"]?.offset
        if (pos is Long) {
            val numOfSubTables = getUInt16At(data, pos.toInt() + 2)

            var selectedTablePriority = 8
            var selectedTTFCMap: TTFCMap? = null
            var selectedPlatformID: Int? = null
            var selectedPlatformSpecificID: Int? = null
            var selectedFormat: Int? = null
            pos += 4
            for (i in 0 until numOfSubTables) {
                val platformID = getUInt16At(data, pos.toInt())
                val platformSpecificID = getUInt16At(data, pos.toInt() + 2)

                val priority = platformPriority(platformID, platformSpecificID)

                if (priority < selectedTablePriority) {
                    // Get the CMap
                    val offset = getUInt32At(data, pos.toInt() + 4)
                    val cmapTableLoc = pos + offset
                    val format = getUInt16At(data, cmapTableLoc.toInt())
                    logd("($platformID, $platformSpecificID) format=$format")
                    val ttfCMap = TTFCMapFactory().getTTFCMap(format, data, cmapTableLoc)

                    if (ttfCMap is TTFCMap) {
                        selectedTTFCMap = ttfCMap
                        selectedTablePriority = priority
                        selectedPlatformID = platformID
                        selectedPlatformSpecificID = platformSpecificID
                        selectedFormat = format
                    }

                }
                pos += 8
            }

            logd("platformID=$selectedPlatformID, platformSpecificID=$selectedPlatformSpecificID")
            logd("TTF CMap format = $selectedFormat")
            platformID = selectedPlatformID
            return selectedTTFCMap
        }
        return null
    }

    private fun platformPriority(platformID: Int, platformSpecificID: Int): Int {
        return when {
            platformID == 0 && platformSpecificID == 4 -> 0
            platformID == 0 && platformSpecificID < 4 -> 1
            platformID == 3 && platformSpecificID == 10 -> 2
            platformID == 3 && platformSpecificID == 1 -> 3
            platformID == 3 && platformSpecificID == 0 -> 4
            platformID == 0 -> 5
            platformID == 1 -> 6
            else -> 7
        }
    }

    fun getGlyphWidths(): IntArray {
        val numOfLongHorMetrics = getNumOfLongHorMetrics()
        return if (numOfLongHorMetrics > 0) {
            getAdvancedWidths(numOfLongHorMetrics)
        } else {
            getGlyphBoundingBoxWidths()
        }
    }

    fun getBuiltInEncoding(encodingArray: SparseArrayCompat<String>) {
        logd("Getting TyueType font built-in encoding")
        val pos = tables["post"]?.offset
        val ttfCMap = getCMap()
        if (pos is Long && ttfCMap is TTFCMap) {
            val mapper = TTFGlyphNamesMapper()
            val glyphNamesArray = SparseArrayCompat<String>()
            val format = getFixedAt(data, pos.toInt())
            logd("post format=$format")
            when (format) {
                1f -> mapper.post1(glyphNamesArray)
                2f -> mapper.post2(glyphNamesArray, data, pos.toInt() + 32)
                2.5f -> mapper.post25(glyphNamesArray, data, pos.toInt() + 32)
                3f -> {
                    if (platformID == 1) {
                        MacOSRomanEncoding.putAllTo(encodingArray)
                    } else {
                        StandardEncoding.putAllTo(encodingArray)
                    }
                    return
                }
                4f -> {
                    val maxpPos = tables["maxp"]?.offset
                    if (maxpPos != null) {
                        val numGlyphs = getUInt16At(data, maxpPos.toInt() + 4)
                        mapper.post4(glyphNamesArray, data, pos.toInt() + 32, numGlyphs)
                    } else {
                        if (platformID == 1) {
                            MacOSRomanEncoding.putAllTo(encodingArray)
                        } else {
                            StandardEncoding.putAllTo(encodingArray)
                        }
                        return
                    }
                }
            }

            // Populate encoding array using cmap to get the glyph index of a given character code and using the resulting
            // array from post to get the glyph name of the corresponding glyph index. The given character code will be
            // the key for an entry in encoding array and the resulting glyph name will be its corresponding value.
            val cMapArray = SparseArrayCompat<Int>()
            ttfCMap.getAll(cMapArray)
            for (i in 0 until cMapArray.size()) {
                val charCode = cMapArray.keyAt(i)
                val glyphIndex = cMapArray.valueAt(i)
                if (platformID != 1) {
                    encodingArray.put(charCode, glyphNamesArray[glyphIndex])
                } else {
                    val charName = MacOSRomanEncoding[charCode]
                    val unicode = charName?.let { AGLCMap.charNameToUnicode(it) }
                    if (unicode != null) {
                        encodingArray.put(unicode, glyphNamesArray[glyphIndex])
                    }
                }
            }
        }
    }

    fun getDefaultTTFEncoding(encodingArray: SparseArrayCompat<String>) {
        logd("Getting TrueType font default encoding")
        getCMap()
        when (platformID) {
            1 -> MacOSRomanEncoding.putAllTo(encodingArray)
            3 -> // Symbolic flag is assumed to be set and thus platformSpecificID is also assumed to be 0
                for (i in 0..255) {
                    stringBuilder.clear()
                    stringBuilder.hexFromInt(i)
                    stringBuilder.insert(0, "F0")
                    encodingArray[i] = stringBuilder.toString()
                }
            else -> // In case the font uses other Unicode platformID,
                StandardEncoding.putAllTo(encodingArray)
        }
    }

    companion object {
        val tags = setOf(
            charArrayOf('c', 'm', 'a', 'p'),
            charArrayOf('g', 'l', 'y', 'f'),
            charArrayOf('h', 'e', 'a', 'd'),
            charArrayOf('h', 'h', 'e', 'a'),
            charArrayOf('h', 'm', 't', 'x'),
            charArrayOf('l', 'o', 'c', 'a'),
            charArrayOf('m', 'a', 'x', 'p'),
            charArrayOf('n', 'a', 'm', 'e'),
            charArrayOf('p', 'o', 's', 't'),
            charArrayOf('c', 'v', 't', ' '),
            charArrayOf('f', 'p', 'g', 'm'),
            charArrayOf('h', 'd', 'm', 'x'),
            charArrayOf('k', 'e', 'r', 'n'),
            charArrayOf('O', 'S', '/', '2'),
            charArrayOf('p', 'r', 'e', 'p')
        )

        fun getUInt16At(data: ByteArray, start: Int): Int {
            var num = 0
            num = num or (data[start].toInt() and 0xff)
            num = num shl 8
            num = num or (data[start + 1].toInt() and 0xff)
            return num
        }

        fun getUInt32At(data: ByteArray, start: Int): Long {
            var num = 0L
            num = num or (data[start].toInt() and 0xff).toLong()
            num = num shl 8
            num = num or (data[start + 1].toInt() and 0xff).toLong()
            num = num shl 8
            num = num or (data[start + 2].toInt() and 0xff).toLong()
            num = num shl 8
            num = num or (data[start + 3].toInt() and 0xff).toLong()
            return num
        }

        fun getFixedAt(data: ByteArray, start: Int): Float {
            val integralDigits = getUInt16At(data, start)
            val fractionalDigits = getUInt16At(data, start + 2)
            val fractionalValue = wholeNumToFractional(fractionalDigits)
            return integralDigits.toFloat() + fractionalValue
        }
    }

    data class Table(val checksum: Long, val offset: Long, val length: Long)
}