package marabillas.loremar.karrypdf.contents.text

import marabillas.loremar.karrypdf.contents.CmykToRgbConverter
import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.objects.PDFObject
import marabillas.loremar.karrypdf.objects.toPDFObject
import marabillas.loremar.karrypdf.utils.exts.*
import marabillas.loremar.karrypdf.utils.multiplyTransformMatrices

internal class TextObjectParser(
    private val context: KarryPDFContext,
    private val obj: Int,
    private val gen: Int
) {
    private val operandsIndices = IntArray(6)
    private val operand = StringBuilder()
    private var pos = 0
    private var currentTextMatrix = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)
    private var tf = StringBuilder()
    private var tfDef = StringBuilder()
    private var ts = 0f
    private var tl = 0f
    private var rgb = floatArrayOf(-1f, -1f, -1f)
    private var cmyk = floatArrayOf(-1f, -1f, -1f, -1f)
    var fontSizeOverride: Float? = null
    private var horizontalScaling = 1f
    private var textRise = 0f

    fun parse(
        s: StringBuilder,
        textObj: TextObject,
        tfDefault: StringBuilder = tfDef,
        startIndex: Int,
        rgb: FloatArray
    ): Int {
        if (tfDefault.isNotBlank()) {
            tf.clear().append(tfDefault)
        }
        this.rgb = rgb

        // Text matrix should not persist from one text object to another. Other states can persist.
        currentTextMatrix.apply {
            set(0, 1f); set(1, 0f); set(2, 0f); set(3, 1f); set(4, 0f); set(5, 0f)
        }

        pos = startIndex
        var operandsCount = 0
        var expectToken = true
        while (pos < s.length) {
            if (expectToken) {
                if (s[pos].isDigit() || s[pos] == '<' || s[pos] == '(' ||
                    s[pos] == '.' || s[pos] == '[' || s[pos] == '/' || s[pos] == '-'
                ) {
                    operandsIndices[operandsCount] = pos
                    operandsCount++

                    if (s.isEnclosingAt(pos)) {
                        pos = s.indexOfClosingChar(pos)
                        pos--
                    }
                } else if (s[pos] == 'T') {
                    pos++
                    if (s[pos] == 'j' || s[pos] == 'J') {
                        showText(pos, s, textObj)
                    } else if (s[pos] == 'd') {
                        positionText(s)
                    } else if (s[pos] == 'm') {
                        currentTextMatrix[0] =
                            getNumericOperand(s, operandsIndices[0], operandsIndices[1] - 1)
                        currentTextMatrix[1] =
                            getNumericOperand(s, operandsIndices[1], operandsIndices[2] - 1)
                        currentTextMatrix[2] =
                            getNumericOperand(s, operandsIndices[2], operandsIndices[3] - 1)
                        currentTextMatrix[3] =
                            getNumericOperand(s, operandsIndices[3], operandsIndices[4] - 1)
                        currentTextMatrix[4] =
                            getNumericOperand(s, operandsIndices[4], operandsIndices[5] - 1)
                        currentTextMatrix[5] = getNumericOperand(s, operandsIndices[5], pos - 2)
                    } else if (s[pos] == 'f') {
                        tf.clear().append(s, operandsIndices[0] + 1/* Exclude (/) delimiter */, pos - 2)
                        tf.cleanTF()
                        fontSizeOverride = null
                    } else if (s[pos] == 'D') {
                        positionText(s)
                        tl = -currentTextMatrix[5]
                    } else if (s[pos] == 'L') {
                        tl = getNumericOperand(s, operandsIndices[0], pos - 2)
                    } else if (s[pos] == '*') {
                        val tdMatrix = floatArrayOf(1f, 0f, 0f, 1f, 0f, -tl)
                        currentTextMatrix = multiplyTransformMatrices(tdMatrix, currentTextMatrix)
                    } else if (s[pos] == 's') {
                        ts = getNumericOperand(s, operandsIndices[0], pos - 2)
                    }
                    operandsCount = 0
                } else if (s[pos] == 'E' && s[pos + 1] == 'T') {
                    pos += 2
                    return pos
                } else if (s[pos] == '\"') {
                    var tjEnd = pos - 2
                    if (s.isUnEnclosingAt(tjEnd))
                        tjEnd = pos - 1
                    operand.clear().append(s, operandsIndices[2], tjEnd)
                    // TODO Support Tw(Word Spacing) and Tc(Character Spacing)
                    val tjObject = operand.toPDFObject(context, obj, gen)
                    if (tjObject != null) {
                        addTextElement(textObj, tjObject)
                    }
                    operandsCount = 0
                } else if (s[pos] == '\'') {
                    // Perform T*
                    val tdMatrix = floatArrayOf(1f, 0f, 0f, 1f, 0f, -tl)
                    currentTextMatrix = multiplyTransformMatrices(tdMatrix, currentTextMatrix)

                    showText(pos, s, textObj)
                    operandsCount = 0
                } else if ((s[pos] == 'R' && s[pos + 1] == 'G') || (s[pos] == 'r' && s[pos + 1] == 'g')) {
                    operandsIndices[operandsCount] = pos
                    getRGB(s)
                    operandsCount = 0
                } else if (s[pos] == 'K' || (s[pos] == 'k')) {
                    operandsIndices[operandsCount] = pos
                    getCMYK(s)
                    operandsCount = 0
                } else {
                    operandsCount = 0
                    if (s.isEnclosingAt(pos)) {
                        pos = s.indexOfClosingChar(pos)
                        pos--
                    }
                }
                if (!s.isWhiteSpaceAt(pos) && !s.isUnEnclosingAt(pos))
                    expectToken = false
            } else if (s.isWhiteSpaceAt(pos) || s.isUnEnclosingAt(pos)) {
                expectToken = true
            } else if (s[pos] == '/' || s.isEnclosingAt(pos)) {
                operandsIndices[operandsCount] = pos
                operandsCount++
                if (s.isEnclosingAt(pos)) {
                    pos = s.indexOfClosingChar(pos)
                    pos--
                }
            }
            pos++
        }
        return pos
    }

    private fun showText(pos: Int, s: StringBuilder, textObj: TextObject) {
        var tjEnd = pos
        while (tjEnd > operandsIndices[0]) {
            if (s.isUnEnclosingAt(tjEnd - 1))
                break
            tjEnd--
        }
        operand.clear().append(s, operandsIndices[0], tjEnd)
        val tjObject = operand.toPDFObject(context, obj, gen)
        if (tjObject != null) {
            addTextElement(textObj, tjObject)
        }
    }

    private fun addTextElement(textObj: TextObject, tj: PDFObject) {
        if (tf.isEmpty()) return

        val textElement = TextElement(
            tf = tf.toString(),
            tj = tj,
            ts = ts,
            rgb = rgb
        ).apply {
            textMatrix = currentTextMatrix.copyOf()
            fontSizeOverride?.let { fontSize = it }
            setTextParamsMatrix(fontSize, horizontalScaling, textRise)
        }

        textObj.add(textElement)
    }

    private fun positionText(s: StringBuilder) {
        val tx = getNumericOperand(s, operandsIndices[0], operandsIndices[1] - 1)
        val ty = getNumericOperand(s, operandsIndices[1], pos - 2)
        val tdMatrix = floatArrayOf(1f, 0f, 0f, 1f, tx, ty)
        currentTextMatrix = multiplyTransformMatrices(tdMatrix, currentTextMatrix)
    }

    private fun getRGB(s: StringBuilder) {
        for (i in 0..2) {
            rgb[i] = operand
                .clear()
                .append(s, operandsIndices[i], operandsIndices[i + 1] - 1)
                .toDouble()
                .toFloat()
        }
    }

    private fun getCMYK(s: StringBuilder) {
        for (i in 0..3) {
            cmyk[i] = operand
                .clear()
                .append(s, operandsIndices[i], operandsIndices[i + 1] - 1)
                .toDouble()
                .toFloat()
        }

        rgb = CmykToRgbConverter.convert(cmyk)
    }

    private fun StringBuilder.cleanTF() {
        var i = 0
        while (i < this.length) {
            if (this.isWhiteSpaceAt(i)) {
                while (i < this.length) {
                    if (this.isWhiteSpaceAt(i)) {
                        this.deleteCharAt(i)
                    } else {
                        this.insert(i, ' ')
                        break
                    }
                }
            }
            i++
        }
    }

    private fun getNumericOperand(
        textObjectContent: StringBuilder,
        startIndex: Int,
        endIndex: Int
    ): Float {
        return operand.clear()
            .append(textObjectContent, startIndex, endIndex)
            .toDouble()
            .toFloat()
    }
}