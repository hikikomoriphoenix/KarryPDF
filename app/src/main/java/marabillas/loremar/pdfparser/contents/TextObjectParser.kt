package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.toPDFObject

class TextObjectParser {
    fun parse(string: String, contents: ArrayList<PageContent>, tfDefault: String = ""): String {
        var td = ""
        var tm = ""
        var tj = ""
        var tf = tfDefault
        var s = string
        val operands = ArrayList<PDFObject>()
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
                    "Td" -> td = "${operands[0]} ${operands[1]}"
                    "Tj", "TJ" -> {
                        tj = "${operands[0]}"
                        val content = TextContent(
                            tf = tf,
                            td = td,
                            tj = tj
                        )
                        contents.add(content)
                    }
                    "ET" -> return s
                }
                operands.clear()
            }
        }
    }
}