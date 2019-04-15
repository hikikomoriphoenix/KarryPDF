package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.EnclosedObjectExtractor
import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.Reference
import marabillas.loremar.pdfparser.objects.toPDFObject

internal class ContentStreamParser {
    companion object {
        val token = StringBuilder()

        fun getNextToken(s: String, startIndex: Int): Int {
            token.clear()
            //println("Memory 0 -> ${Runtime.getRuntime().totalMemory()}")
            var i = startIndex

            // Skip white space characters
            while (i < s.length && isWhiteSpaceAt(s, i))
                i++
            if (i >= s.length)
                return i

            when {
                isEnclosingAt(s, i) -> {
                    val close = EnclosedObjectExtractor.indexOfClosingChar(s, i)
                    token.append(s.substring(i, close + 1))
                    return close + 1
                }
                else -> {
                    if (s[i].isDigit()) {
                        val firstSpace = s.indexOf(' ', i)
                        val secondSpace = s.indexOf(' ', firstSpace + 1)
                        if (s[secondSpace + 1] == 'R' && isDelimiterAt(s, secondSpace + 2)) {
                            for (c in i..secondSpace + 1)
                                token.append(s[c])
                            if (Reference.REGEX.matches(token))
                                return secondSpace + 2
                        }
                    }

                    val del: Int = if (s[i] == '/')
                        indexOfDelimiter(s, i + 1)
                    else
                        indexOfDelimiter(s, i)

                    for (c in i until del)
                        token.append(s[c])
                    return del
                }
            }
        }

        private fun isEnclosingAt(s: String, i: Int): Boolean {
            return (s[i] == '(' || s[i] == '[' || s[i] == '<' || s[i] == '{')
        }

        private fun isWhiteSpaceAt(s: String, i: Int): Boolean {
            return (s[i] == ' ' || s[i] == '\n' || s[i] == '\r')
        }

        private fun isDelimiterAt(s: String, i: Int): Boolean {
            return (isEnclosingAt(s, i) || isWhiteSpaceAt(s, i) || s[i] == '/')
        }

        private fun indexOfDelimiter(s: String, start: Int): Int {
            var i = start
            while (i < s.length) {
                if (isEnclosingAt(s, i) || isWhiteSpaceAt(s, i) || s[i] == '/')
                    return i
                i++
            }
            return i
        }
    }

    fun parse(streamData: String): ArrayList<PageObject> {
        val operands = ArrayList<PDFObject>()
        val pageObjects = ArrayList<PageObject>()
        val textObjectParser = TextObjectParser()
        var tf = ""
        var i = 0
        var ts = 0L

        //println("stream->\n$streamData")

        while (i < streamData.length) {
            i = getNextToken(streamData, i)
            if (token.isBlank())
                break
            //println("token->$token")
            val token = token.toString()
            val operand = token.toPDFObject(true)
            if (operand != null) {
                operands.add(operand)
            } else {
                when (token) {
                    "Tf" -> tf = "${operands[0]} ${operands[1]}"
                    "BT" -> {
                        val textObj = TextObject()
                        i = textObjectParser.parse(streamData, textObj, tf, i)
                        pageObjects.add(textObj)
                    }
                }
                operands.clear()
            }
        }

        println("Time spent for TextObjectParser operation -> ${textObjectParser.tctr} ms")
        return pageObjects
    }
}

fun String.getNextToken(startIndex: Int): Int {
    return ContentStreamParser.getNextToken(this, startIndex)
}