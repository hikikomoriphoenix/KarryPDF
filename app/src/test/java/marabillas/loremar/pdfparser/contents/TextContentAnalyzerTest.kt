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

    @Test
    fun testHandleMultiColumnTexts() {
        val t1 = TextObject(); t1.td[0] = 0f; t1.td[1] = 0f
        val t2 = TextObject(); t2.td[0] = 100f; t2.td[1] = 0f
        val t3 = TextObject(); t3.td[0] = 0f; t3.td[1] = 100f
        val t4 = TextObject(); t4.td[0] = 100f; t4.td[1] = 100f
        val t5 = TextObject(); t5.td[0] = 0f; t5.td[1] = 80f
        val t6 = TextObject(); t6.td[0] = 100f; t6.td[1] = 80f
        val t7 = TextObject(); t7.td[0] = 0f; t7.td[1] = 50f
        val textObjs = ArrayList<TextObject>()
        textObjs.add(t1)
        textObjs.add(t2)
        textObjs.add(t3)
        textObjs.add(t4)
        textObjs.add(t5)
        textObjs.add(t6)
        textObjs.add(t7)
        TextContentAnalyzer(textObjs).handleMultiColumnTexts()
        assertThat(t1.columned, `is`(true))
        assertThat(t2.columned, `is`(true))
        assertThat(t3.columned, `is`(true))
        assertThat(t4.columned, `is`(true))
        assertThat(t5.columned, `is`(true))
        assertThat(t6.columned, `is`(true))
        assertThat(t7.columned, `is`(true))
        assertThat(t1.rowed, `is`(false))
        assertThat(t2.rowed, `is`(false))
        assertThat(t3.rowed, `is`(true))
        assertThat(t4.rowed, `is`(true))
        assertThat(t5.rowed, `is`(true))
        assertThat(t6.rowed, `is`(true))
        assertThat(t7.rowed, `is`(false))
    }
}