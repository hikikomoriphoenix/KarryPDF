package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.Numeric
import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.toPDFObject

internal class TextObjectParser {
    fun parse(string: String, textObj: TextObject, tfDefault: String = ""): String {
        val td = FloatArray(2)
        var ts = 0f
        var tl = 0f
        var tf = tfDefault
        var s = string
        val operands = ArrayList<PDFObject>()

        val positionText: () -> Unit = {
            td[0] = (operands[0] as Numeric).value.toFloat()
            td[1] = (operands[1] as Numeric).value.toFloat()
        }

        val addTextElement: (tj: PDFObject) -> Unit = {
            val content = TextElement(
                tf = tf,
                td = td.copyOf(),
                tj = it,
                ts = ts
            )
            textObj.add(content)
            if (textObj.count() == 1) {
                textObj.td[0] = td[0]
                textObj.td[1] = td[1]
            }
        }

        val toNextLine: () -> Unit = {
            td[0] = 0f
            td[1] = -tl
        }

        while (true) {
            if (s == "") return ""
            val token = s.getNextToken()
            s = s.substringAfter(token).trim()
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
                    "Tj", "TJ" -> addTextElement(operands[0])
                    "\"" -> addTextElement(operands[2])
                    "\'" -> {
                        toNextLine()
                        addTextElement(operands[0])
                    }
                    "ET" -> return s
                }
                operands.clear()
            }
        }
    }
}