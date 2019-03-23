package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.*

class TextObjectParser {
    fun parse(string: String, contents: ArrayList<PageContent>, tfDefault: String = ""): String {
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

        val addContent: (string: String) -> Unit = {
            val content = TextContent(
                tf = tf,
                td = td.copyOf(),
                tj = it,
                ts = ts
            )
            contents.add(content)
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
                        td[0] = (operands[4] as Numeric).value.toFloat()
                        td[1] = (operands[5] as Numeric).value.toFloat()
                    }
                    "TL" -> tl = (operands[0] as Numeric).value.toFloat()
                    "Ts" -> ts = (operands[0] as Numeric).value.toFloat()
                    "Tj" -> addContent("${operands[0]}")
                    "\"" -> addContent("${operands[2]}")
                    "\'" -> {
                        toNextLine()
                        addContent("${operands[0]}")
                    }
                    "TJ" -> {
                        val arr = operands[0] as PDFArray
                        val sb = StringBuilder()
                        arr.forEach {
                            if (it is PDFString) {
                                sb.append(it)
                            } else {
                                val offset = (it as Numeric).value.toFloat()
                                if (offset < 0) sb.append(" ")
                            }
                        }
                        addContent(sb.toString())
                    }
                    "ET" -> return s
                }
                operands.clear()
            }
        }
    }
}