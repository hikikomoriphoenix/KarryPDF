package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.TimeCounter
import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.toPDFObject

internal class TextObjectParser {
    var tctr = 0L
    private val td = FloatArray(2)
    private var ts = 0f
    private var tl = 0f
    private var tf = ""
    private val operands = ArrayList<PDFObject>()
    private var i = 0

    fun parse(s: String, textObj: TextObject, tfDefault: String = "", startIndex: Int): Int {
        TimeCounter.reset()

        td[0] = 0f
        td[1] = 0f
        ts = 0f
        tl = 0f
        if (tfDefault.isNotBlank()) {
            tf = tfDefault
        }
        operands.clear()
        i = startIndex

        while (i < s.length) {
            i = s.getNextToken(i)
            val token = ContentStreamParser.token.toString()
            //println("token -> $token")
            val operand = token.toPDFObject(true)
            if (operand != null) {
                operands.add(operand)
            } else {
                when (token) {
                    "Tf" -> tf = "${operands[0]} ${operands[1]}"
                    "Td" -> positionText()
                    "TD" -> {
                        positionText()
                        tl = -td[1]
                    }
                    "T*" -> toNextLine()
                    "Tm" -> {
                        var tx = (operands[4] as Numeric).value.toFloat()
                        var ty = (operands[5] as Numeric).value.toFloat()

                        // Handle mirroring
                        val sx = (operands[0] as Numeric).value.toFloat()
                        val sy = (operands[3] as Numeric).value.toFloat()
                        if (sx < 0) tx *= -1
                        if (sy < 0) ty *= -1

                        td[0] = tx
                        td[1] = ty
                    }
                    "TL" -> tl = (operands[0] as Numeric).value.toFloat()
                    "Ts" -> ts = (operands[0] as Numeric).value.toFloat()
                    "Tj", "TJ" -> addTextElement(textObj, operands[0])
                    "\"" -> addTextElement(textObj, operands[2])
                    "\'" -> {
                        toNextLine()
                        addTextElement(textObj, operands[0])
                    }
                    "ET" -> {
                        tctr += TimeCounter.getTimeElapsed()
                        return i
                    }
                }
                operands.clear()
            }
        }

        return i
    }

    private fun positionText() {
        td[0] = (operands[0] as Numeric).value.toFloat()
        td[1] = (operands[1] as Numeric).value.toFloat()
    }

    private fun addTextElement(textObj: TextObject, tj: PDFObject) {
        val content = TextElement(
            tf = tf,
            td = td.copyOf(),
            tj = tj,
            ts = ts
        )
        textObj.add(content)
        if (textObj.count() == 1) {
            textObj.td[0] = td[0]
            textObj.td[1] = td[1]
        }
    }

    private fun toNextLine() {
        td[0] = 0f
        td[1] = -tl
    }
}