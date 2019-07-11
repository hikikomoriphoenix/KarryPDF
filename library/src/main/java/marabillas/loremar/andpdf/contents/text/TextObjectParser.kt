package marabillas.loremar.andpdf.contents.text

import marabillas.loremar.andpdf.contents.CmykToRgbConverter
import marabillas.loremar.andpdf.objects.PDFObject
import marabillas.loremar.andpdf.objects.toPDFObject
import marabillas.loremar.andpdf.objects.toPDFString
import marabillas.loremar.andpdf.utils.exts.*

internal class TextObjectParser(private val obj: Int, private val gen: Int) {
    private val operandsIndices = IntArray(6)
    private val operand = StringBuilder()
    private var pos = 0
    private var td = FloatArray(2)
    private var tf = StringBuilder()
    private var tfDef = StringBuilder()
    private var ts = 0f
    private var tl = 0f
    private var rgb = floatArrayOf(-1f, -1f, -1f)
    private var cmyk = floatArrayOf(-1f, -1f, -1f, -1f)

    fun parse(
        s: StringBuilder,
        textObj: TextObject,
        tfDefault: StringBuilder = tfDef,
        startIndex: Int,
        ctm: FloatArray,
        rgb: FloatArray
    ): Int {
        if (tfDefault.isNotBlank()) {
            tf.clear().append(tfDefault)
        }

        this.rgb = rgb

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
                        showText(pos, s, textObj, ctm)
                    } else if (s[pos] == 'd') {
                        positionText(s)
                    } else if (s[pos] == 'm') {
                        td[0] = operand
                            .clear()
                            .append(s, operandsIndices[4], operandsIndices[5] - 1)
                            .toDouble()
                            .toFloat()
                        td[1] = operand
                            .clear()
                            .append(s, operandsIndices[5], pos - 2)
                            .toDouble()
                            .toFloat()
                        val sx = operand
                            .clear()
                            .append(s, operandsIndices[0], operandsIndices[1] - 1)
                            .toDouble()
                            .toFloat()
                        if (sx < 0) {
                            td[0] = td[0] * (-1)
                        }
                        val sy = operand
                            .clear()
                            .append(s, operandsIndices[3], operandsIndices[4] - 1)
                            .toDouble()
                            .toFloat()
                        if (sy < 0) {
                            td[1] = td[1] * (-1)
                        }
                    } else if (s[pos] == 'f') {
                        tf.clear().append(s, operandsIndices[0] + 1/* Exclude (/) delimiter */, pos - 2)
                    } else if (s[pos] == 'D') {
                        positionText(s)
                        tl = -td[1]
                    } else if (s[pos] == 'L') {
                        tl = operand
                            .clear()
                            .append(s, operandsIndices[0], pos - 2)
                            .toDouble()
                            .toFloat()
                    } else if (s[pos] == '*') {
                        td[0] = 0f
                        td[1] = -tl
                    } else if (s[pos] == 's') {
                        ts = operand
                            .clear()
                            .append(s, operandsIndices[0], pos - 2)
                            .toDouble()
                            .toFloat()
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
                    addTextElement(textObj, operand.toPDFObject(obj, gen) ?: "()".toPDFString(), ctm)
                    operandsCount = 0
                } else if (s[pos] == '\'') {
                    // Perform T*
                    td[0] = 0f
                    td[1] = -tl

                    showText(pos, s, textObj, ctm)
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

    private fun showText(pos: Int, s: StringBuilder, textObj: TextObject, ctm: FloatArray) {
        var tjEnd = pos - 2
        if (s.isUnEnclosingAt(tjEnd))
            tjEnd = pos - 1
        operand.clear().append(s, operandsIndices[0], tjEnd)
        addTextElement(textObj, operand.toPDFObject(obj, gen) ?: "()".toPDFString(), ctm)
    }

    private fun addTextElement(textObj: TextObject, tj: PDFObject, ctm: FloatArray) {
        if (tf.isEmpty()) return

        // If first element, apply CTM and initialize TextObject's x and y
        if (textObj.count() == 0) {
            td[0] = td[0] * ctm[0] + ctm[4]
            td[1] = td[1] * ctm[3] + ctm[5]
            textObj.td[0] = td[0]
            textObj.td[1] = td[1]
        }

        val content = TextElement(
            tf = tf.toString(),
            td = td.copyOf(),
            tj = tj,
            ts = ts,
            rgb = rgb
        )
        textObj.add(content)
    }

    private fun positionText(s: StringBuilder) {
        td[0] = operand
            .clear()
            .append(s, operandsIndices[0], operandsIndices[1] - 1)
            .toDouble()
            .toFloat()
        td[1] = operand
            .clear()
            .append(s, operandsIndices[1], pos - 2)
            .toDouble()
            .toFloat()
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

        rgb = CmykToRgbConverter.inst.convert(cmyk)
    }
}