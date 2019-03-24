package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.PDFString
import marabillas.loremar.pdfparser.objects.toPDFArray
import marabillas.loremar.pdfparser.objects.toPDFString
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TextContentAnalyzerTest {
    @Test
    fun testHandleTJArrays() {
        val t1 = TextElement(tj = "(Hello World)".toPDFString())
        val t2 = TextElement(tj = "[(Good) 20 (Bye) -300 (World)]".toPDFArray())
        val t3 = TextElement(
            tj = ("[(The) -300 (quick) -300 (brown) -300 (fox) -300 (jumps) -300 (over) -300 " +
                    " (the) -300 (lazy) -300 (dog.)]").toPDFArray()
        )
        val t4 =
            TextElement(tj = "[(W) -50 (O) -50 (R) -50 (L) -50 (D) -350 (P) -50 (E) -50 (A) -50 (C) -50 (E)]".toPDFArray())
        val textObj = TextObject()
        textObj.add(t1)
        textObj.add(t2)
        textObj.add(t3)
        textObj.add(t4)
        TextContentAnalyzer(arrayListOf(textObj)).handleTJArrays()
        textObj.forEachIndexed { i, textElement ->
            val s = (textElement.tj as PDFString).value
            when (i) {
                0 -> assertThat(s, `is`("Hello World"))
                1 -> assertThat(s, `is`("GoodBye World"))
                2 -> assertThat(s, `is`("The quick brown fox jumps over the lazy dog."))
                3 -> assertThat(s, `is`("W O R L D  P E A C E"))
            }
        }
    }
}