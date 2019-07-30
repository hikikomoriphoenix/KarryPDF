package marabillas.loremar.andpdf.font.cff

import android.support.v4.util.SparseArrayCompat
import marabillas.loremar.andpdf.exceptions.font.InvalidCFFException
import marabillas.loremar.andpdf.font.cmap.CMap
import marabillas.loremar.andpdf.utils.exts.set
import marabillas.loremar.andpdf.utils.length
import marabillas.loremar.andpdf.utils.logd
import java.lang.Math.pow
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class CFFParser(private val data: ByteArray, private val fontName: String) {
    private var pointer = 0
    private var charsetOffset = 0
    private var encodingOffset = 0
    private var charStringsOffset = 0
    private var charStringType = 2
    private var privateDictSize = 0
    private var privateDictOffset = 0
    private var defaultWidthX = 0f
    private var nominalWidthX = 0f
    private val glyphWidths = mutableListOf<Float>()
    private val charNameToGlyphIndexMap = hashMapOf<String, Int>()
    private val strings = mutableListOf<String>()

    init {
        val topDictNum = processNameIndex()
        processTopDict(topDictNum)
        processStringIndex()
        processPrivateDict()
        processCharStrings()
        processCharsets()
        charNameToGlyphIndexMap[".notdef"] = 0
    }

    private fun processNameIndex(): Int {
        val fontNames = getIndexObjectsArray(4)
        if (fontNames.count() == 1) {
            return 0
        } else {
            fontNames.forEachIndexed { i, nameData ->
                if (String(nameData) == fontName)
                    return i
            }
            throw InvalidCFFException("Can't determine which font to use")
        }
    }

    private fun processTopDict(num: Int) {
        val topDicts = getIndexObjectsArray(pointer)
        val topDict = topDicts[num]
        decodeDict(topDict, ::processTopDictOperation)
    }

    private fun processStringIndex() {
        val stringIndexData = getIndexObjectsArray(pointer)
        stringIndexData.forEach { stringData ->
            strings.add(String(stringData))
        }
    }

    private fun processPrivateDict() {
        val privateDictData = data.copyOfRange(privateDictOffset, privateDictOffset + privateDictSize)
        decodeDict(privateDictData, ::processPrivateDictOperation)
    }

    private fun getIndexObjectsArray(start: Int): Array<ByteArray> {
        pointer = start
        val count = getCard16At(pointer)
        pointer += 2
        val offSize = getCard8At(pointer)
        pointer++
        val offsetArray = IntArray(count + 1) {
            val offset = getOffset(offSize, pointer)
            pointer += offSize
            offset
        }

        val reference = pointer - 1
        val objectsArray = Array(count) { i ->
            val startOffset = offsetArray[i]
            val endOffset = offsetArray[i + 1]
            data.copyOfRange(reference + startOffset, reference + endOffset)
        }
        pointer = reference + offsetArray.last()
        return objectsArray
    }

    private fun decodeDict(
        dictData: ByteArray,
        onOperationAction: (operator: Int, operands: MutableList<Double>, isTwoByte: Boolean) -> Unit
    ) {
        val operands = mutableListOf<Double>()
        var i = 0
        while (i < dictData.size) {
            when (val byte = dictData[i].toInt() and 0xff) {
                in 32..246 -> operands.add((byte - 139).toDouble())
                in 247..250 -> {
                    val b1 = dictData[++i].toInt() and 0xff
                    operands.add(((byte - 247) * 256 + b1 + 108).toDouble())
                }
                in 251..254 -> {
                    val b1 = dictData[++i].toInt() and 0xff
                    operands.add((-(byte - 251) * 256 - b1 - 108).toDouble())
                }
                28 -> {
                    val b1 = dictData[++i].toInt() and 0xff
                    val b2 = dictData[++i].toInt() and 0xff
                    operands.add((b1 shl 8 or b2).toDouble())
                }
                29 -> {
                    val b1 = dictData[++i].toInt() and 0xff
                    val b2 = dictData[++i].toInt() and 0xff
                    val b3 = dictData[++i].toInt() and 0xff
                    val b4 = dictData[++i].toInt() and 0xff
                    operands.add((b1 shl 24 or b2 shl 16 or b3 shl 8 or b4).toDouble())
                }
                30 -> {
                    i = decodeRealNumberOperand(dictData, i, operands)
                }
                12 -> {
                    val b = dictData[++i].toInt() and 0xff
                    onOperationAction(b, operands, true)
                    operands.clear()
                }
                in 0..21 -> {
                    onOperationAction(byte, operands, false)
                    operands.clear()
                }
            }
            i++
        }
    }

    private fun decodeRealNumberOperand(dictData: ByteArray, startIndex: Int, operands: MutableList<Double>): Int {
        var i = startIndex
        var wholeNum = 0
        var fractNum = 0
        var exp = 0
        var isWholeNum = true
        var isFractional = false
        var isExponential = false
        var isNegative = false
        var isNegExp = false

        while (true) {
            val b = dictData[++i].toInt() and 0xff
            val nibble = IntArray(2)
            nibble[0] = (b and 0xf0) shr 4
            nibble[1] = b and 0x0f

            repeat(2) {
                when (nibble[it]) {
                    in 0..9 -> {
                        when {
                            isWholeNum -> {
                                wholeNum = wholeNum * 10 + nibble[it]
                            }
                            isFractional -> {
                                fractNum = fractNum * 10 + nibble[it]
                            }
                            isExponential -> {
                                exp = exp * 10 + nibble[it]
                            }
                        }
                    }
                    10 -> {
                        isWholeNum = false
                        isFractional = true
                    }
                    11 -> {
                        isFractional = false
                        isExponential = true
                    }
                    12 -> {
                        isFractional = false
                        isExponential = true
                        isNegExp = true
                    }
                    14 -> {
                        isNegative = true
                    }
                }
            }

            if (nibble[0] == 15 || nibble[1] == 15)
                break
        }

        if (isNegative) wholeNum *= -1
        if (isNegExp) exp *= -1

        var realNumOperand = wholeNum.toDouble()
        if (fractNum > 0) {
            val fractionalPart = fractNum / (pow(10.0, fractNum.length().toDouble()))
            realNumOperand += fractionalPart
        }
        realNumOperand *= pow(10.0, exp.toDouble())

        operands.add(realNumOperand)

        return i
    }

    private fun processTopDictOperation(operator: Int, operands: MutableList<Double>, isTwoByte: Boolean = false) {
        if (isTwoByte)
            processTopDictOperationPlus(operator, operands)
        else {
            when (operator) {
                15 -> charsetOffset = operands.last().toInt()
                16 -> encodingOffset = operands.last().toInt()
                17 -> charStringsOffset = operands.last().toInt()
                18 -> {
                    privateDictSize = operands[operands.lastIndex - 1].toInt()
                    privateDictOffset = operands.last().toInt()
                }
            }
        }
    }

    private fun processTopDictOperationPlus(operator: Int, operands: MutableList<Double>) {
        when (operator) {
            6 -> charStringType = operands.last().toInt()
        }
    }

    private fun processPrivateDictOperation(operator: Int, operands: MutableList<Double>, isTwoByte: Boolean = false) {
        when (operator) {
            20 -> defaultWidthX = operands.last().toFloat()
            21 -> nominalWidthX = operands.last().toFloat()
        }
    }

    private fun processCharStrings() {
        val charStrings = getIndexObjectsArray(charStringsOffset)
        charStrings.forEach { data ->
            if (charStringType == 1) {
                decodeType1CharString(data, ::processType1CharStringCommand)
            } else if (charStringType == 2) {
                decodeType2CharString(data, ::processType2CharStringCommand)
            }
        }
    }

    private fun decodeType1CharString(
        charStringData: ByteArray,
        onCommandAction: (operator: Int, operands: MutableList<Int>, glyphIndex: Int) -> Unit
    ) {
        glyphWidths.add(defaultWidthX)
        val glyphIndex = glyphWidths.lastIndex
        val operands = mutableListOf<Int>()
        var i = 0
        while (i < charStringData.size) {
            when (val byte = charStringData[i].toInt() and 0xFF) {
                in 32..246 -> operands.add(byte - 139)
                in 247..250 -> {
                    val w = charStringData[++i].toInt() and 0xFF
                    operands.add((byte - 247) * 256 + w + 108)
                }
                in 251..254 -> {
                    val w = charStringData[++i].toInt() and 0xFF
                    operands.add(-(byte - 251) * 256 - w - 108)
                }
                255 -> {
                    val buffer = ByteBuffer.allocate(4)
                    buffer.order(ByteOrder.BIG_ENDIAN)
                    repeat(4) {
                        buffer.put((charStringData[++i].toInt() and 0xFF).toByte())
                    }
                    buffer.flip()
                    operands.add(buffer.int)
                }
                in 0..31 -> {
                    if (byte != 12) {
                        onCommandAction(byte, operands, glyphIndex)
                    }
                    operands.clear()
                }
            }
            i++
        }
    }

    private fun processType1CharStringCommand(operator: Int, operands: MutableList<Int>, glyphIndex: Int) {
        when (operator) {
            13 -> glyphWidths[glyphIndex] = operands.last().toFloat() + nominalWidthX
        }
    }

    private fun decodeType2CharString(
        charStringData: ByteArray,
        onCommandAction: (operator: Int, operands: MutableList<Int>, glyphIndex: Int) -> Unit
    ) {
        glyphWidths.add(defaultWidthX)
        val glyphIndex = glyphWidths.lastIndex
        val operands = mutableListOf<Int>()
        var i = 0
        loop@ while (i < charStringData.size) {
            when (val byte = charStringData[i].toInt() and 0xFF) {
                in 32..246 -> operands.add(byte - 139)
                in 247..250 -> {
                    val w = charStringData[++i].toInt() and 0xFF
                    operands.add((byte - 247) * 256 + w + 108)
                }
                in 251..254 -> {
                    val w = charStringData[++i].toInt() and 0xFF
                    operands.add(-(byte - 251) * 256 - w - 108)
                }
                255 -> {
                    val buffer = ByteBuffer.allocate(4)
                    buffer.order(ByteOrder.BIG_ENDIAN)
                    repeat(4) {
                        buffer.put((charStringData[++i].toInt() and 0xFF).toByte())
                    }
                    buffer.flip()
                    operands.add(buffer.int)
                }
                28 -> {
                    val buffer = ByteBuffer.allocate(2)
                    buffer.order(ByteOrder.BIG_ENDIAN)
                    repeat(2) {
                        buffer.put((charStringData[++i].toInt() and 0xFF).toByte())
                    }
                    buffer.flip()
                    operands.add(buffer.short.toInt())
                }
                in 0..31 -> {
                    if (byte != 12) {
                        onCommandAction(byte, operands, glyphIndex)
                    }
                    operands.clear()
                    break@loop
                }
            }
            i++
        }
    }

    private fun processType2CharStringCommand(operator: Int, operands: MutableList<Int>, glyphIndex: Int) {
        val hasWidth = when (operator) {
            22, 4 -> operands.count() > 1
            21, 1, 3, 18, 23 -> operands.count() > 2
            14, 19, 20 -> operands.count() > 0
            else -> false
        }
        if (hasWidth)
            glyphWidths[glyphIndex] = operands.first().toFloat() + nominalWidthX
    }

    private fun processCharsets() {
        var ptr = charsetOffset
        val format = getCard8At(charsetOffset)
        ptr++
        val numGlyphs = glyphWidths.count()
        when (format) {
            0 -> {
                repeat(numGlyphs - 1) { index ->
                    val sid = getSIDAt(ptr)
                    val charName = getString(sid)
                    charNameToGlyphIndexMap[charName] = index + 1
                    ptr += 2
                }
            }
            1 -> {
                var count = 0
                while (count < numGlyphs - 1) {
                    val startSID = getSIDAt(ptr)
                    ptr += 2
                    val left = getCard8At(ptr)
                    for (sid in startSID..(startSID + left)) {
                        val charName = getString(sid)
                        charNameToGlyphIndexMap[charName] = ++count
                    }
                    ptr++
                }
            }
            2 -> {
                var count = 0
                while (count < numGlyphs - 1) {
                    val startSID = getSIDAt(ptr)
                    ptr += 2
                    val left = getCard16At(ptr)
                    for (sid in startSID..(startSID + left)) {
                        val charName = getString(sid)
                        charNameToGlyphIndexMap[charName] = ++count
                    }
                    ptr += 2
                }
            }
        }
    }

    fun getCharacterWidths(encodingArray: SparseArrayCompat<String>, cmap: CMap?): SparseArrayCompat<Float> {
        val characterWidths = SparseArrayCompat<Float>()

        for (i in 0 until encodingArray.size()) {
            val charCode = encodingArray.keyAt(i)
            val unicode = cmap?.charCodeToUnicode(charCode)

            val charName = encodingArray.valueAt(i)
            val glyphIndex = charNameToGlyphIndexMap[charName]
            if (glyphIndex != null) {
                val width = glyphWidths[glyphIndex]
                if (unicode is Int) {
                    characterWidths[unicode] = width
                }
            }
        }

        // Assign width for missing character
        val glyphIndex = charNameToGlyphIndexMap[".notdef"]
        if (glyphIndex != null) {
            val width = glyphWidths[glyphIndex]
            characterWidths[-1] = width
        }

        logd("${characterWidths.size()} widths obtained from Type1 font")
        return characterWidths
    }

    private fun getCard8At(i: Int): Int {
        return data[i].toInt() and 0xff
    }

    private fun getCard16At(i: Int): Int {
        return getUnsignedTwoBytes(i)
    }

    private fun getSIDAt(i: Int): Int {
        return getUnsignedTwoBytes(i)
    }

    private fun getUnsignedTwoBytes(i: Int): Int {
        var num = 0
        num = num or (data[i].toInt() and 0xff)
        num = num shl 8
        num = num or (data[i + 1].toInt() and 0xff)
        return num
    }

    private fun getOffset(offSize: Int, i: Int): Int {
        var num = 0
        num = num or (data[i].toInt() and 0xff)
        var pos = i
        repeat(offSize - 1) {
            num = num shl 8
            num = num or (data[++pos].toInt() and 0xff)
        }
        return num
    }

    private fun getString(sid: Int): String {
        return if (sid in 0 until CFFStandardStrings.count())
            CFFStandardStrings[sid]
        else
            CFFStandardStrings[sid - CFFStandardStrings.count()]
    }
}