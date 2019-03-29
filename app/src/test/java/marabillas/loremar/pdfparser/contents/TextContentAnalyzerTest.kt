package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.objects.PDFString
import marabillas.loremar.pdfparser.objects.toPDFArray
import marabillas.loremar.pdfparser.objects.toPDFString
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
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

    @Test
    fun testGroupTexts() {
        val t1 = TextElement(tf = "/Font 10", tj = "(Hello)".toPDFString(), td = floatArrayOf(0f, 100f))
        val t2 = TextElement(tf = "/Font 10", tj = "(World)".toPDFString(), td = floatArrayOf(50f, 0f))
        val t3 = TextElement(tf = "/Font 10", tj = "(Goodbye)".toPDFString(), td = floatArrayOf(0f, -15f))
        val t4 = TextElement(tf = "/Font 10", tj = "(I love you)".toPDFString(), td = floatArrayOf(0f, -22f))
        val t5 = TextElement(tf = "/Font 10", tj = "(Monday)".toPDFString(), td = floatArrayOf(100f, 100f))
        val t6 = TextElement(tf = "/Font 10", tj = "(Tuesday)".toPDFString(), td = floatArrayOf(0f, 50f))
        val t7 = TextElement(tf = "/Font 10", tj = "(Wednesday)".toPDFString(), td = floatArrayOf(100f, 50f))
        val tObj1 = TextObject()
        tObj1.add(t1)
        tObj1.add(t2)
        tObj1.add(t3)
        tObj1.add(t4)
        tObj1.td[0] = t1.td[0]
        tObj1.td[1] = t1.td[1]
        val tObj2 = TextObject()
        tObj2.add(t5)
        tObj2.td[0] = t5.td[0]
        tObj2.td[1] = t5.td[1]
        val tObj3 = TextObject()
        tObj3.add(t6)
        tObj3.td[0] = t6.td[0]
        tObj3.td[1] = t6.td[1]
        val tObj4 = TextObject()
        tObj4.add(t7)
        tObj4.td[0] = t7.td[0]
        tObj4.td[1] = t7.td[1]

        val t8 = TextElement(tf = "/Font 10", tj = "(The quick)".toPDFString(), td = floatArrayOf(0f, 40f))
        val t9 = TextElement(tf = "/Font 10", tj = "(brown fox)".toPDFString(), td = floatArrayOf(20f, 0f))
        val t10 = TextElement(tf = "/Font 10", tj = "(jumps over)".toPDFString(), td = floatArrayOf(0f, 25f))
        val t11 = TextElement(tf = "/Font 10", tj = "(the lazy dog)".toPDFString(), td = floatArrayOf(0f, -15f))
        val t12 = TextElement(tf = "/Font 10", tj = "(Good Morning)".toPDFString(), td = floatArrayOf(0f, -22f))
        val t13 = TextElement(tf = "/Font 10", tj = "(Good Night)".toPDFString(), td = floatArrayOf(0f, -40f))
        val tObj5 = TextObject()
        tObj5.add(t8)
        tObj5.add(t9)
        tObj5.td[0] = t8.td[0]
        tObj5.td[1] = t8.td[1]
        val tObj6 = TextObject()
        tObj6.add(t10)
        tObj6.add(t11)
        tObj6.add(t12)
        tObj6.td[0] = t10.td[0]
        tObj6.td[1] = t10.td[1]
        val tObj7 = TextObject()
        tObj7.add(t13)
        tObj7.td[0] = t13.td[0]
        tObj7.td[1] = t13.td[1]

        val t14 = TextElement(tf = "/Font 10", tj = "(Hi)".toPDFString(), td = floatArrayOf(0f, 115f))
        val tObj8 = TextObject()
        tObj8.add(t14)
        tObj8.td[0] = t14.td[0]
        tObj8.td[1] = t14.td[1]

        val analyzer = TextContentAnalyzer(arrayListOf(tObj8, tObj1, tObj2, tObj3, tObj4, tObj5, tObj6, tObj7))
        analyzer.handleMultiColumnTexts()
        analyzer.groupTexts()

        assertThat(analyzer.contentGroups.count(), `is`(5))
        assertTrue(analyzer.contentGroups[1] is Table)
        val g1 = analyzer.contentGroups[0] as TextGroup
        val g2 = analyzer.contentGroups[1] as Table
        val g3 = analyzer.contentGroups[2] as TextGroup
        val g4 = analyzer.contentGroups[3] as TextGroup
        val g5 = analyzer.contentGroups[4] as TextGroup

        assertThat(g1.count(), `is`(1))
        assertThat(g1[0].count(), `is`(1))
        assertThat(g1[0][0], `is`(t14))

        assertThat(g2[0][0].count(), `is`(2))
        assertThat(g2[0][0][0].count(), `is`(2))
        assertThat(g2[0][0][1].count(), `is`(1))
        assertThat(g2[0][0][0][0].count(), `is`(2))
        assertThat(g2[0][0][0][1].count(), `is`(1))
        assertThat(g2[0][0][1][0].count(), `is`(1))
        assertThat(g2[0][0][0][0][0], `is`(t1))
        assertThat(g2[0][0][0][0][1], `is`(t2))
        assertThat(g2[0][0][0][1][0], `is`(t3))
        assertThat(g2[0][0][1][0][0], `is`(t4))

        assertThat(g3.count(), `is`(3))
        assertThat(g3[0].count(), `is`(2))
        assertThat(g3[1].count(), `is`(1))
        assertThat(g3[2].count(), `is`(1))
        assertThat(g3[0][0], `is`(t8))
        assertThat(g3[0][1], `is`(t9))
        assertThat(g3[1][0], `is`(t10))
        assertThat(g3[2][0], `is`(t11))

        assertThat(g4.count(), `is`(1))
        assertThat(g4[0].count(), `is`(1))
        assertThat(g4[0][0], `is`(t12))

        assertThat(g5.count(), `is`(1))
        assertThat(g5[0].count(), `is`(1))
        assertThat(g5[0][0], `is`(t13))
    }

    @Test
    fun testCheckForListTypeTextGroups() {
        val t0 = TextElement(tj = "(A)".toPDFString())
        val t1 = TextElement(tj = "(Dog.)".toPDFString())
        val t2 = TextElement(tj = "(cat.)".toPDFString())
        val t3 = TextElement(tj = "(Rat.)".toPDFString())
        val g1 = TextGroup()
        g1.add(arrayListOf(t0, t1))
        g1.add(arrayListOf(t2))
        g1.add(arrayListOf(t3))
        val t4 = TextElement(tj = "(Hi my name is)".toPDFString())
        val t5 = TextElement(tj = "(James Bond.)".toPDFString())
        val g2 = TextGroup()
        g2.add(arrayListOf(t4))
        g2.add(arrayListOf(t5))
        val table = Table()
        val row = Table.Row()
        val cell1 = Table.Cell()
        val cell2 = Table.Cell()
        table.add(row)
        row.add(cell1)
        row.add(cell2)
        cell1.add(g1)
        cell2.add(g2)

        val t6 = TextElement(tj = "(A)".toPDFString())
        val t7 = TextElement(tj = "(Car.)".toPDFString())
        val t8 = TextElement(tj = "(Plane.)".toPDFString())
        val t9 = TextElement(tj = "(Boat.)".toPDFString())
        val g3 = TextGroup()
        g3.add(arrayListOf(t6, t7))
        g3.add(arrayListOf(t8))
        g3.add(arrayListOf(t9))
        val t10 = TextElement(tj = "(Asta la vista)".toPDFString())
        val t11 = TextElement(tj = "(Baby.)".toPDFString())
        val g4 = TextGroup()
        g4.add(arrayListOf(t10))
        g4.add(arrayListOf(t11))

        val analyzer = TextContentAnalyzer(arrayListOf(TextObject()))
        analyzer.contentGroups.add(table)
        analyzer.contentGroups.add(g3)
        analyzer.contentGroups.add(g4)

        analyzer.checkForListTypeTextGroups()

        assertThat(g1.isAList, `is`(true))
        assertThat(g2.isAList, `is`(false))
        assertThat(g3.isAList, `is`(true))
        assertThat(g4.isAList, `is`(false))
    }

    @Test
    fun testGetLargestWidth() {
        val t1 = TextElement(tj = "(Hello)".toPDFString())
        val t2 = TextElement(tj = "(World)".toPDFString())
        val t3 = TextElement(tj = "(Hi)".toPDFString())
        val g1 = TextGroup()
        val g2 = TextGroup()
        g1.add(arrayListOf(t1, t2))
        g2.add(arrayListOf(t3))
        val analyzer = TextContentAnalyzer(arrayListOf(TextObject()))
        analyzer.contentGroups.add(g1)
        analyzer.contentGroups.add(g2)
        val w = analyzer.getLargestWidth()
        assertThat(w, `is`(10))
    }

    @Test
    fun testConcatenateDividedByHyphen() {
        val t1 = TextElement(tj = "(United we stand. Di-)".toPDFString())
        val t2 = TextElement(tj = "(vided we fall. Uni-)".toPDFString())
        val t3 = TextElement(tj = "(ted we stand.)".toPDFString())
        val g1 = TextGroup()
        g1.add(arrayListOf(t1))
        g1.add(arrayListOf(t2))
        g1.add(arrayListOf(t3))
        val analyzer = TextContentAnalyzer(arrayListOf(TextObject()))
        analyzer.contentGroups.add(g1)
        analyzer.concatenateDividedByHyphen()
        assertThat(g1.count(), `is`(1))
        assertThat(g1[0].count(), `is`(3))
        val s = "${g1[0][0].tj as PDFString}${g1[0][1].tj as PDFString}${g1[0][2].tj as PDFString}"
        assertThat(s, `is`("United we stand. Divided we fall. United we stand."))

        val t4 = TextElement(tj = "(United we stand.)".toPDFString())
        val t5 = TextElement(tj = "(Divided we fall.)".toPDFString())
        val g2 = TextGroup()
        g2.add(arrayListOf(t4))
        g2.add(arrayListOf(t5))
        analyzer.contentGroups.clear()
        analyzer.contentGroups.add(g2)
        analyzer.concatenateDividedByHyphen()
        assertThat(g2.count(), `is`(2))
    }

    @Test
    fun testFormParagraphs() {
        val t1 = TextElement(tj = "(Good Morning.)".toPDFString())
        val t2 = TextElement(tj = "(The quick brown fox jumps over)".toPDFString())
        val t3 = TextElement(tj = "(the lazy dog. The quick brown fox)".toPDFString())
        val t4 = TextElement(tj = "(jumps over.)".toPDFString())
        val t5 = TextElement(tj = "(Hello my name is Bond. James)".toPDFString())
        val t6 = TextElement(tj = "(Bond.)".toPDFString())
        val g = TextGroup()
        g.add(arrayListOf(t1))
        g.add(arrayListOf(t2))
        g.add(arrayListOf(t3))
        g.add(arrayListOf(t4))
        g.add(arrayListOf(t5))
        g.add(arrayListOf(t6))

        val analyzer = TextContentAnalyzer(arrayListOf(TextObject()))
        analyzer.contentGroups.add(g)
        val w = analyzer.getLargestWidth()
        analyzer.formParagraphs(w)

        assertThat(g.count(), `is`(3))
        assertThat((g[0][0].tj as PDFString).value, `is`("Good Morning."))
        val s1 = "${g[1][0].tj as PDFString}${g[1][1].tj as PDFString}${g[1][2].tj as PDFString}"
        assertThat(s1, `is`("The quick brown fox jumps over the lazy dog. The quick brown fox jumps over."))
        val s2 = "${g[2][0].tj as PDFString}${g[2][1].tj}"
        assertThat(s2, `is`("Hello my name is Bond. James Bond."))
    }
}