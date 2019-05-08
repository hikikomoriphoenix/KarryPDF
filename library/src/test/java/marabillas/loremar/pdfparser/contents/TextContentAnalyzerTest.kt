package marabillas.loremar.pdfparser.contents

import marabillas.loremar.pdfparser.contents.text.*
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
        val t2 =
            TextElement(tj = "[(Good) 20 (Bye) -300 (World)]".toPDFArray())
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
    fun testGroupTexts() {
        val t1 = TextElement(
            tj = "(Hello)".toPDFString(),
            td = floatArrayOf(0f, 100f),
            tf = "/Font 10"
        )
        val t2 = TextElement(
            tj = "(World)".toPDFString(),
            td = floatArrayOf(7f, 0f),
            tf = "/Font 10"
        )
        val t3 = TextElement(
            tj = "(Goodbye)".toPDFString(),
            td = floatArrayOf(0f, -15f),
            tf = "/Font 10"
        )
        val t4 = TextElement(
            tj = "(Sayonara)".toPDFString(),
            td = floatArrayOf(0f, -22f),
            tf = "/Font 10"
        )
        val t5 = TextElement(
            tj = "(How are you?)".toPDFString(),
            td = floatArrayOf(0f, 40f),
            tf = "/Font 10"
        )
        val t6 = TextElement(
            tj = "(I'm fine)".toPDFString(),
            td = floatArrayOf(15f, 40f),
            tf = "/Font 10"
        )
        val t7 = TextElement(
            tj = "(Thank you)".toPDFString(),
            td = floatArrayOf(0f, 25f),
            tf = "/Font 10"
        )
        val t8 = TextElement(
            tj = "()".toPDFString(),
            td = floatArrayOf(0f, 10f),
            tf = "/Font 10"
        )
        val t9 = TextElement(
            tj = "()".toPDFString(),
            td = floatArrayOf(10f, 10f),
            tf = "/Font 10"
        )
        val t10 = TextElement(
            tj = "()".toPDFString(),
            td = floatArrayOf(10f, -15f),
            tf = "/Font 10"
        )
        val t11 = TextElement(
            tj = "()".toPDFString(),
            td = floatArrayOf(20f, 10f),
            tf = "/Font 10"
        )

        // Create a TextObject that will produce two TextGroups
        val g1 = TextObject()
        g1.td[0] = t1.td[0]
        g1.td[1] = t1.td[1]
        g1.add(t1)
        g1.add(t2)
        g1.add(t3)
        g1.add(t4)

        // Create three TextObjects that will produce one TextGroup. The first one is far enough from the previous
        // TextObject to avoid being added to previous TextGroup.
        val g2 = TextObject()
        g2.td[0] = t5.td[0]
        g2.td[1] = t5.td[1]
        g2.add(t5)
        val g3 = TextObject()
        g3.td[0] = t6.td[0]
        g3.td[1] = t6.td[1]
        g3.add(t6)
        val g4 = TextObject()
        g4.td[0] = t7.td[0]
        g4.td[1] = t7.td[1]
        g4.add(t7)

        // Create one row Table with second TextObject being multi-linear.
        val g5 = TextObject()
        g5.td[0] = t8.td[0]
        g5.td[1] = t8.td[1]
        g5.add(t8)
        val g6 = TextObject()
        g6.td[0] = t9.td[0]
        g6.td[1] = t9.td[1]
        g6.add(t9)
        g6.add(t10)
        val g7 = TextObject()
        g7.td[0] = t11.td[0]
        g7.td[1] = t11.td[1]
        g7.add(t11)

        val textObjects = arrayListOf(g1, g2, g3, g4, g5, g6, g7)
        textObjects.sortWith(
            compareBy(
                { -it.getY() },
                { it.getX() })
        )
        val analyzer = TextContentAnalyzer(textObjects)
        analyzer.detectTableComponents()
        analyzer.groupTexts()

        // Assert the number and type of ContentGroups produced.
        assertThat(analyzer.contentGroups.size, `is`(4))
        assertTrue(analyzer.contentGroups[0] is TextGroup)
        assertTrue(analyzer.contentGroups[1] is TextGroup)
        assertTrue(analyzer.contentGroups[2] is TextGroup)
        assertTrue(analyzer.contentGroups[3] is Table)

        // Assert that each ContentGroup has the correct number of elements within each.
        assertThat((analyzer.contentGroups[0] as TextGroup).size(), `is`(2))
        assertThat((analyzer.contentGroups[1] as TextGroup).size(), `is`(1))
        assertThat((analyzer.contentGroups[2] as TextGroup).size(), `is`(2))
        assertThat((analyzer.contentGroups[3] as Table).size(), `is`(1))
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

        val analyzer =
            TextContentAnalyzer(arrayListOf(TextObject()))
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
        val analyzer =
            TextContentAnalyzer(arrayListOf(TextObject()))
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
        val analyzer =
            TextContentAnalyzer(arrayListOf(TextObject()))
        analyzer.contentGroups.add(g1)
        analyzer.concatenateDividedByHyphen()
        assertThat(g1.size(), `is`(1))
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
        assertThat(g2.size(), `is`(2))
    }

    @Test
    fun testFormParagraphs() {
        val t1 = TextElement(tj = "(Good Morning.)".toPDFString())
        val t2 =
            TextElement(tj = "(The quick brown fox jumps over)".toPDFString())
        val t3 =
            TextElement(tj = "(the lazy dog. The quick brown fox)".toPDFString())
        val t4 = TextElement(tj = "(jumps over.)".toPDFString())
        val t5 =
            TextElement(tj = "(Hello my name is Bond. James)".toPDFString())
        val t6 = TextElement(tj = "(Bond.)".toPDFString())
        val g = TextGroup()
        g.add(arrayListOf(t1))
        g.add(arrayListOf(t2))
        g.add(arrayListOf(t3))
        g.add(arrayListOf(t4))
        g.add(arrayListOf(t5))
        g.add(arrayListOf(t6))

        val analyzer =
            TextContentAnalyzer(arrayListOf(TextObject()))
        analyzer.contentGroups.add(g)
        val w = analyzer.getLargestWidth()
        analyzer.formParagraphs(w)

        assertThat(g.size(), `is`(3))
        assertThat((g[0][0].tj as PDFString).value, `is`("Good Morning."))
        val s1 = "${g[1][0].tj as PDFString}${g[1][1].tj as PDFString}${g[1][2].tj as PDFString}"
        assertThat(s1, `is`("The quick brown fox jumps over the lazy dog. The quick brown fox jumps over."))
        val s2 = "${g[2][0].tj as PDFString}${g[2][1].tj}"
        assertThat(s2, `is`("Hello my name is Bond. James Bond."))
    }
}