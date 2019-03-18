package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.extractEnclosedObject
import marabillas.loremar.pdfparser.objects.toPDFString

internal class ContentStreamParser {
    private var reader = "".reader().buffered()
    private val contents = ArrayList<PageContent>()

    fun parse(streamData: String): ArrayList<PageContent> {
        reader = streamData.reader().buffered()
        while (true) {
            val s = reader.readLine() ?: break
            parseDefault(s)
        }

        return contents
    }

    private fun parseDefault(s: String) {
        if (s == "BT") parseText()
    }

    private fun parseText() {
        var td = ""
        var tm = ""
        var tf = ""
        while (true) {
            val s = reader.readLine()
            when {
                s.endsWith("Tj") -> {
                    val data = s.substringBeforeLast("Tj").trim()
                    val tj = data.extractEnclosedObject().toPDFString().value
                    val textObj = TextContent(
                        tj = tj,
                        td = td,
                        tm = tm,
                        tf = tf
                    )
                    contents.add(textObj)
                }
                s.endsWith("Td") -> {
                    td = s.substringBeforeLast("Td").trim()
                }
                s.endsWith("Tm") -> {
                    tm = s.substringBeforeLast("Tm").trim()
                }
                s.endsWith("Tf") -> {
                    tf = s.substringBeforeLast("Tf").trim()
                }
                s == "ET" -> return
            }
        }
    }
}