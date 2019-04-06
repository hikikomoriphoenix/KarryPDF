package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.extractEnclosedObject
import marabillas.loremar.pdfparser.objects.startsEnclosed
import marabillas.loremar.pdfparser.objects.toPDFObject

internal class ContentStreamParser {
    companion object {
        /**
         * Get the next token in the content stream which could either be a string representing a valid PDFObject or an
         * operator.
         *
         * @param s Remaining string in the stream.
         * @return the next token as a string.
         */
        fun getNextToken(s: String): String {
            s.trim()
            return when {
                s.startsEnclosed() -> s.extractEnclosedObject()
                else -> {
                    // Check if next token is a Reference Object
                    var p = "^\\d+ \\d+ R".toRegex()
                    var t = p.find(s)?.value ?: ""

                    // If not Reference then get the substring before any delimiter
                    if (t == "") {
                        p = "^/?([\\w*'\"]+|-?[0-9]+([,.][0-9]+)?|-?.?[0-9]+)[(<\\[{/\\s]".toRegex()
                        t = p.find(s)?.value ?: ""

                        // When end of stream is reached, the previous pattern will not match. Hence, just return the
                        // remaining string.
                        if (t != "") t.substring(0, t.length - 1) else s
                    } else {
                        t
                    }
                }
            }
        }
    }

    fun parse(streamData: String): ArrayList<PageObject> {
        var s = streamData
        //println("ContentStream->\n$s")
        val operands = ArrayList<PDFObject>()
        val pageObjects = ArrayList<PageObject>()
        var tf = ""
        while (true) {
            if (s == "") break
            val token = getNextToken(s)
            s = s.substringAfter(token).trim()
            val operand = token.toPDFObject(true)
            if (operand != null) {
                operands.add(operand)
            } else {
                when (token) {
                    "Tf" -> tf = "${operands[0]} ${operands[1]}"
                    "BT" -> {
                        val textObj = TextObject()
                        s = TextObjectParser().parse(s, textObj, tf)
                        pageObjects.add(textObj)
                    }
                }
                operands.clear()
            }
        }

        return pageObjects
    }
}

fun String.getNextToken(): String {
    return ContentStreamParser.getNextToken(this)
}